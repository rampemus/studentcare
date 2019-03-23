package org.utu.studentcare.db;

import org.utu.studentcare.applogic.AppLogicException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * SQL interface for all SQL backends.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public interface SQLConnection extends AutoCloseable {
    // suora yhteys SQL-backendille
    //Connection connection();

    /** Suorittaa vapaamuotoisen SQL-käskyn */
    boolean performCustomStatement(String statement) throws SQLException, AppLogicException;

    /** Asettaa tietokannan viivästettyyn commit-tilaan jos paljon dataa kirjattavana */
    void setDelayedCommit(boolean status) throws SQLException;

    /** Etsii ensimmäisen datan esiintymän tietokannassa annetuilla ehdoilla */
    <T> Optional<T> findFirst(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws SQLException, AppLogicException;

    /** Etsii kaikki datan esiintymät tietokannassa annetuilla ehdoilla */
    <T> List<T> findAll(SQLFunction<ResultSet, T> mapping, String statement, Object... data) throws SQLException, AppLogicException;

    /** Lisää dataa tietokantaan */
    int insert(String statement, Object... data) throws SQLException, AppLogicException;

    /** Päivittää datan tietokannassa */
    int update(String statement, Object... data) throws SQLException, AppLogicException;

    /** Laskee datan rivien määrän tietokannassa */
    int count(String statement, Object... data) throws SQLException, AppLogicException;

    /** Poistaa dataa tietokannasta */
    int delete(String statement, Object... data) throws SQLException, AppLogicException;

    /** Palauttaa nykyhetken päiväyksen SQL-moottorin mukaan */
    String now() throws SQLException, AppLogicException;

    @FunctionalInterface
    interface SQLFunction<T, R> {
        R apply(T t) throws SQLException, AppLogicException;
    }

    /**
     *
     * @throws SQLException
     */

    /**
     * Luo SQL-yhteyden.
     *
     * @param path tietostopolku (liitä alkuun "slow" jos haluat tahallista hidastusta kyselyihin)
     * @param debugMode tulostetaanko konsoliin kyselyiden teksti
     * @param delay hidastuksen määrä (ms)
     * @return Yhteysolio
     * @throws SQLException
     */
    static SQLConnection createConnection(String path, boolean debugMode, int delay) throws SQLException {
        if ("dummy".equals(path)) {
            return new DummyConnection();
        }
        if (path.startsWith("slow"))
            return new SQLiteConnection(path.substring(4), debugMode) {
                @Override
                protected PreparedStatement prepare(String type, String statement, Object... data) throws SQLException, AppLogicException {
                    try {
                        if (!type.equals("insert")) Thread.sleep(delay);
                    } catch (InterruptedException e) {
                    }
                    return super.prepare(type, statement, data);
                }
            };
        else
            return new SQLiteConnection(path, debugMode);
    }

    /**
     * Luo SQL-yhteyden.
     *
     * @param path tietostopolku (liitä alkuun "slow" jos haluat tahallista hidastusta kyselyihin 400ms)
     * @param debugMode tulostetaanko konsoliin kyselyiden teksti
     * @return Yhteysolio
     * @throws SQLException
     */
    static SQLConnection createConnection(String path, boolean debugMode) throws SQLException {
        return createConnection(path, debugMode, 400);
    }
}
