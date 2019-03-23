package org.utu.studentcare.applogic;

/**
 * A floating point value range. Can be used for representing e.g. course points and grades.
 */
// TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
public class ValRange {
    /** lower bound of the range */
    public final double min;

    /** upper bound of the range */
    public final double max;

    public ValRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public ValRange() {
        this(Double.NaN, Double.NaN);
    }

    private static String round(double value) {
        int i = (int) Math.round(value * 10);
        if (i % 10 == 0)
            return "" + (i / 10);
        else
            return "" + (i / 10) + "." + (i % 10);
    }

    /**
     * @return Pretty printed representation of the range.
     */
    @Override
    public String toString() {
        String minS = round(min);
        String maxS = round(max);
        if (minS.equals(maxS)) return minS;

        return minS + ".." + maxS;
    }
}