package org.utu.studentcare.applogic;

/**
 * Represents a single variable which can be evaluated.
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public interface GradingSpec {
    /**
     * Name of the variable.
     *
     * @return
     */
    String getId();

    /**
     * The resulting value range of this variable.
     * The value context is used when referring to dependent variables/exercise results.
     * The values need to be computed in the correct order to avoid missing dependencies.
     *
     * @param context
     * @return
     */
    ValRange value(ValueContext context);
}