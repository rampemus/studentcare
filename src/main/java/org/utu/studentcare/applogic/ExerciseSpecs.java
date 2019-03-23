package org.utu.studentcare.applogic;

import org.utu.studentcare.calculator.SpecParser;

import java.util.List;

/**
 * Represents a list of course exercise descriptions and rules for variables that are used for
 * determining the course grading.
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
  */
public interface ExerciseSpecs {
    /**
     * Exercise descriptions.
     *
     * @return
     */
    List<ExerciseSpec> getExerciseDecls();

    /**
     * Variable descriptions.
     *
     * @return
     */
    List<GradingSpec> getGradingDecls();

    /**
     * Creates a new specification of exercises and variables using the course description.
     * The syntax is described in SpecParserGrammar.
     * See DBCleaner for examples.
     *
     * Example:
     * {
     *     DEF exercisea[0..5]: "excercise a that can be graded with 0..5 points",
     *     DEF exerciseb[0..2]: "excercise b that can be graded with 0..2 points",
     *     sum = exercisea + exerciseb * 2,
     *     grade = IF sum < 5 THEN 0 ELSE if sum < 7 THEN 1 ELSE 5
     * }
     *
     * @param input course description
     * @return
     * @throws AppLogicException
     */
    static ExerciseSpecs generateFrom(String input) throws AppLogicException {
        return SpecParser.parseExerciseSpec(input);
    }
}