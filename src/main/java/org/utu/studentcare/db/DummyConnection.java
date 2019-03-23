package org.utu.studentcare.db;

import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.applogic.ExerciseSpecs;
import org.utu.studentcare.db.orm.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Used for pre-rendering the GUI (state graph, link validation).
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 */
class DummyConnection implements SQLConnection {
    @Override
    public void setDelayedCommit(boolean status) {
    }

    @Override
    public boolean performCustomStatement(String statement) {
        return true;
    }

    private final String rule = "{ DEF dummy[0..2]: \"dummy data\", DEF dummyb[0..2]: \"dummy data\", arvosana = 5 }";

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findFirst(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws AppLogicException {
        String s = statement.toLowerCase();
        Object ret = new Object();
        ExerciseSpecs specs = ExerciseSpecs.generateFrom(rule);
        if (statement.contains("from 'courses'"))
            ret = new Course("dummy", "dummy", "dummy", 0);
        if (statement.contains("from 'courseinstances'")) {
            ret = new CourseInstance(new Course("dummy", "dummy", "dummy", 0), "dummy", rule);
        }
        if (statement.contains("from 'personnel'"))
            ret = new Student("dummy", "dummy", "dummy", 0, "dummy", "dummy", "dummy", true, true);

        if (statement.contains("from 'coursegrades'"))
            ret = new CourseGrade(0, "dummy", 5, "2019-01-01", 1);
        if (statement.contains("from 'exercises'"))
            ret = new Exercise(0, "dummy", "dummy", "dummy", "2019-01-02", 0, "dummy", "dummy", 0.0, "", Optional.of(specs.getExerciseDecls().get(0)));
        return Optional.of((T) ret);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws AppLogicException {
        ExerciseSpecs specs = ExerciseSpecs.generateFrom(rule);
        if (statement.contains("from 'exercises'")) {
            return List.of(
                    (T) (new Exercise(0, "dummy", "dummy", "dummy", "2019-01-02", 0, "dummy", "dummy", 0.0, "", Optional.of(specs.getExerciseDecls().get(0)))),
                    (T) (new Exercise(0, "dummy", "dummyb", "dummy", "2019-01-02", 1, "dummy", "dummy", 2.0, "2019-01-02", Optional.of(specs.getExerciseDecls().get(1))))
            );
        }
        return findFirst(mapping, statement, data).stream().collect(Collectors.toList());
    }

    @Override
    public int insert(String statement, Object... data) {
        return 1;
    }

    @Override
    public int update(String statement, Object... data) {
        return 1;
    }

    @Override
    public int count(String statement, Object... data) {
        return 0;
    }

    @Override
    public int delete(String statement, Object... data) {
        return 1;
    }

    @Override
    public String now() {
        return "dummy";
    }

    @Override
    public void close() {
    }
}
