package org.utu.studentcare.textmodeui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
abstract class AbstractComponentContainer<X extends AbstractComponentContainer> extends AbstractComponent implements ComponentContainer<X> {
    final List<Component> contents;

    AbstractComponentContainer(Collection<Component> source) {
        super(2);
        contents = new ArrayList<>(source);
    }

    AbstractComponentContainer() {
        this(List.of());
    }
}