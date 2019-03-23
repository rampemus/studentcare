package org.utu.studentcare.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // 50% -> max
        // 50% -> min .. max
        int targetLength = new Random().nextBoolean() ? maxNumber : new Random().nextInt(maxNumber - minNumber + 1) + minNumber;

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

    public String generateUserName(String firstNames, String lastName, Predicate<String> filter) {
        Function<String, String> processName = name -> name.replaceAll("-", " ").replaceAll("[^\\x20-\\x7F]", "").toLowerCase();

        String[] firstNamesASCII = processName.apply(firstNames).split(" ");
        String lastNameASCII = processName.apply(lastName);

        List<String> prefixes = new ArrayList<>();
        prefixes.add("");

        for (String s : firstNamesASCII) {
            List<String> newNames = new ArrayList<>();
            for (String prefix : prefixes) {
                for (int l : new int[]{2, 3, 1, 4, 0})
                    newNames.add(prefix + s.substring(0, Math.min(l, s.length())));
            }
            prefixes = newNames;
        }
        List<String> userNames = prefixes.stream().map(p -> {
            String result = p + lastNameASCII;
            return result.substring(0, Math.min(result.length(), 6));
        }).filter(filter.negate()).collect(Collectors.toList());

        return Stream.concat(
                userNames.stream().filter(n -> n.length()==6),
                userNames.stream().filter(n -> n.length()<6).sorted((m, n) -> n.length() - m.length())
        ).findFirst().orElse(null);
    }

    public String generateFirstNames() {
        return generateFirstNames(1, 3).stream().collect(Collectors.joining(" "));
    }
}