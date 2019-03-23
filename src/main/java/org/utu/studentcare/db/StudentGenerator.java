package org.utu.studentcare.db;

import org.utu.studentcare.db.orm.Student;

import java.util.*;

public class StudentGenerator extends UserGenerator {
    private final CSVNames nameGenerator = new CSVNames();

    public StudentGenerator() {
        for (String program : nameGenerator.programs) {
            int idx = 1;
            String shortName = program.substring(0, 3).toUpperCase();
            String key = shortName;
            while (progMappings.containsKey(key))
                key = shortName + (++idx);

            progMappings.put(key, program);
        }
    }

    // volatile state
    protected final Set<String> generatedUsernames = new HashSet<>();

    private final Map<String, String> progMappings = new HashMap<>();

    private int studentIdCursor = 1000;

    @Override
    String generatemName() {
        return generateName(nameGenerator.mNames);
    }

    @Override
    String generatefName() {
        return generateName(nameGenerator.fNames);
    }

    @Override
    String generateLastName() {
        return generateName(nameGenerator.lastNames);
    }

    public Set<String> programIDs() {
        return progMappings.keySet();
    }

    public String programName(String shortName) {
        return progMappings.getOrDefault(shortName, "???");
    }

    public String generateProgram() {
        List<String> programs = new ArrayList<>(programIDs());
        return programs.get(new Random().nextInt(programs.size()));
    }

    String generatePassword(String firstName, String lastName) {
        return ("" + firstName.charAt(0) + lastName.charAt(0) + "1234").toLowerCase();
    }

    public Student generateStudent() {
        String fNames, sName, userName;

        // generateUserName returns null if the username is already reserved
        do {
            fNames = generateFirstNames();
            sName = generateLastName();
            userName = generateUserName(fNames, sName, generatedUsernames::contains);
        } while (userName == null);

        generatedUsernames.add(userName);
        studentIdCursor += new Random().nextInt(10) + 1;

        return new Student(fNames, sName, generateProgram(), studentIdCursor,
                "utu:" + studentIdCursor, generatePassword(fNames, sName), userName,
                false, false
        );
    }
}
