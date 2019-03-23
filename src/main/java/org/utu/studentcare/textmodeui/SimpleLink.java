package org.utu.studentcare.textmodeui;

import java.util.List;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
class SimpleLink {
    public final String name;
    public final List<String> parts;
    public final String content;

    SimpleLink(String name, List<String> parts, String content) {
        this.name = name;
        this.parts = parts;
        this.content = content;
    }
}