package org.utu.studentcare.textmodeui;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
abstract class AbstractComponent implements Component {
    private final int type;

    AbstractComponent(int type) {
        this.type = type;
    }

    @Override
    public int type() {
        return type;
    }
}