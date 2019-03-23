package org.utu.studentcare.textmodeui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
class EvaluatedDOM extends AbstractDOM<String, Component, AbstractComponentContainer<?>> {
    @Override
    protected String fromY(String y, MenuSession s) {
        return y;
    }

    @Override
    protected String toY(String y) {
        return y;
    }

    @Override
    protected Component fromX(Component x, MenuSession s) {
        return x;
    }

    @Override
    protected Component toX(Component x) {
        return x;
    }

    @Override
    protected Component toX_(AbstractComponentContainer<?> x) {
        return x;
    }

    /*
    //@Override
    protected AbstractComponentContainer<?> fromZ(AbstractComponentContainer<?> y, MenuSession s) {
        return y;
    }

    //@Override
    protected AbstractComponentContainer<?> toZ(AbstractComponentContainer<?> x) {
        return x;
    }
    */

    @SuppressWarnings("unchecked")
    List<String> print(Component component, Supplier<Integer> linkCounter) {
        List<String> content = new ArrayList<>();

        switch (component.type()) {
            case 1:
                throw new Error("This was supposed to be evaluated!");
                //return print(((WrapperContainer) component).content, linkCounter);
            case 2:
                for (Component c : ((AbstractComponentContainer<?>) component).contents)
                    content.addAll(print(c, linkCounter));
                return content;
            case 3:
                String tContent = ((Text) component).content;
                return tContent == null ? List.of() : List.of(tContent);
            case 4:
                for (String line : print(((Link) component).child, linkCounter))
                    content.add(linkCounter.get() + ") " + line);
                return content;
        }
        throw new Error("Unknown node: " + component);
    }

    @SuppressWarnings("unchecked")
    List<SimpleLink> findLinks(Component component, Supplier<Integer> linkCounter) {
        switch (component.type()) {
            case 1:
                throw new Error("This was supposed to be evaluated!");
                //return findLinks(((WrapperContainer) component).content, linkCounter);
            case 2:
                List<SimpleLink> content = new ArrayList<>();
                for (Component c : ((AbstractComponentContainer<?>) component).contents)
                    content.addAll(findLinks(c, linkCounter));
                return content;
            case 3:
                return List.of();
            case 4:
                String output = String.join("", print(((Link) component).child, linkCounter));
                return output.equals("") ? List.of() : List.of(new SimpleLink(linkCounter.get().toString(), ((Link) component).target.parts, output));
        }
        throw new Error("Unknown node: " + component);
    }
}
