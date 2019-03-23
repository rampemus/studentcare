package org.utu.studentcare.db.orm;

import org.utu.studentcare.applogic.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Represents all the exercises of a single course a specific student has or could have answered.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Exercises {
    public final List<Exercise> exercises;
    public final ExerciseSpecs specs;

    public Exercises(int studentId, String instanceId, List<Exercise> done, ExerciseSpecs specs) {
        List<Exercise> tmp = new ArrayList<>();

        for (ExerciseSpec s : specs.getExerciseDecls()) {
            Exercise e = new Exercise(studentId, instanceId, s.getId(), s);
            Exercise e2 = done.stream().filter(ex -> ex.exerciseId.equals(s.getId())).findFirst().map(ex -> ex.withSpec(s)).orElse(e);
            tmp.add(e2);
        }
        this.exercises = tmp;
        this.specs = specs;
    }

    public Optional<String> latest() {
        return exercises.stream().map(e -> e.gradeDate).max(Comparator.naturalOrder());
    }

    public Optional<Exercise> find(String name) {
        return exercises.stream().filter(ex -> ex.exerciseId.equals(name)).findFirst();
    }

    public Optional<ValRange> grade() {
        ValueContext c = new ValueContext();
        for (Exercise e : exercises) {
            if (e.graded())
                c.put(e.exerciseId, e.grade);
        }

        try {
            return Optional.of(c.evaluate(specs).get("arvosana"));
        } catch (AppLogicException e) {
            try {
                return Optional.of(c.evaluate(specs).get("grade"));
            } catch (AppLogicException e2) {
                return Optional.empty();
            }
        }
    }

    public String status() {
        int done = 0, graded = 0;
        for (Exercise e : exercises) {
            if (!e.uploadDate.equals(""))
                done++;
            if (!e.gradeDate.equals(""))
                graded++;
        }

        String tmp = "";
        if (done > 0) {
            tmp += done + " tehty";
            if (graded > done)
                tmp += ", " + (graded - done) + " arvioimatta!";
        } else {
            tmp = "ei suorituksia.";
        }

        return tmp;
    }
}