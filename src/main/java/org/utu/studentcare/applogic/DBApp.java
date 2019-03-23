package org.utu.studentcare.applogic;

import org.utu.studentcare.db.DBCleaner;
import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.db.orm.*;
import org.utu.studentcare.textmodeui.AppLogic;
import org.utu.studentcare.textmodeui.Menu;
import org.utu.studentcare.textmodeui.MenuSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Main application with all the business logic.
 */
public class DBApp implements Runnable {
    /**
     * Database file.
     */
    private final String dbFile;

    /**
     * Interface for reading interactive user input line by line.
     */
    private final Supplier<String> inputStream;

    /**
     * Closes the JavaFX platform and finally executes the provided lambda.
     */
    private final Consumer<Consumer<Void>> shutdownHook;

    /**
     * Prints debug information about sql queries etc. Let's the user login without id/pw.
     */
    private final boolean debugMode = true;

    /**
     * Slows down sql queries artificially.
     */
    private final boolean slowMode = false;

    /**
     * Models the text mode user interface.
     */
    private final AppLogic appLogic = new AppLogic(debugMode, "main",
            // alku/päävalikko
            new Menu("main", "S", s -> c -> c
                    .h1("StudentCare Pro 1.0 by Tietotuunarit Ltd")
                    .p("Kirjautunut sisään käyttäjänä " + s.user.wholeName())
                    .p("Roolit: opiskelija" + (s.user.isTeacher ? " + opettaja" : "") + (s.user.isAdmin ? " + hallinto" : ""))
                    .h3("Toiminnot:")
                    .a("joinCourses", "Liity kurssille")
                    .a_("studentCourses", ss -> s.user.attending(s.connection).isEmpty() ? null : "Opiskelemasi kurssit")
                    .a("teachCourses", s.user.isTeacher ? "Ala opettaa kurssia" : "")
                    .a("gradeCourses", s.user.isTeacher ? "Arvioi kurssisuorituksia" : "")
                    .a_("approveCourses", ss -> !s.user.isAdmin ? null : "Opintorekisteriin kirjaukset" + (CourseGrade.waitingApproval(s.connection).isEmpty() ? "(kaikki ok!)" : " (uutta kirjattavaa!)"))
                    .a("wipeDB", s.user.isAdmin ? "Alusta tietokanta" : null)
                    .a("logout", s.active() ? "Kirjaudu ulos" : null)

            ),
            // oppilas valitsee kurssin, jolle liittyä
            new Menu("joinCourses", "S", s -> c -> c
                    .h1("Valitse opiskeltava kurssi vapaista kursseista")
                    .nest(cc -> cc.li(s.user.notAttending(s.connection).stream().map(co -> c.a(l -> l.x("joinCourse").x(co.instanceId), co.wholeNameId(40)))))
                    .a("main", "Takaisin päävalikkoon")
            ),
            // tyhjä valikko, sivuvaikutus: liittää opiskeljan kurssille
            new Menu("joinCourse", "SS", s -> {
                s.user.joinCourse(s.connection, CourseInstance.findI(s.connection, s.param(1)).orElseThrow(() -> new AppLogicException("Kurssia ei löytynyt!")));
                return c -> c
                        .h1("Liitytään kurssille " + s.param(1))
                        .a("joinCourses", "Takaisin");
            }),
            // opettaja valitsee kurssin, jolle liittyä opettamaan
            new Menu("teachCourses", "S", s -> c -> c
                    .h1("Valitse opetettava kurssi vapaista kursseista")
                    .nest(cc -> cc.li(s.user.asTeacher().notTeaching(s.connection).stream().map(co -> c.a(l -> l.x("teachCourse").x(co.instanceId), co.wholeNameId(40)))))
                    .a("main", "Takaisin päävalikkoon")
            ),
            // tyhjä valikko, sivuvaikutus: liittää opettajan kurssille
            new Menu("teachCourse", "SS", s -> {
                s.user.asTeacher().teachCourse(s.connection, CourseInstance.findI(s.connection, s.param(1)).orElseThrow(() -> new AppLogicException("Kurssia ei löytynyt!")));
                return c -> c
                        .p("Liitytään opettamaan kurssille " + s.param(1))
                        .a("teachCourses", "Takaisin");
            }),
            // oppilaan valikko, josta valita tarkasteltava kurssi. kursseille pitää olla liittynyt aiemmin
            new Menu("studentCourses", "S", s -> c -> c
                    .h1("Valitse opiskeltavista kursseistasi")
                    .nest(cc -> cc.li(s.user.attending(s.connection).stream().map(co -> c.a(l -> l.x("studentCourse").x(co.instanceId), co.wholeNameId(40)))))
                    .a("main", "Takaisin päävalikkoon")
            ),
            // opettajan valikko, josta valita tarkasteltava kurssi (arviointia varten). kursseille pitää olla liittynyt aiemmin
            new Menu("gradeCourses", "S", s -> c -> c
                    .h1("Valitse arvioitavista kursseistasi")
                    .nest(cc -> cc.li(s.user.asTeacher().teaching(s.connection).stream().map(co -> c.a(l -> l.x("gradeCourse").x(co.instanceId), co.wholeNameId(40)))))
                    .a("main", "Takaisin päävalikkoon")
            ),
            // opiskelijan kurssinäkymä. harjoitusten rakenne, pistelasku, aiemmat kurssisuoritukset, harjoitusten teko
            new Menu("studentCourse", "SS", s -> {
                Student student = Student.find(s.connection, s.user.id).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                List<CourseGrade> grades = CourseGrade.findAll(s.connection, student.id, s.param(1));

                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .h3("Harjoitusten rakenne")
                        .li(exs.specs.getExerciseDecls().stream().map(co -> c.p(co.overview())))
                        .h3("Pistelasku:")
                        .li(exs.specs.getGradingDecls().stream().map(co -> c.p(co.toString())))
                        .h3("Kurssisuoritukset:")
                        .li(grades.stream().map(g -> c.p(s2 -> g.overview(s.connection))))
                        .h3("Toiminnot:")
                        .li(exs.exercises.stream().filter(Exercise::uploaded).map(e -> c.a(l -> l.x("studentCourseExercise").x(s.params()).x(e.exerciseId), "Palauta harjoitus " + e.exerciseId)))
                        .br()
                        .a(l -> l.x("partCourse").x(s.param(1)).x(s.user.id), "Poistu kurssilta")
                        .a("studentCourses", "Takaisin");
            }),
            // opiskelijan harjoitustehtävän näkymä. UI:n rajallisuuden vuoksi tässä ei vielä syötetä dataa
            new Menu("studentCourseExercise", "SSS", s -> {
                Student student = Student.find(s.connection, s.user.id).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                Exercise e = exs.find(s.param(2)).orElseThrow(() -> new AppLogicException("Harjoitusta ei löytynyt!"));
                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .h3("Harjoituksen rakenne")
                        .p(s2 -> e.verboseView(s.connection))
                        .a(l -> l.x("studentCourseExerciseAnswer").x(s.params()), "Anna tehtävään uusi vastaus")
                        .a(l -> l.x("studentCourse").x(s.param(1)), "Takaisin");
            }),
            // pyytää dataa käyttäjältä vastaukseksi (tarkoitus oli ottaa liitetiedosto talteen, mutta UI on aika rajallinen siihen)
            new Menu("studentCourseExerciseAnswer", "SSS", s -> {
                Student student = Student.find(s.connection, s.user.id).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                Exercise e = exs.find(s.param(2)).orElseThrow(() -> new AppLogicException("Harjoitusta ei löytynyt!"));

                s.outputStream.println("Anna uusi vastaus tehtävään " + e.exerciseId + ":");
                String answer = s.inputStream.get();

                s.outputStream.println("Anna myös vapaavalintainen kommentti:");
                String comment = s.inputStream.get();

                e.upload(s.connection, answer, comment);

                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .a(l -> l.x("studentCourse").x(s.param(1)), "Takaisin");
            }),
            // poistaa opiskelijan kurssilta
            new Menu("partCourse", "SS", s -> {
                s.user.partCourse(s.connection, CourseInstance.findI(s.connection, s.param(1)).orElseThrow(() -> new AppLogicException("Kurssia ei löytynyt!")));
                return c -> c
                        .h1("Poistutaan kurssilta " + s.param(1))
                        .a("studentCourses", "Takaisin");
            }),
            // opettajalle, kurssin arviointiruutu
            new Menu("gradeCourse", "SS", s -> {
                CourseInstance course = CourseInstance.findI(s.connection, s.param(1)).orElseThrow(() -> new AppLogicException("Kurssia ei löytynyt!"));
                ExerciseSpecs specs = course.exerciseSpecs();
                List<Student> students = course.students(s.connection);
                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .h3("Harjoitusten rakenne:")
                        .li(specs.getExerciseDecls().stream().map(co -> c.p(co.overview())))
                        .h3("Pistelasku:")
                        .li(specs.getGradingDecls().stream().map(co -> c.p(co.toString())))
                        .h3("Suorittajia:")
                        .li(students.stream().map(st -> c.a(l -> l.x("gradeCourseStudent").x(s.params()).x(st.id),
                                c.p(s2 -> st.wholeNameId() + ": " + st.exercises(s.connection, s.param(1)).status()))))
                        .br()
                        .a(l -> l.x("abandonCourse").x(s.params()).x(s.user.id), "Lopeta kurssin opetus")
                        .a("gradeCourses", "Takaisin");
            }),
            // poistaa opettajan kurssin opettajista
            new Menu("abandonCourse", "SS", s -> {
                s.user.asTeacher().abandonCourse(s.connection, CourseInstance.findI(s.connection, s.param(1)).orElseThrow(() -> new AppLogicException("Kurssia ei löytynyt!")));
                return c -> c
                        .h1("Poistutaan kurssilta (tuhoaa kurssi-instanssin) " + s.param(1))
                        .a("gradeCourses", "Takaisin");
            }),
            // tietyn kurssin tietyn opiskelijan arviointi
            new Menu("gradeCourseStudent", "SSI", s -> {
                Student student = Student.find(s.connection, s.paramI(2)).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                Optional<CourseGrade> cg = CourseGrade.find(s.connection, student.id, s.param(1));
                Optional<ValRange> grade = exs.grade();
                boolean gradedOrNewExercises = exs.latest().map(latestEx -> cg.map(g -> g.gradeDate.compareTo(latestEx) < 0).orElse(false)).orElse(false);
                int gradeNum = grade.map(g -> g.min == g.max && gradedOrNewExercises ? (int) g.min : -1).orElse(-1);
                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .h2(student.wholeNameId())
                        .h3("Harjoitukset:")
                        .li(exs.exercises.stream().map(ex -> c.a(l -> l.x("gradeCourseStudentExercise").x(s.params()).x(ex.exerciseId), s2 -> c.p(ex.status(s.connection)))))
                        .p(exs.exercises.isEmpty() ? "Ei suorituksia!" : null)
                        .h3("Pistelasku:")
                        .li(exs.specs.getGradingDecls().stream().map(co -> c.p(co.toString())))
                        .h3("Arvosanat:")
                        .p("Pisteiden mukaan: " + grade.map(ValRange::toString).orElse("Kurssilla ei ole laskukaavaa arvosanalle!"))
                        .p("Kirjattu kurssiarvosana: " + cg.map(CourseGrade::shortOverview).orElse("-"))
                        .a(l -> l.x("gradeCourseStudentWith").x(s.params()).x(gradeNum), gradeNum == -1 ? null : "Hyväksy kurssiarvosana " + gradeNum)
                        .a(l -> l.x("gradeCourse").x(s.params()), "Takaisin");
            }),
            // tietyn kurssin tietyn opiskelijan kurssisuorituksen kirjaus hyväksyttäväksi opintorekisteriin
            new Menu("gradeCourseStudentWith", "SSII", s -> {
                Student student = Student.find(s.connection, s.paramI(2)).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                CourseGrade cg = new CourseGrade(s.paramI(2), s.param(1), s.paramI(3), s.connection.now(), s.user.id);
                cg.updateDB(s.connection);
                Exercises exs = student.exercises(s.connection, s.param(1));
                return c -> c
                        .h1("Kurssi " + s.param(1))
                        .h2(student.wholeNameId())
                        .h3("Kirjattu kurssisuoritus:")
                        .p(s2 -> cg.overview(s.connection))
                        .h3("Harjoitukset:")
                        .li(exs.exercises.stream().map(ex -> c.a(l -> l.x("gradeCourseStudentExercise").x(s.param(1)).x(s.paramI(2)).x(ex.exerciseId), s2 -> c.p(ex.status(s.connection)))))
                        .h3("Pistelasku:")
                        .li(exs.specs.getGradingDecls().stream().map(co -> c.p(co.toString())))
                        .a(l -> l.x("gradeCourseStudent").x(s.params()), "Takaisin");
            }),
            // tietyn kurssin tietyn opiskelijan tietyn harjoituksen arviointi pisteillä
            new Menu("gradeCourseStudentExercise", "SSIS", s -> {
                Student student = Student.find(s.connection, s.paramI(2)).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                Exercise e = exs.find(s.param(3)).orElseThrow(() -> new AppLogicException("Harjoitusta ei löytynyt!"));
                ExerciseSpec spec = e.specification.orElseThrow(() -> new AppLogicException("Harjoituksen kuvausta ei löytynyt!"));
                return c -> c
                        .h1(s.param(1))
                        .h2(student.wholeNameId())
                        .p(s2 -> e.verboseView(s.connection))
                        .br()
                        .x(e.graded() ? c.br() : c
                                .p("Arvioi pistein:")
                                .li(spec.possibleValues().stream().map(v -> c.a(l -> l.x("gradeCourseStudentExerciseWith").x(s.params()).x(v), v.toString() + " pistettä")))
                        )
                        .a(l -> l.x("gradeCourseStudent").x(s.params()), "Takaisin");
            }),
            // tietyn kurssin tietyn opiskelijan tietyn harjoituksen arvioinnin talletus
            new Menu("gradeCourseStudentExerciseWith", "SSISD", s -> {
                Student student = Student.find(s.connection, s.paramI(2)).orElseThrow(() -> new AppLogicException("Opiskelijaa ei löytynyt!"));
                Exercises exs = student.exercises(s.connection, s.param(1));
                Exercise e = exs.find(s.param(3)).orElseThrow(() -> new AppLogicException("Harjoitusta ei löytynyt!"));
                Exercise e2 = e.grade(s.connection, s.user.id, s.paramD(4), "Hienoa.");
                return c -> c
                        .h1(s.param(1))
                        .h2(student.wholeNameId())
                        .p(s2 -> "Kirjattu suoritus: " + e2.verboseView(s.connection))
                        .a(l -> l.x("gradeCourseStudent").x(s.params()), "Takaisin");
            }),
            // tietyn kurssin tietyn opiskelijan tietyn suorituksen kirjaus opintorekisteriin
            new Menu("approveCourse", "SISS", s -> {
                CourseGrade cg = CourseGrade.find(s.connection, s.paramI(1), s.param(2), s.param(3)).orElseThrow(() -> new AppLogicException("No such element"));
                CourseInstance ci = cg.course(s.connection);
                Student st = cg.student(s.connection);
                cg.approve(s.connection, s.user.id);
                return c -> c
                        .h1("Kurssisuorituksen kirjaus opintorekisteriin")
                        .p("Kirjattu opintorekisteriin: " + cg.gradeDate + ": " + ci.wholeNameId(30) + " - " + st.wholeNameId() + " - " + cg.adminDate)
                        .a("approveCourses", "Takaisin");
            }),
            // kurssisuoritusten kirjaaminen opintorekisteriin (hallinnon yleisnäkymä)
            new Menu("approveCourses", "S", s -> {
                List<CourseGrade> courseGradesWaiting = CourseGrade.waitingApproval(s.connection);
                List<CourseGrade> courseGradesApproved = CourseGrade.lastApproved(s.connection, 10);
                return c -> c
                        .h1("Hallinto")
                        .h3("Viimeksi opintorekisteriin merkitty:")
                        .li(courseGradesApproved.stream().map(cg -> c.p(ss -> cg.adminOverview(s.connection))))
                        .h3("Arvioituja suorituksia jonossa:")
                        .li(courseGradesWaiting.stream().map(cg -> c.a(l -> l.x("approveCourse").x(cg.studentId).x(cg.instanceId).x(cg.gradeDate), s2 -> c.p(cg.adminOverview(s.connection)))))
                        .br()
                        .a("main", "Takaisin päävalikkoon");
            }),
            // alustaa tietokannan
            new Menu("wipeDB", "S", s -> c -> c
                    .p(ss -> DBCleaner.initDB(s.connection, 0))
                    .a("main", "Takaisin päävalikkoon")
            ),
            // kirjautuu ulos
            new Menu("logout", "S", ss -> {
                ss.close();
                return c -> c.p("Kirjaudutaan ulos..");
            }));

