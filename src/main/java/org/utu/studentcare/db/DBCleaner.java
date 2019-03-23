package org.utu.studentcare.db;

import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.applogic.TaskStuff;
import org.utu.studentcare.db.orm.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Populates the database with randomized stuff.
 * <p>
 * How to use:
 * DBCleaner dbc = new DBCleaner(dbConnection);
 * dbc.wipeTables().populateTables();
 * or
 * DBCleaner.initDB(dbConnection, delay);
 * <p>
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class DBCleaner extends TaskStuff {
    private final SQLConnection connection;

    public DBCleaner(SQLConnection connection) {
        this.connection = connection;
    }

    final String schemaFile = "dbschema.sql";

    public static final String[] tables = {
            "coursegrades",
            "exercises",
            "coursestudents",
            "courseteachers",
            "courseinstances",
            "courses",
            "personnel",
            "programs"
    };

    public static String initDB(SQLConnection connection, int delay) throws SQLException, AppLogicException {
        if (connection.now().equals("dummy")) return "Cancelled";

        for (int i = delay; i > 0; i--) {
            System.out.println("DB initialization starting in " + i + " seconds!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        new DBCleaner(connection).rebuildDB().wipeTables().populateTables();
        return "Done";
    }

    public DBCleaner wipeTable(String table) throws SQLException, AppLogicException {
        String sanitizedTable = table.replace(';', ' ').split(" ")[0].toLowerCase();
        connection.delete("delete from " + sanitizedTable);
        taskUpdate("Table " + table + " wiped.");

        return this;
    }

    public DBCleaner wipeTables() throws SQLException, AppLogicException {
        taskStarted("Wiping of the DB", tables.length);
        for (String table : tables)
            wipeTable(table);
        taskFinished("Wiping of the DB");
        return this;
    }

    String readDBSchema(String file) {
        try (Stream<String> stream = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(file))).lines()) {
            return stream.collect(Collectors.joining());
        } catch (Exception e) {
            throw new Error("Could not DB schema file " + file + ": " + e.getMessage());
        }
    }

    public DBCleaner rebuildDB() throws SQLException, AppLogicException {
        String[] schemaDescription = readDBSchema(schemaFile).split(";");
        taskStarted("Rebuilding of the DB", schemaDescription.length - 2);

        connection.setDelayedCommit(true);

        for (int i = 1; i < schemaDescription.length - 1; i++)
            connection.performCustomStatement(schemaDescription[i]+";");
                //throw new SQLException("Statement [" + schemaDescription[i] + "] failed to execute!");

        connection.setDelayedCommit(false);

        taskFinished("Rebuilding of the DB");
        return this;
    }

    public DBCleaner populateTables() throws SQLException, AppLogicException {
        connection.setDelayedCommit(true);

        StudentGenerator generator = new StudentGenerator();

        taskStarted("Populating the programs table", generator.programIDs().size());
        {
            for (String prog : generator.programIDs()) {
                if (connection.insert("insert into 'programs' values (?,?)", prog, generator.programName(prog)) != 1)
                    throw new SQLException("Program table update failed!");

                taskUpdate();
            }
        }
        taskFinished("Populating the programs table");

        List<String> courses = new CSVNames().courses;
        taskStarted("Populating the courses table", courses.size());
        {
            for (String courseDesc : courses) {
                String[] parts = courseDesc.split(",");

                int credits = (int) Double.parseDouble(parts[parts.length - 1]);
                String shortName = parts[parts.length - 2];
                String description = "";
                String courseName = Arrays.asList(parts).stream().limit(parts.length - 2).collect(Collectors.joining(","));
                Course c = new Course(courseName, shortName, description, credits);
                c.updateDB(connection);
                taskUpdate();
            }
        }
        taskFinished("Populating the courses table");

        taskStarted("Populating the courseinstances table", connection.count("select count(*) from 'courses'"));
        {
            for (Course c : Course.all(connection)) {
                for (int year = 2015; year <= 2019; year++) {
                    int exerciseCount = new Random().nextInt(10);
                    int points = 0;
                    StringBuilder gradingRule = new StringBuilder("{");
                    for (char i = 'a'; i < 'a' + exerciseCount; i++) {
                        int p = new Random().nextInt(5) + 5;
                        gradingRule.append("DEF harj").append(i).append("[0..").append(p).append("]: \"Harjoitustehtävä ").append(i).append("\",");
                        points += p;
                    }

                    gradingRule.append("pisteet = ");
                    for (char i = 'a'; i < 'a' + exerciseCount; i++)
                        gradingRule.append("harj").append(i).append("+");

                    gradingRule.append("0, arvosana = IF pisteet < ").append(points / 2).append(" THEN 0 ELSE 5 }");

                    CourseInstance instance = new CourseInstance(c, c.shortName + "_" + year, gradingRule.toString());
                    if (!instance.updateDB(connection))
                        throw new SQLException("Courseinstances table update failed!");

                }
                taskUpdate();
            }
        }
        taskFinished("Populating the courseinstances table");

        int studentCount = 10000;
        taskStarted("Populating the personnel table", studentCount);
        {
            for (int i = 0; i < studentCount; i++) {
                taskUpdate();
                if (!generator.generateStudent().updateDB(connection))
                    throw new SQLException("personnel table update failed!");
            }
        }
        taskFinished("Populating the personnel table");

        taskStarted("Joining courses", 1);
        {
            CourseInstance instance = CourseInstance.findI(connection, "DTEK1049", "2019").orElseThrow();
            instance = instance.setGrading(
                    "{" +
                            "DEF harjaa[0..2]: \"viikkoharjoitukset a1-2\"," +
                            "DEF harjab[0..2]: \"viikkoharjoitukset a3-4\"," +
                            "DEF harjac[0..2]: \"viikkoharjoitukset a5-6\"," +
                            "DEF harjb[0..8]: \"käytettävyysharjoitus\"," +
                            "DEF harjc[0..8]: \"selvitystyö\"," +
                            "DEF harjd[0..6]: \"ohjelmointiharjoitus\"," +
                            "pisteet = harjaa+harjab+harjac+harjb+harjc+harjd," +
                            "arvosana = IF pisteet < 14 THEN 0 ELSE IF pisteet < 17 THEN 1 ELSE IF pisteet < 20 THEN 2 ELSE IF pisteet < 23 THEN 3 ELSE IF pisteet < 26 THEN 4 ELSE 5" +
                            "}");
            instance.updateDB(connection);

            Teacher t = null;

            int i = 0;
            for (Student s : connection.findAll(Student::fromDB, "select * from 'personnel' limit 50")) {
                s.joinCourse(connection, instance);
                if (i == 0 || i == 1) {
                    s = t = new Teacher(s);
                    if (!t.teachCourse(connection, instance))
                        throw new SQLException("failed to add course teacher!");
                }
                if (i == 0 || i == 2) {
                    s = new Secretary(s);
                }
                if (i > 2 && i < 10) {
                    instance.grade(connection, s, i % 5 + 1, t);
                }
                s.updateDB(connection);
                i++;
            }
        }
        taskFinished("Joining courses");

        connection.setDelayedCommit(false);

        return this;
    }

    public String debug() {
        final StringBuilder output = new StringBuilder();
        try {
            int count = 0;

            for (String table : tables) {
                int rows = connection.count("select count(*) from '" + table + "'");
                count += rows;
                output.append("Table ").append(table).append(" contains ").append(rows).append(" rows.\n");
            }
            if (count == 0) {
                initDB(connection, 5);
                return debug();
            }

            output.append("\n");

            output.append("For debugging purposes, some users:\n");

            connection.findFirst(Teacher::fromDB, "select * from personnel where isTeacher == true").ifPresent(t ->
                    output.append("\nTeacher: ").append(t.wholeName()).append("\nid:").append(t.username).append(" pw:").append(t.password));

            connection.findFirst(Secretary::fromDB, "select * from personnel where isAdmin == true").ifPresent(t ->
                    output.append("\nSecretary: ").append(t.wholeName()).append("\nid:").append(t.username).append(" pw:").append(t.password));

            connection.findFirst(Teacher::fromDB, "select * from personnel where isAdmin == false and isTeacher == false").ifPresent(t ->
                    output.append("\nNormal student: ").append(t.wholeName()).append("\nid:").append(t.username).append(" pw:").append(t.password));
        } catch (SQLException | AppLogicException e) {
            output.append("Error: ").append(e.getMessage());
        }

        output.append("\n");
        return output.toString();
    }
}
