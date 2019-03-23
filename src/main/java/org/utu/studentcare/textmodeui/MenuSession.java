package org.utu.studentcare.textmodeui;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.applogic.Session;
import org.utu.studentcare.db.orm.Student;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public class MenuSession extends Session {
    private List<String> origin;
    private List<String> route;
    private List<String> routeSignature;

    public MenuSession(Student user, SQLConnection connection, PrintStream outputStream, Supplier<String> inputStream) {
        super(user, connection, outputStream, inputStream);
    }

    public MenuSession(Optional<Student> user, SQLConnection connection, PrintStream outputStream, Supplier<String> inputStream) {
        super(user, connection, outputStream, inputStream);
    }

    void navigate(List<String> route) {
        origin = this.route;
        this.route = new ArrayList<>();
        this.routeSignature = new ArrayList<>();
        for (String o : route) {
            try {
                Integer.parseInt(o);
                routeSignature.add("I");
            } catch (Exception e) {
                try {
                    Double.parseDouble(o);
                    routeSignature.add("D");
                } catch (Exception e2) {
                    routeSignature.add("S");
                }
            }
            this.route.add(o);
        }
    }

    public String origin() {
        return origin.stream().reduce((a, b) -> a + "/" + b).orElseGet(this::menu);
    }

    public String route() {
        return route.stream().reduce((a, b) -> a + "/" + b).orElseGet(this::menu);
    }

    String routeSignature() {
        return String.join("", routeSignature);
    }

    public List<String> params() {
        ArrayList<String> ps = new ArrayList<>(route);
        ps.remove(0);
        return ps;
    }

    String menu() {
        return route.get(0);
    }

    String paramF(int idx) throws AppLogicException {
        if (idx >= route.size())
            throw new AppLogicException("Invalid parameter index "+idx);
        return route.get(idx);
    }

    int paramIF(int idx) throws AppLogicException {
        try {
            return Integer.parseInt(paramF(idx));
        } catch (NumberFormatException e) {
            throw new AppLogicException("Invalid integer parameter: " + e);
        }
    }

    double paramDF(int idx) throws AppLogicException {
        try {
            return Double.parseDouble(paramF(idx));
        } catch (NumberFormatException e) {
            throw new AppLogicException("Invalid double parameter: " + e);
        }
    }

    public String param(int idx) {
        try {
            return paramF(idx);
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
    }

    public int paramI(int idx) {
        try {
            return paramIF(idx);
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
    }

    public double paramD(int idx) {
        try {
            return paramDF(idx);
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
    }
}
