package org.utu.studentcare.textmodeui;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.db.orm.Student;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Liittyy tekstikäyttöliittymään.
 *
 * Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi!
 * Voi silti olla tarpeen tekstiversion toiminnan käsittämiseksi
 */
public class AppLogic {
    private final Map<String, Menu> menuMap = new HashMap<>();
    private final String activeMenuName;

    public AppLogic(boolean debugMode, String activeMenuName, Menu... menus) throws SQLException, AppLogicException {
        this.activeMenuName = activeMenuName;
        for (Menu menu : menus) {
            if (menuMap.containsKey(menu.name))
                    throw new Error("Duplicate menu!");

            menuMap.put(menu.name, menu);
        }

        if (debugMode) {
            validateMenus();
            try {
                Files.write(Paths.get("menugraph.dot"), menuGraph().getBytes(), StandardOpenOption.CREATE);
                Runtime.getRuntime().exec("dot -Tpng menugraph.dot -o menugraph.png");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private Component fakeRendering(Menu m) throws SQLException, AppLogicException {
        MenuSession fakeSession = new MenuSession(
                new Student("dummy", "dummy", "dummy", 1, "dummy", "dummy", "dummy", true, true),
                SQLConnection.createConnection("dummy", true),
                new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                    }
                }),
                () -> "dummy") {

            String paramF(int idx) { return "dummy"; }
            int paramIF(int idx) { return 0; }
            double paramDF(int idx) { return 0; }
        };
        ArrayList<String> api = new ArrayList<>();
        if (m.apiSignature == null)
            api.add("main");
        else
            for(char c: m.apiSignature.toCharArray())
                if (c == 'S')
                    api.add("dummy");
                else api.add("0");
                fakeSession.navigate(api);

        Component root = new DOM().new Text("dummy", null);

        try {
            root = m.content.apply(fakeSession);
        }
        catch(AppLogicException | SQLException e) {
            System.err.println("Fake rendering failed with "+e);
        }

        return new DOM(true).evaluate(root, fakeSession);
    }

    private void validateMenus() throws SQLException, AppLogicException {
        for (Menu m : menuMap.values()) {
            List<SimpleLink> targets = new EvaluatedDOM().findLinks(fakeRendering(m), linkIndexer());
            for (SimpleLink l : targets)
                if (!menuMap.containsKey(l.parts.get(0)))
                    throw new Error("Menu " + l.parts.get(0) + " missing!");
        }
    }

    private String menuGraph() throws SQLException, AppLogicException {
        List<String> graph = new ArrayList<>();
        graph.add("digraph g {");
        for (Menu m : menuMap.values()) {
            List<SimpleLink> targets = new EvaluatedDOM().findLinks(fakeRendering(m), linkIndexer());
            String from = m.name.replace('"', '$');
            int i = 1;
            graph.add(from + " [shape=polygon,fillcolor=green,style=filled];");
            for (SimpleLink link : targets) {
                String to = (link.parts != null && link.parts.size() > 0 ? link.parts.get(0) : "unknown").replace('"', '$');
                String via = from + to + (i++);
                graph.add(from + " -> " + via+ ";");
                graph.add(via + " -> " + to + ";");
                graph.add(via + " [shape=egg,fillcolor=cyan,style=filled];");
                String viaLabel = (link.content != null ? link.content : link.name == null ? "?" : link.name).replace('"', '\'');
                viaLabel = viaLabel.replaceAll("dummy", "X").replaceAll("2019-01-02", "Y");
                if (viaLabel.length()>15) {
                    int splitIndex1 = viaLabel.length()/3;
                    int splitIndex2 = viaLabel.substring(splitIndex1).indexOf(' ');
                    if (splitIndex2 != -1)
                        viaLabel = viaLabel.substring(0, splitIndex1+splitIndex2)+"\\n"+viaLabel.substring(splitIndex1+splitIndex2);
                }
                graph.add(via + "[label=\"" + viaLabel + "\"];");
            }

        }
        graph.add("}");
        return String.join("\n", graph);
    }

    private Supplier<Integer> linkIndexer() {
        return new Supplier<>() {
            int i = 1;

            @Override
            public Integer get() {
                return i++;
            }
        };
    }

    public boolean control(Supplier<String> commandQueue, MenuSession session) throws SQLException, AppLogicException {
        // session inactive if no valid user supplied
        if (!session.active())
            return false;

        session.navigate(List.of(activeMenuName));

        while (true) {
            System.out.println("Entering menu " + session.route());
            System.out.println();
            String menuKey = session.menu();
            if (!menuMap.containsKey(menuKey))
                throw new AppLogicException("Menu not found!");

            Menu activeMenu = menuMap.get(menuKey);

            if (activeMenu.apiSignature != null)
                assert (activeMenu.apiSignature.equals(session.routeSignature()));
            else
                System.err.println("Unsafe menu API [" + activeMenuName + "]!");

            Component renderOutput = new DOM().evaluate(activeMenu.content.apply(session), session);
            List<String> prologue = new EvaluatedDOM().print(renderOutput, linkIndexer());
            List<SimpleLink> targets = new EvaluatedDOM().findLinks(renderOutput, linkIndexer());

            if (!session.active()) return true;

            System.out.println("\033c");

            for (String l : prologue)
                System.out.println(l);

            String command = commandQueue.get();

            if (command.equals("logout")) return true;
            if (command.equals("quit")) return false;

            for (SimpleLink l : targets)
                if (l.name.equals(command)) {
                    session.navigate(l.parts);
                    break;
                }
        }
    }
}