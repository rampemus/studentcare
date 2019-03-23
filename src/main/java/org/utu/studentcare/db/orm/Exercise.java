package org.utu.studentcare.db.orm;

import org.utu.studentcare.applogic.ExerciseSpec;
import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A single exercise answer entity with SQL ORM (object-relational mapping methods).
 * Only store in the DB if graded and/or uploaded.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Exercise {
    public final int studentId;
    public final String instanceId;
    public final String exerciseId;
    public final String uploadResource;
    public final String uploadDate;
    public final int teacherId;
    public final String comment;
    public final String teacherComment;
    public final double grade;
    public final String gradeDate;
    public final Optional<ExerciseSpec> specification;


    public Exercise(int studentId, String instanceId, String exerciseId, String uploadResource, String uploadDate, int teacherId, String comment, String teacherComment, double grade, String gradeDate, Optional<ExerciseSpec> specification) {
        this.studentId = studentId;
        this.instanceId = instanceId;
        this.exerciseId = exerciseId;
        this.uploadResource = uploadResource;
        this.uploadDate = uploadDate;
        this.teacherId = teacherId;
        this.comment = comment;
        this.teacherComment = teacherComment;
        this.grade = grade;
        this.gradeDate = gradeDate;
        this.specification = specification;
    }

    public Exercise(int studentId, String instanceId, String exerciseId, ExerciseSpec specification) {
        this(studentId, instanceId, exerciseId, "", "", 0, "", "", 0.0, "", Optional.of(specification));
    }

    public Exercise withSpec(ExerciseSpec specification) {
        return new Exercise(studentId, instanceId, exerciseId, uploadResource, uploadDate, teacherId, comment, teacherComment, grade, gradeDate, Optional.of(specification));
    }

    public Exercise grade(SQLConnection connection, int teacherId, double grade, String teacherComment) throws SQLException, AppLogicException {
        Exercise e = new Exercise(studentId, instanceId, exerciseId, uploadResource, uploadDate, teacherId, comment, teacherComment, grade, connection.now(), specification);
        e.updateDB(connection);
        return e;
    }

    public Exercise upload(SQLConnection connection, String uploadResource, String comment) throws SQLException, AppLogicException {
        Exercise e = new Exercise(studentId, instanceId, exerciseId, uploadResource, connection.now(), teacherId, comment, teacherComment, grade, gradeDate, specification);
        e.updateDB(connection);
        return e;
    }

    public boolean updateDB(SQLConnection connection) throws SQLException, AppLogicException {
        if (!graded() && !uploaded())
            throw new SQLException("Not graded or uploaded!");
        int i =
                find(connection, studentId, instanceId, exerciseId).isPresent() ?
                        connection.update("update 'exercises' set uploadResource = ?, uploadDate = ?, teacherId = ?, comment = ?, teacherComment = ?, grade = ?, gradeDate = ? where studentId == ? and instanceId == ? and gradeDate = ?", uploadResource, uploadDate, teacherId, comment, teacherComment, grade, gradeDate, studentId, instanceId, exerciseId)
                        :
                        connection.insert("insert into 'exercises'(uploadResource,uploadDate,teacherId,comment,teacherComment,grade,gradeDate, studentId, instanceId, exerciseId) values (?,?,?,?,?,?,?,?,?,?)", uploadResource, uploadDate, teacherId, comment, teacherComment, grade, gradeDate, studentId, instanceId, exerciseId);

        return i == 1;

    }

    private String gradingStatus(SQLConnection connection) throws SQLException, AppLogicException {
        String tmp = "";
        if (!uploadDate.equals("")) {
            tmp += ", tehty " + uploadDate;
        }
        if (!gradeDate.equals("")) {
            tmp += ", arvioitu " + gradeDate + "(" + gradedBy(connection).map(t -> t.username).orElse("?") + ") -> " + grade;
        }
        return tmp;
    }

    public Optional<Teacher> gradedBy(SQLConnection connection) throws SQLException, AppLogicException {
        return Teacher.findT(connection, teacherId);
    }

    public String status(SQLConnection connection) throws SQLException, AppLogicException {
        return specification.map(ExerciseSpec::overview).orElse(exerciseId) + gradingStatus(connection);
    }

    public String verboseView(SQLConnection connection) throws SQLException, AppLogicException {
        return status(connection) + "\nVastaus: " + uploadResource + "\n\nKommentti: " + comment;
    }

    public boolean graded() {
        return !gradeDate.equals("");
    }

    public boolean uploaded() {
        return !uploadDate.equals("");
    }

    @Override
    public String toString() {
        return "Exercise{" +
                "studentId=" + studentId +
                ", instanceId='" + instanceId + '\'' +
                ", exerciseId=" + exerciseId +
                ", uploadResource='" + uploadResource + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", teacherId=" + teacherId +
                ", comment='" + comment + '\'' +
                ", teacherComment='" + teacherComment + '\'' +
                ", grade=" + grade +
                ", gradeDate='" + gradeDate + '\'' +
                '}';
    }

    public static Exercise fromDB(ResultSet resultSet) throws SQLException {
        return new Exercise(
                resultSet.getInt("studentId"),
                resultSet.getString("instanceId"),
                resultSet.getString("exerciseId"),
                resultSet.getString("uploadResource"),
                resultSet.getString("uploadDate"),
                resultSet.getInt("teacherId"),
                resultSet.getString("comment"),
                resultSet.getString("teacherComment"),
                resultSet.getFloat("grade"),
                resultSet.getString("gradeDate"),
                Optional.empty()
        );
    }

    public static Optional<Exercise> find(SQLConnection connection, int studentId, String instanceId, String exerciseId) throws SQLException, AppLogicException {
        return connection.findFirst(Exercise::fromDB, "select * from 'exercises' where studentId == ? and instanceId == ? and exerciseId == ?", studentId, instanceId, exerciseId);
    }
}