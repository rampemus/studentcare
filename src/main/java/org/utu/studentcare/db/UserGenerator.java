package org.utu.studentcare.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generates persons + utu usernames.
 */
abstract class UserGenerator {
    abstract String generatemName();

    abstract String generatefName();

    abstract String generateLastName();

    public String generateName(List<String> nameList) {
        return nameList.get(new Random().nextInt(nameList.size()));
    }

    public boolean isDoubleName(String name) {
        return namePartCount(name) == 2;
    }

    public int namePartCount(String name) {
        return name.equals("") ? 0 : name.replaceAll(" +", " ").replaceAll("-", " ").split(" ").length;
    }

    public List<String> generateFirstNames(int minNumber, int maxNumber) {
        List<String> names = new ArrayList<>();
        int nameCount = 0;
        Supplier<String> nameGenerator = this::generatefName;

        if (new Random().nextBoolean()) nameGenerator = this::generatemName;

        int targetLength = new Random().nextBoolean() ? maxNumber : new Random().nextInt(maxNumber + minNumber - 1) + minNumber;

        while (nameCount < targetLength) {
            String nextName = nameGenerator.get();
            int nextCount = namePartCount(nextName);
            if (nameCount + nextCount <= targetLength) {
                names.add(nextName);
                nameCount += nextCount;
            }
        }

        return names;
    }

    public String generateUserName(String fNames, String sName, Predicate<String> filter) {
        Function<String, String> processName = name -> name.replaceAll("-", " ").replaceAll("[^\\x20-\\x7F]", "").toLowerCase();

        String[] fNamesASCII = processName.apply(fNames).split(" ");
        String sNameASCII = processName.apply(sName);

        List<String> prefixes = new ArrayList<>();
        prefixes.add("");

        for (String s : fNamesASCII) {
            List<String> newNames = new ArrayList<>();
            for (String prefix : prefixes) {
                for (int l : new int[]{2, 3, 1, 4, 0})
                    newNames.add(prefix + s.substring(0, Math.min(l, s.length())));
            }
            prefixes = newNames;
        }
        for (String prefix : prefixes) {
            String name = prefix + sNameASCII;
            if (name.length() < 6) {
                if (!filter.test(name))
                    return name;
            } else {
                String candidate = name.substring(0, 6);
                if (!filter.test(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public String generateFirstNames() {
        return generateFirstNames(1, 3).stream().collect(Collectors.joining(" "));
    }
}