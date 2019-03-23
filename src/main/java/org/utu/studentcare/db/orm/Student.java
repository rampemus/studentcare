package org.utu.studentcare.db.orm;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * A single student entity with SQL ORM (object-relational mapping methods).
 * All persons are students in this db schema.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Student {
    public final String firstNames;
    public final String familyName;
    public final String program;
    public final int id;
    public final String idString;
    public final String password;
    public final String username;
    public final boolean isTeacher;
    public final boolean isAdmin;
    public final boolean isStudent;

    public Student(Student s) {
        this.firstNames = s.firstNames;
        this.familyName = s.familyName;
        this.program = s.program;
        this.id = s.id;
        this.idString = s.idString;
        this.password = s.password;
        this.username = s.username;
        this.isTeacher = s.isTeacher;
        this.isAdmin = s.isAdmin;
        this.isStudent = true;
    }

    public Student(String firstNames, String familyName, String program, int id, String idString, String password, String username, boolean isTeacher, boolean isAdmin) {
        this.firstNames = firstNames;
        this.familyName = familyName;
        this.program = program;
        this.id = id;
        this.idString = idString;
        this.password = password;
        this.username = username;
        this.isTeacher = isTeacher;
        this.isAdmin = isAdmin;
        this.isStudent = true;
    }

    @Override
    public String toString() {
        return "Student{" +
                "firstNames='" + firstNames + '\'' +
                ", familyName='" + familyName + '\'' +
                ", program='" + program + '\'' +
                ", id=" + id +
                ", idString='" + idString + '\'' +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", isTeacher=" + isTeacher +
                ", isAdmin=" + isAdmin +
                ", isStudent=" + isStudent +
                '}';
    }

    public String wholeName() {
        return firstNames + " " + familyName;
    }

    public String wholeNameId() {
        return wholeName() + " (" + idString + " / " + username + ")";
    }

    public List<CourseInstance> attending(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(
                r -> CourseInstance.fromDB(connection, r),
                "select * from 'courseinstances' where instanceId in (select instanceId from 'coursestudents' where studentId == ?)",
                id);
    }

    public List<CourseInstance> notAttending(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(
                r -> CourseInstance.fromDB(connection, r),
                "select * from 'courseinstances' where not instanceId in (select instanceId from 'coursestudents' where studentId == ?) and instanceId like '%_'||strftime('%Y', 'now')",
                id);
    }

    public Exercises exercises(SQLConnection connection, String instanceId) throws SQLException, AppLogicException {
        return new Exercises(id, instanceId,
                connection.findAll(Exercise::fromDB,
                        "select * from 'exercises' where studentId == ? and instanceId == ?", id, instanceId),
                CourseInstance.findI(connection, instanceId).orElseThrow().exerciseSpecs()
        );
    }

    public boolean joinCourse(SQLConnection connection, CourseInstance courseInstance) throws SQLException, AppLogicException {
        return connection.insert("insert into 'coursestudents'(studentid, instanceId) values (?,?)", id, courseInstance.instanceId) == 1;
    }

    public boolean partCourse(SQLConnection connection, CourseInstance courseInstance) throws SQLException, AppLogicException {
        return connection.delete("delete from 'coursestudents' where studentid == ? and instanceId == ?", id, courseInstance.instanceId) == 1;
    }

    public boolean updateDB(SQLConnection connection) throws SQLException, AppLogicException {
        int i;

        if (find(connection, id).isPresent()) {
            i = connection.update(
                    "update 'personnel' set firstNames = ?, familyName = ?, program = ?, idString = ?, password = ?, username = ?, isTeacher = ?, isAdmin = ?, isStudent = ? where id == ?",
                    firstNames, familyName, program, idString, password, username, isTeacher, isAdmin, isStudent, id);

        } else {
            i = connection.insert(
                    "insert into 'personnel'(firstNames,familyName,program,idString,password,username,isTeacher,isAdmin,isStudent,id) values (?,?,?,?,?,?,?,?,?,?)",
                    firstNames, familyName, program, idString, password, username, isTeacher, isAdmin, isStudent, id);
        }

        return i == 1;
    }

    public static Student fromDB(ResultSet resultSet) throws SQLException {
        return new Student(
                resultSet.getString("firstNames"),
                resultSet.getString("familyName"),
                resultSet.getString("program"),
                resultSet.getInt("id"),
                resultSet.getString("idString"),
                resultSet.getString("password"),
                resultSet.getString("username"),
                resultSet.getBoolean("isTeacher"),
                resultSet.getBoolean("isAdmin")
        );
    }

    public static Optional<Student> find(SQLConnection connection, int id) throws SQLException, AppLogicException {
        return connection.findFirst(Student::fromDB, "select * from 'personnel' where id == ?", id);
    }

    public static Optional<Student> find(SQLConnection connection, String username) throws SQLException, AppLogicException {
        return connection.findFirst(Student::fromDB, "select * from 'personnel' where username == ?", username);
    }

    public static Optional<Student> authenticate(SQLConnection connection, String username, String password) throws SQLException, AppLogicException {
        return find(connection, username).filter(s -> s.password.equals(password));
    }

    public Teacher asTeacher() {
        return new Teacher(this);
    }

    public Secretary asSecretary() {
        return new Secretary(this);
    }
}