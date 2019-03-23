package org.utu.studentcare.textmodeui;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public class DOM extends AbstractDOM<SQLConnection.SQLFunction<MenuSession, String>, SQLConnection.SQLFunction<MenuSession, Component>, SQLConnection.SQLFunction<MenuSession, SQLConnection.SQLFunction<ComponentContainer<?>, ComponentContainer<?>>>> {
    private final boolean robust;

    protected DOM(boolean robust) {
        this.robust = robust;
    }

    protected DOM() {
        this(false);
    }

    @Override
    protected String fromY(SQLConnection.SQLFunction<MenuSession, String> y, MenuSession s) throws SQLException, AppLogicException {
        try {
            return y.apply(s);
        } catch (SQLException | AppLogicException e) {
            if (robust) {
                System.err.println("DOM evaluation faced: "+e);
                return e.getMessage();
            }
            else throw e;
        }
    }

    @Override
    protected SQLConnection.SQLFunction<MenuSession, String> toY(String x) {
        return s -> x;
    }

    @Override
    protected Component fromX(SQLConnection.SQLFunction<MenuSession, Component> y, MenuSession s) throws SQLException, AppLogicException {
        try {
            return y.apply(s);
        } catch (SQLException | AppLogicException e) {
            if (robust) {
                System.err.println("DOM evaluation faced: "+e);
                return new Text(e.getMessage(), null);
            }
            else throw e;
        }
    }

    @Override
    protected SQLConnection.SQLFunction<MenuSession, Component> toX(Component x) {
        return s -> x;
    }

    @Override
    protected SQLConnection.SQLFunction<MenuSession, Component> toX_(SQLConnection.SQLFunction<MenuSession, SQLConnection.SQLFunction<ComponentContainer<?>, ComponentContainer<?>>> x) throws SQLException, AppLogicException {
        return s -> x.apply(s).apply(new ChainingComponentContainer());
    }

    public class ChainingComponentContainer extends AbstractComponentContainer<ChainingComponentContainer> {
        public ChainingComponentContainer() {
            super();
        }

        public List<Component> contents() {
            return contents;
        }

        public ChainingComponentContainer thisX() {
            return this;
        }

        public ChainingComponentContainer newX() {
            return new ChainingComponentContainer();
        }

        public ChainingComponentContainer(Collection<Component> source, Component... more) {
            super(source);
            contents.addAll(Arrays.asList(more));
        }

        public ChainingComponentContainer(ComponentContainer<?> source, Component... more) {
            this(source.contents(), more);
        }

        public ChainingComponentContainer x(Component c) {
            return new ChainingComponentContainer(contents, c);
        }

        public ChainingComponentContainer br() {
            return x(new Text(toY("")));
        }

        public ChainingComponentContainer h1(String content) {
            return p("--== " + content + " ==--").br();
        }

        public ChainingComponentContainer h2(String content) {
            return p("-- " + content + " --").br();
        }

        public ChainingComponentContainer h3(String content) {
            return br().p("= " + content);
        }

        public ChainingComponentContainer p(String content) {
            return x(new Text(toY(content)));
        }

        public ChainingComponentContainer p(SQLConnection.SQLFunction<MenuSession, String> content) {
            return x(new Text(content));
        }

        public ChainingComponentContainer nest(SQLConnection.SQLFunction<ComponentContainer<?>, ComponentContainer<?>> x) {
            try {
                return x(x.apply(new ChainingComponentContainer()));
            }
            catch(SQLException | AppLogicException e) {
                return new ChainingComponentContainer().p(e.getMessage());
            }
        }

        public ChainingComponentContainer a(String target, Component content) {
            return x(new Link(new LinkParts().x(target), content));
        }

        public ChainingComponentContainer a(String target, String content) {
            return x(new Link(new LinkParts().x(target), new Text(content, null)));
        }

        public ChainingComponentContainer a_(String target, SQLConnection.SQLFunction<MenuSession, String> content) {
            return x(new Link(new LinkParts().x(target), new Text(content)));
        }

        public ChainingComponentContainer a(String target, SQLConnection.SQLFunction<MenuSession, Component> content) {
            return x(new Link(new LinkParts().x(target), new WrapperContainer(content)));
        }

        public ChainingComponentContainer a(Function<LinkParts, LinkParts> target, String content) {
            return x(new Link(target, content));
        }

        public ChainingComponentContainer a(Function<LinkParts, LinkParts> target, Component content) {
            return x(new Link(target.apply(new LinkParts()), content));
        }

        public ChainingComponentContainer a_(Function<LinkParts, LinkParts> target, SQLConnection.SQLFunction<MenuSession, String> content) {
            return x(new Link(target, content));
        }

        public ChainingComponentContainer a(Function<LinkParts, LinkParts> target, SQLConnection.SQLFunction<MenuSession, Component> content) {
            return x(new Link(target.apply(new LinkParts()), new WrapperContainer(content)));
        }

        @Override
        public String toString() {
            return contents.size() + "[" + contents.stream().map(Object::toString).collect(Collectors.joining()) + "]";
        }
    }
}