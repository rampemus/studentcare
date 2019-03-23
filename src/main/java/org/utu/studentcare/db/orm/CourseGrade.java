package org.utu.studentcare.db.orm;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * A single course grade entity with SQL ORM (object-relational mapping methods).
 * Only store in the DB if graded.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class CourseGrade {
    public final int studentId;
    public final String instanceId;
    public final int grade;
    public final String gradeDate;
    public final int teacherId;
    public final int adminId;
    public final String adminDate;

    public CourseGrade(int studentId, String instanceId, int grade, String gradeDate, int teacherId, int adminId, String adminDate) {
        this.studentId = studentId;
        this.instanceId = instanceId;
        this.grade = grade;
        this.gradeDate = gradeDate;
        this.teacherId = teacherId;
        this.adminId = adminId;
        this.adminDate = adminDate;
    }

    public CourseGrade(int studentId, String instanceId, int grade, String gradeDate, int teacherId) {
        this(studentId, instanceId, grade, gradeDate, teacherId, 0, "");
    }

    @Override
    public String toString() {
        return "CourseGrade{" +
                "studentId=" + studentId +
                ", instanceId=" + instanceId +
                ", grade=" + grade +
                ", gradeDate='" + gradeDate + '\'' +
                ", teacherId=" + teacherId +
                ", adminId=" + adminId +
                ", adminDate='" + adminDate + '\'' +
                '}';
    }

    public String shortOverview() {
        return grade + " (" + gradeDate + ")";
    }

    public boolean graded() {
        return !gradeDate.equals("");
    }

    public String overview(SQLConnection connection) throws SQLException, AppLogicException {
        CourseInstance c = course(connection);
        return c.wholeNameId(20) + ", " + ": arvosana " + grade + " @ " + gradeDate + (adminDate.equals("") ? ", ei opintorekisterissä" : ", kirjattu rekisteriin" + adminDate);
    }

    public String adminOverview(SQLConnection connection) throws SQLException, AppLogicException {
        CourseInstance c = course(connection);
        Student s = student(connection);

        return (adminDate.equals("") ? "" : adminDate + ": ") + c.wholeNameId(30) + " - " + grade + " - " + s.wholeNameId() + " - " + gradeDate;
    }

    public CourseInstance course(SQLConnection connection) throws SQLException, AppLogicException {
        return CourseInstance.findI(connection, instanceId).orElseThrow(() -> new AppLogicException("Course id not found: " + instanceId));
    }

    public Student student(SQLConnection connection) throws SQLException, AppLogicException {
        return Student.find(connection, studentId).orElseThrow(() -> new AppLogicException("Personnel id not found:" + studentId));
    }

    public Teacher teacher(SQLConnection connection) throws SQLException, AppLogicException {
        return Student.find(connection, teacherId).map(Student::asTeacher).orElseThrow(() -> new AppLogicException("Personnel id not found:" + teacherId));
    }

    public Secretary secretary(SQLConnection connection) throws SQLException, AppLogicException {
        return Student.find(connection, adminId).map(Student::asSecretary).orElseThrow(() -> new AppLogicException("Personnel id not found:" + adminId));
    }

    public static CourseGrade fromDB(ResultSet resultSet) throws SQLException {
        return new CourseGrade(
                resultSet.getInt("studentId"),
                resultSet.getString("instanceId"),
                resultSet.getInt("grade"),
                resultSet.getString("gradeDate"),
                resultSet.getInt("teacherId"),
                resultSet.getInt("adminId"),
                resultSet.getString("adminDate")
        );
    }

    public boolean approve(SQLConnection connection, int adminId) throws SQLException, AppLogicException {
        CourseGrade approved = new CourseGrade(studentId, instanceId, grade, gradeDate, teacherId, adminId, connection.now());
        return approved.updateDB(connection);
    }

    public boolean updateDB(SQLConnection connection) throws SQLException, AppLogicException {
        if (!graded())
            throw new SQLException("Not graded!");

        int i = find(connection, studentId, instanceId, gradeDate).isPresent() ?
                connection.update("update 'coursegrades' set grade = ?, teacherId = ?, adminId = ?, adminDate = ? where studentId == ? and instanceId == ? and gradeDate = ?", grade, teacherId, adminId, adminDate, studentId, instanceId, gradeDate)
                :
                connection.insert("insert into 'coursegrades'(grade,teacherId,adminId,adminDate,studentId,instanceId,gradeDate) values (?,?,?,?,?,?,?)", grade, teacherId, adminId, adminDate, studentId, instanceId, gradeDate);

        return i == 1;
    }

    public static Optional<CourseGrade> find(SQLConnection connection, int studentId, String instanceId, String gradeDate) throws SQLException, AppLogicException {
        return connection.findFirst(CourseGrade::fromDB, "select * from 'coursegrades' where studentId == ? and instanceId == ? and gradeDate == ?", studentId, instanceId, gradeDate);
    }

    public static Optional<CourseGrade> find(SQLConnection connection, int studentId, String instanceId) throws SQLException, AppLogicException {
        return connection.findFirst(CourseGrade::fromDB, "select * from 'coursegrades' where studentId == ? and instanceId == ? order by gradeDate desc", studentId, instanceId);
    }

    public static List<CourseGrade> findAll(SQLConnection connection, int studentId, String instanceId) throws SQLException, AppLogicException {
        return connection.findAll(CourseGrade::fromDB, "select * from 'coursegrades' where studentId == ? and instanceId == ? order by gradeDate desc", studentId, instanceId);
    }

    public static List<CourseGrade> waitingApproval(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(CourseGrade::fromDB, "select * from 'coursegrades' where adminId == 0 order by gradeDate desc");
    }

    public static List<CourseGrade> lastApproved(SQLConnection connection, int count) throws SQLException, AppLogicException {
        return connection.findAll(CourseGrade::fromDB, "select * from 'coursegrades' where adminId != 0 order by adminDate desc limit ?", count);
    }
}