package org.utu.studentcare.applogic;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a course exercise description.
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public interface ExerciseSpec {
    /**
     * @return Name of the exercise (lower case, no numbers / spaces / non-letter characters).
     */
    String getId();

    /**
     * @return Description of the exercise.
     */
    String getDescription();

    /**
     * @return The range of possible points for this exercise. (usually 0..something)
     */
    ValRange getRange();

    /**
     * @return Pretty printed exercise description.
     */
    default String overview() {
        return getDescription() + " (" + getId() + "): " + getRange() + " pistettä";
    }

    /**
     * By default returns all integer values between getRange().min .. getRange().max.
     *
     * @return Possible values for this exercise. Overwrite if you need fractional numbers.
     */
    default List<Double> possibleValues() {
        ArrayList<Double> tmp = new ArrayList<>();
        for (int i = (int) getRange().min; i <= getRange().max; i++)
            tmp.add(i * 1.0);
        return tmp;
    }
}