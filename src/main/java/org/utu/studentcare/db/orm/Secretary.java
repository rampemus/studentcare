package org.utu.studentcare.db.orm;

/**
 * Not really needed, a Student with a special constructor (isAdmin = true).
 *
 * TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
 */
public class Secretary extends Student {
    public Secretary(Student s) {
        super(s.firstNames, s.familyName, s.program, s.id, s.idString, s.password, s.username, s.isTeacher, true);
    }

    public Secretary(String firstNames, String familyName, String program, int id, String idString, String password, String username) {
        super(firstNames, familyName, program, id, idString, password, username, false, true);
    }
}