package org.utu.studentcare.applogic;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.db.orm.Student;

import java.io.PrintStream;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Session object.
 * Maintains a session state, e.g. authenticated user, sql connection, i/o streams.
 * TODO: Oheisluokka, voi olla tarpeellinen tai sitten ei.
 */
public class Session {
    public final Student user;
    public final SQLConnection connection;
    public final Supplier<String> inputStream;
    public final PrintStream outputStream;
    private boolean active;

    public Session(Student user, SQLConnection connection, PrintStream outputStream, Supplier<String> inputStream) {
        this.user = user;
        this.connection = connection;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        active = user != null;
    }

    public Session(Optional<Student> user, SQLConnection connection, PrintStream outputStream, Supplier<String> inputStream) {
        this(user.orElse(null), connection, outputStream, inputStream);
    }

    /** Session active. */
    public boolean active() {
        return active;
    }

    /** Close the session. */
    public void close() {
        active = false;
    }
}