    public DBApp(String dbFile, Supplier<String> inputStream, Consumer<Consumer<Void>> shutdownHook) throws SQLException, AppLogicException {
        this.dbFile = dbFile;
        this.inputStream = inputStream;
        this.shutdownHook = shutdownHook;
    }

    /**
     * Loops until a valid username/password pair is provided -> return the authenticated user
     * in case the user provides 'quit' as a name, return empty
     */
    private Optional<Student> login(SQLConnection connection) throws SQLException, AppLogicException {
        Optional<Student> login = Optional.empty();
        while (login.equals(Optional.empty())) {
            System.out.println("Enter your username:");
            String id = inputStream.get();
            if (id.equals("quit")) return login;
            System.out.println("Enter your password:");
            String pw = inputStream.get();
            login = Student.authenticate(connection, id, pw);
        }
        return login;
    }

    /**
     * In case a debug mode is requested, the db must contain a student&teacher&admin person, otherwise
     * nuke the db and try again.
     */
    private Optional<Student> findDebugUserOrNukeTheDBAndThenFindAgain(SQLConnection connection) throws SQLException, AppLogicException {
        Optional<Student> debugUser = Optional.empty();
        if (debugMode) {
            while (!debugUser.isPresent()) {
                try {
                    debugUser = connection.findFirst(Teacher::fromDB, "select * from personnel where isTeacher == true and isAdmin == true");
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }
                if (!debugUser.isPresent()) {
                    System.err.println("The database was broken (no admin & teacher entities for debugging). Rebuilding...");
                    DBCleaner.initDB(connection, 5);
                }
            }
        }
        return debugUser;
    }

