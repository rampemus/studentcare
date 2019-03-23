package org.utu.studentcare.db.orm;

import org.utu.studentcare.db.SQLConnection;
import org.utu.studentcare.applogic.AppLogicException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * A single course entity with SQL ORM (object-relational mapping methods).
 * Also see CourseInstance if you need a specific instance for a single year's course.
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Course {
    public final String name;
    public final String shortName;
    public final String description;
    public final int credits;

    public Course(String name, String shortName, String description, int credits) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.credits = credits;
    }

    @Override
    public String toString() {
        return "Course{" +
                "name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", description='" + description + '\'' +
                ", credits=" + credits +
                '}';
    }

    public boolean updateDB(SQLConnection connection) throws SQLException, AppLogicException {
        int i;
        if (find(connection, shortName).isPresent())
            i = connection.update("update 'courses' set name = ?, description = ?, credits = ? where shortName == ?", name, description, credits, shortName);
        else
            i = connection.insert("insert into 'courses'(name,description,credits,shortName) values (?,?,?,?)", name, description, credits, shortName);

        return i == 1;
    }

    public static Course fromDB(ResultSet resultSet) throws SQLException {
        return new Course(
                resultSet.getString("name"),
                resultSet.getString("shortName"),
                resultSet.getString("description"),
                resultSet.getInt("credits")
        );
    }

    public static List<Course> all(SQLConnection connection) throws SQLException, AppLogicException {
        return connection.findAll(Course::fromDB, "select * from 'courses'");
    }

    public static Optional<Course> find(SQLConnection connection, String courseId) throws SQLException, AppLogicException {
        return connection.findFirst(Course::fromDB, "select * from 'courses' where shortName == ?", courseId);
    }
}
