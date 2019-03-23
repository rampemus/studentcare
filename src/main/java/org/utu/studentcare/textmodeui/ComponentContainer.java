package org.utu.studentcare.textmodeui;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public interface ComponentContainer<X extends ComponentContainer> extends Component {
    X thisX();

    X newX();

    List<Component> contents();

    default X add(Component source) {
        contents().add(source);
        return thisX();
    }

    default X add(Collection<Component> source) {
        contents().addAll(source);
        return thisX();
    }

    default X add(Stream<Component> source) {
        return add(source.collect(Collectors.toList()));
    }

    default X x(Component c) {
        X x = newX();
        for (Component c2 : contents())
            x.add(c2);
        x.add(c);
        return x;
    }

    default X li(Collection<Component> source) {
        X x = newX();
        for (Component c2 : contents())
            x.add(c2);
        for (Component c2 : source)
            x.add(c2);
        return x;
    }

    default X li(Stream<Component> source) {
        X x = newX();
        for (Component c2 : contents())
            x.add(c2);
        for (Component c2 : source.collect(Collectors.toList()))
            x.add(c2);
        return x;
    }

    default <Y> X x(Stream<Y> source, SQLConnection.SQLFunction<Y, Component> transform, SQLConnection.SQLFunction<Y, Component> failure) {
        return li(source.map(elem -> {
            try {
                return transform.apply(elem);
            } catch (SQLException | AppLogicException e) {
                try {
                    return failure.apply(elem);
                } catch (SQLException | AppLogicException e2) {
                    return new DOM().new Text(e.getMessage() + " ; " + e2.getMessage(), null);
                }
            }
        }));
    }
    default <Y> X x(List<Y> source, SQLConnection.SQLFunction<Y, Component> transform, SQLConnection.SQLFunction<Y, Component> failure) {
        return x(source.stream(), transform, failure);
    }
}