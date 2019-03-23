package org.utu.studentcare.db.orm;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * A single teacher entity with SQL ORM (object-relational mapping methods).
 * Teacher is basically a student with additional operations.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Teacher extends Student {
    public Teacher(Student s) {
        super(s.firstNames, s.familyName, s.program, s.id, s.idString, s.password, s.username, true, s.isAdmin);
    }

    public Teacher(String firstNames, String familyName, String program, int id, String idString, String password, String username) {
        super(firstNames, familyName, program, id, idString, password, username, true, false);
    }

    public boolean teachCourse(SQLConnection connection, CourseInstance courseInstance) throws SQLException, AppLogicException {
        return connection.insert("insert into 'courseteachers'(teacherId, instanceId) values (?,?)", id, courseInstance.instanceId) == 1;
    }

    public boolean abandonCourse(SQLConnection connection, CourseInstance courseInstance) throws SQLException, AppLogicException {
        return connection.delete("delete from 'courseteachers' where teacherId == ? and instanceId == ?", id, courseInstance.instanceId) == 1;
    }

    public List<CourseInstance> teaching(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(
                r -> CourseInstance.fromDB(connection, r),
                "select * from 'courseinstances' where instanceId in (select instanceId from 'courseteachers' where teacherId == ?)",
                id);
    }

    public List<CourseInstance> notTeaching(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(
                r -> CourseInstance.fromDB(connection, r),
                "select * from 'courseinstances' where instanceId not in (select instanceId from 'courseteachers' where teacherId == ?) and instanceId like '%_'||strftime('%Y', 'now')",
                id);
    }

    public static Teacher fromDB(ResultSet resultSet) throws SQLException {
        return new Teacher(Student.fromDB(resultSet));
    }

    public static Optional<Teacher> findT(SQLConnection connection, String username) throws SQLException, AppLogicException {
        return Student.find(connection, username).map(Teacher::new);
    }

    public static Optional<Teacher> findT(SQLConnection connection, int id) throws SQLException, AppLogicException {
        return Student.find(connection, id).map(Teacher::new);
    }

    public static Optional<Teacher> authenticateT(SQLConnection connection, String username, String password) throws SQLException, AppLogicException {
        return Student.authenticate(connection, username, password).map(Teacher::new);
    }
}