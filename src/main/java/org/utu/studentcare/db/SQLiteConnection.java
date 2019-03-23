package org.utu.studentcare.db;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-yhteysluokka SQLitelle.
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
class SQLiteConnection implements SQLConnection {
    private final Connection connection;
    private final boolean debugMode;

    public SQLiteConnection(String dbPath, boolean debugMode) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            throw new SQLException("SQLite JDBC classes missing!");
        }
        String dbFile = "jdbc:sqlite:" + dbPath;
        if (debugMode) System.out.println("Connecting to " + dbPath);
        this.connection = openConnection(dbFile);
        this.debugMode = debugMode;
        if (debugMode) System.out.println("Connected.");
    }

    @Override
    public void setDelayedCommit(boolean status) throws SQLException {
        if (status) {
            connection.setAutoCommit(false);
        } else {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }
/*
    public Connection connection() {
        return connection;
    }
*/
    private Connection openConnection(String db) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(false);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        config.setOpenMode(SQLiteOpenMode.EXCLUSIVE);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);

        return DriverManager.getConnection(db, config.toProperties());
    }

    public final int timeout = 10;

    @Override
    public boolean performCustomStatement(String statement) throws SQLException, AppLogicException {
        return prepare("", statement).execute();
    }

    private ResultSet find(String statement, Object... data) throws SQLException, AppLogicException {
        return prepare("select", statement, data).executeQuery();
    }

    public <T> Optional<T> findFirst(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws SQLException, AppLogicException {
        ResultSet rs1 = find(statement, data);
        return rs1.next() ? Optional.of(mapping.apply(rs1)) : Optional.empty();
    }

    public <T> List<T> findAll(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws SQLException, AppLogicException {
        List<T> instances = new ArrayList<>();
        ResultSet rs = find(statement, data);

        while (rs.next())
            instances.add(mapping.apply(rs));

        return instances;
    }

    public int insert(String statement, Object... data) throws SQLException, AppLogicException {
        return prepare("insert", statement, data).executeUpdate();
    }

    public int update(String statement, Object... data) throws SQLException, AppLogicException {
        return prepare("update", statement, data).executeUpdate();
    }

    public int count(String statement, Object... data) throws SQLException, AppLogicException {
        ResultSet rs = prepare("select", statement, data).executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int delete(String statement, Object... data) throws SQLException, AppLogicException {
        return prepare("delete", statement, data).executeUpdate();
    }

    protected PreparedStatement prepare(String type, String statement, Object... data) throws SQLException, AppLogicException {
        if (!statement.toLowerCase().startsWith(type)) throw new SQLException("Incompatible statement type! Expected "+type+", got "+statement);
        PreparedStatement ps = connection.prepareStatement(statement);

        if (debugMode) {
            StringBuilder debug = new StringBuilder(statement);
            if (data.length > 0) debug.append(" <- ");
            for (Object o : data)
                debug.append(o).append("| ");
            System.out.println(debug);
        }

        for (int i = 0; i < data.length; i++) {
            if (data[i] instanceof String)
                ps.setString(i + 1, (String) data[i]);
            else if (data[i] instanceof Integer)
                ps.setInt(i + 1, (Integer) data[i]);
            else if (data[i] instanceof Boolean)
                ps.setBoolean(i + 1, (Boolean) data[i]);
            else if (data[i] instanceof Double)
                ps.setDouble(i + 1, (Double) data[i]);
        }
        ps.setQueryTimeout(timeout);
        ps.closeOnCompletion();
        return ps;
    }

    public String now() throws SQLException, AppLogicException {
        ResultSet rs = prepare("select", "select datetime('now') as date").executeQuery();
        if (rs.next()) return rs.getString(1);
        throw new SQLException("Could not compute datetime!");
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        if (debugMode) System.out.println("Closed connection.");
    }
}