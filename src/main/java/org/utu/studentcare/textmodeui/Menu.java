package org.utu.studentcare.textmodeui;

import org.utu.studentcare.db.SQLConnection;

import java.util.function.Function;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public class Menu {
    public final String name;
    public final SQLConnection.SQLFunction<MenuSession, Component> content;
    public final String apiSignature;

    public Menu(String name, Component content) {
        this(name, "", s -> content, 0);
    }

    public Menu(String name, String apiSignature, SQLConnection.SQLFunction<MenuSession, Component> content, int i) {
        this.name = name;
        this.apiSignature = apiSignature;
        this.content = content;
    }

    public Menu(String name, String apiSignature, SQLConnection.SQLFunction<MenuSession, Function<DOM.ChainingComponentContainer, Component>> generator) {
        this(name, apiSignature, s -> generator.apply(s).apply(new DOM().new ChainingComponentContainer()), 0);
    }
}