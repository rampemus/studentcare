package org.utu.studentcare.db.orm;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;
import org.utu.studentcare.applogic.ExerciseSpecs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * A single course instance entity for a single year's course with SQL ORM (object-relational mapping methods).
 * Extends Course so can also be used for all purposes you'd need a Course for.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class CourseInstance extends Course {
    public final String instanceId;
    public final String gradingRule;

    public CourseInstance(String name, String shortName, String description, int credits, String instanceId, String gradingRule) {
        super(name, shortName, description, credits);
        this.instanceId = instanceId;
        this.gradingRule = gradingRule;
    }

    public CourseInstance(Course c, String instanceId, String gradingRule) {
        this(c.name, c.shortName, c.description, c.credits, instanceId, gradingRule);
    }

    @Override
    public String toString() {
        return "CourseInstance{" +
                "instanceId='" + instanceId + '\'' +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", description='" + description + '\'' +
                ", credits=" + credits +
                ", gradingRule=" + gradingRule +
                '}';
    }

    public ExerciseSpecs exerciseSpecs() throws AppLogicException {
        return ExerciseSpecs.generateFrom(gradingRule);
    }

    public String wholeNameId(int maxlen) {
        return (name.length() > maxlen ? (name + "..").substring(0, maxlen) : name) + " (" + instanceId + ", "+credits+"op)";
    }

    public CourseInstance setGrading(String gradingRule) {
        return new CourseInstance(name, shortName, description, credits, instanceId, gradingRule);
    }

    public boolean updateDB(SQLConnection connection) throws SQLException, AppLogicException {
        int i;

        if (findI(connection, instanceId).isPresent()) {
            i = connection.update("update 'courseinstances' set gradingRule = ? where instanceId == ? and courseId == ?", gradingRule, instanceId, shortName);
        } else {
            i = connection.insert("insert into 'courseinstances'(gradingRule,instanceId,courseId) values (?,?,?)", gradingRule, instanceId, shortName);
        }
        return i == 1 && super.updateDB(connection);
    }

    public static Optional<CourseInstance> findI(SQLConnection connection, String course, String year) throws SQLException, AppLogicException {
        return connection.findFirst(rs -> fromDB(connection, rs), "select * from 'courseinstances' where courseId == ? and instanceId like ?", course, "%" + year);
    }

    public static Optional<CourseInstance> findI(SQLConnection connection, String instanceId) throws SQLException, AppLogicException {
        return connection.findFirst(rs -> fromDB(connection, rs), "select * from 'courseinstances' where instanceId == ?", instanceId);
    }

    public static CourseInstance fromDB(SQLConnection connection, ResultSet resultSet) throws SQLException, AppLogicException {
        String instanceId = resultSet.getString("instanceId");
        String courseId = resultSet.getString("courseId");
        String gradingRule = resultSet.getString("gradingRule");

        Course c = Course.find(connection, courseId).orElseThrow();
        return new CourseInstance(c, instanceId, gradingRule);
    }

    public List<Student> students(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(Student::fromDB, "select * from 'personnel' where id in (select studentId from 'coursestudents' where instanceId == ?)", instanceId);
    }

    public CourseGrade grade(SQLConnection connection, Student s, int grade, Teacher t) throws SQLException, AppLogicException {
        CourseGrade record = new CourseGrade(s.id, instanceId, grade, connection.now(), t.id);
        record.updateDB(connection);
        return record;
    }
}