package org.utu.studentcare.calculator;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;
import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.applogic.ExerciseSpecs;
import static org.parboiled.errors.ErrorUtils.printParseErrors;

/**
 * Course specification grammar.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class SpecParser {
    /**
     * Parses the given string.
     *
     * Example:
     * String input = "{
     *     DEF exercisea[0..5]: \"excercise a that can be graded with 0..5 points\",
     *     DEF exerciseb[0..2]: \"excercise b that can be graded with 0..2 points\",
     *     sum = exercisea + exerciseb * 2,
     *     grade = IF sum < 5 THEN 0 ELSE if sum < 7 THEN 1 ELSE 5
     * }"
     * SpecParser.parseExerciseSpec(input);
     *
     * @param input
     * @return
     * @throws AppLogicException
     */
    public static ExerciseSpecs parseExerciseSpec(String input) throws AppLogicException {
        SpecParserGrammar parser = Parboiled.createParser(SpecParserGrammar.class);

        ParsingResult<?> result = new RecoveringParseRunner(parser.InputLine()).run(input);

        if (result.hasErrors()) {
            throw new AppLogicException("\nParse Error:\n" + printParseErrors(result));
        }

        return (ExerciseSpecs) result.parseTreeRoot.getValue();
    }
/*
    public static void test() throws AppLogicException {
        ValueContext c = new ValueContext();
        ValueContext c2 = new ValueContext();
        c2.put("harjaa", 0);
        c2.put("harjab", 0);
        c2.put("harjac", 0);
        c2.put("harjb", 4);
        c2.put("harjc", 6);

        String input = "{" +
                "DEF harjaa[0..2]: \"viikkoharjoitukset a1-2\"," +
                "DEF harjab[0..2]: \"viikkoharjoitukset a3-4\"," +
                "DEF harjac[0..2]: \"viikkoharjoitukset a5-6\"," +
                "DEF harjb[0..8]: \"käytettävyysharjoitus\"," +
                "DEF harjc[0..8]: \"selvitystyö\"," +
                "DEF harjd[0..6]: \"ohjelmointiharjoitus\"," +
                "pisteet = harjaa+harjab+harjac+harjb+harjc+harjd," +
                "arvosana = IF pisteet < 14 THEN 0 ELSE IF pisteet < 17 THEN 1 ELSE IF pisteet < 20 THEN 2 ELSE IF pisteet < 23 THEN 3 ELSE IF pisteet < 26 THEN 4 ELSE 5" +
                "}";

        ExerciseSpecs value = parseExerciseSpec(input);
        ExerciseSpecs value2 = parseExerciseSpec(value.toString());
        System.out.println("Starting");
        if (!value.toString().equals(value2.toString()))
            throw new Error("Problem with serialization!");

        System.out.println(input);
        System.out.println(value);
        System.out.println(value2);
        System.out.println(c.evaluate(value));
        System.out.println(c2.evaluate(value2));
    }
*/
}