    /**
     * Text mode UI's main loop:
     * 1) start a sql connection
     * 2) loop until user decides to quit
     * 3) activate shutdown hooks
     */
    public void run() {
        Path path = Paths.get(dbFile);

        String dbPath = path.toString();

        boolean emptyDB = !Files.exists(path);

        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        try (SQLConnection connection = SQLConnection.createConnection(slowMode ? "slow" + dbPath : dbPath, debugMode)) {
            if (emptyDB) {
                System.err.println("Database " + path + " did not exist. Created a new DB instance.");
                DBCleaner.initDB(connection, 0);
            }

            Optional<Student> debugUser = findDebugUserOrNukeTheDBAndThenFindAgain(connection);
            Optional<Student> user;
            do {
                if (debugMode)
                    System.out.println(new DBCleaner(connection).debug());

                // in debug mode, the first login attempt is done with the debug user
                user = debugUser.isPresent() ? debugUser : login(connection);
                debugUser = Optional.empty();

            } while (appLogic.control(inputStream, new MenuSession(user, connection, System.out, inputStream)));
        } catch (Exception e) {
            handleEx(e, shutdownHook);
            return;
        }

        shutdownHook.accept(s -> {
        });
    }

    /**
     * Exception pretty printer
     *
     * @param ex
     * @param shutdownHook
     */
    static void handleEx(Exception ex, Consumer<Consumer<Void>> shutdownHook) {
        try {
            throw ex;
        } catch (SQLException e) {
            System.err.println("SQL ERROR:" + e.getMessage());
            if (e.getSQLState() != null) System.err.println(e.getSQLState());
        } catch (Exception e) {
            System.err.println("ERROR:" + e.getMessage());
        }
        System.err.println(" ");
        shutdownHook.accept(s -> {
            for (Object o : ex.getStackTrace()) System.err.println(" -> " + o);
        });
    }

    /**
     * Initializes the text mode UI
     *
     * @param dbFile
     * @param shutdownHook
     * @param commandQueue
     */
    public static void init(String dbFile, Consumer<Consumer<Void>> shutdownHook, LinkedBlockingQueue<String> commandQueue) {
        Supplier<String> inputStream = () -> {
            while (true) {
                try {
                    return commandQueue.take();
                } catch (InterruptedException e) {
                }
                // in case the stdin breaks, avoid making the GUI totally unresponsive
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        };
        new Thread(() -> {
            try {
                new DBApp(dbFile, inputStream, shutdownHook).run();
            } catch (Exception e) {
                handleEx(e, shutdownHook);
            }
        }).start();
    }
}