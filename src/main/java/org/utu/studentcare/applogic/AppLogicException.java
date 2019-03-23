package org.utu.studentcare.applogic;

/**
 * Exception for application business logic.
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class AppLogicException extends Exception {
    public AppLogicException(String message) {
        super(message);
    }
}