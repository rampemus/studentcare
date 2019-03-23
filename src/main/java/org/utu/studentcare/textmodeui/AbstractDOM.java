package org.utu.studentcare.textmodeui;

import org.utu.studentcare.applogic.AppLogicException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public abstract class AbstractDOM<Y, X, Z> {
    protected abstract String fromY(Y y, MenuSession s) throws SQLException, AppLogicException;

    protected abstract Component fromX(X x, MenuSession s) throws SQLException, AppLogicException;

    protected abstract Y toY(String x);

    protected abstract X toX(Component x);

    protected abstract X toX_(Z x) throws SQLException, AppLogicException;

    //protected abstract AbstractComponentContainer<?> fromZ(Z x, MenuSession s) throws SQLException;

    //protected abstract Z toZ(AbstractComponentContainer<?> x);

    class WrapperContainer extends AbstractComponent {
        final X content;

        public WrapperContainer(Component content, Void foo) {
            this(toX(content));
        }

        public WrapperContainer(X content) {
            super(1);
            this.content = content;
        }

        @Override
        public String toString() {
            return "{" + content.toString() + "}";
        }
    }

    private class Container extends AbstractComponentContainer<Container> {
        @Override
        public Container thisX() {
            return this;
        }

        @Override
        public Container newX() {
            return new Container();
        }

        public List<Component> contents() { return contents; }
    }

    AbstractComponentContainer<Container> getContainer() {
        return new Container();
    }


    class Text extends AbstractComponent {
        final Y content;

        public Text(Y content) {
            super(3);
            this.content = content;
        }

        public Text(String content, Void foo) {
            this(toY(content));
        }

        @Override
        public String toString() {
            return content == null ? "(null)" : content.toString();
        }
    }

    public class LinkParts {
        final List<Y> parts;

        public LinkParts() {
            this(List.of());
        }

        LinkParts(Collection<Y> source) {
            this.parts = new ArrayList<>(source);
        }

        LinkParts(Collection<Y> source, Y more) {
            this(source);
            parts.add(more);
        }

        public LinkParts x(String part) {
            return new LinkParts(parts, toY(part));
        }

        public LinkParts x(List<String> newParts) {
            LinkParts tmp = this;

            for(String s: newParts)
                tmp = tmp.x(s);

            return tmp;
        }

        public LinkParts x(int part) {
            return new LinkParts(parts, toY(""+part));
        }

        public LinkParts x(double part) {
            return new LinkParts(parts, toY(""+part));
        }

        public LinkParts x(Y part) {
            return new LinkParts(parts, part);
        }
    }

    class Link extends AbstractComponent {
        final LinkParts target;
        final Component child;

        public Link(LinkParts target, Component child) {
            super(4);
            this.target = target;
            this.child = child;
        }

        public Link(Function<LinkParts, LinkParts> target, String content) {
            this(target.apply(new LinkParts()), new Text(content, null));
        }

        public Link(Function<LinkParts, LinkParts> target, Y content) {
            this(target.apply(new LinkParts()), new Text(content));
        }

        @Override
        public String toString() {
            return target.toString() + "->" + child.toString();
        }
    }

    protected EvaluatedDOM.LinkParts evaluate(LinkParts linkParts, MenuSession session) throws SQLException, AppLogicException {
        List<String> parts = new ArrayList<>();
        for (Y y : linkParts.parts)
            parts.add(fromY(y, session));
        return new EvaluatedDOM().new LinkParts(parts);
    }

    @SuppressWarnings("unchecked")
    Component evaluate(Component component, MenuSession session) throws SQLException, AppLogicException {
        //System.out.println("trace: "+component.toString());
        switch (component.type()) {
            case 1:
                return evaluate(fromX(((WrapperContainer) component).content, session), session);
            case 2:
                List<Component> comps = new ArrayList<>();
                ComponentContainer<?> container = (ComponentContainer<?>)component;
                for (Component c : container.contents()) {
                    comps.add(evaluate(c, session));
                }
                return new EvaluatedDOM().getContainer().add(comps);
            case 3:
                return new EvaluatedDOM().new Text(fromY(((Text) component).content, session), null);
            case 4:
                return new EvaluatedDOM().new Link(evaluate(((Link) component).target, session), evaluate(((Link) component).child, session));
            default:
                throw new Error("Unknown node: " + component);
        }
    }
}
