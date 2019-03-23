package org.utu.studentcare.applogic;

import java.util.HashMap;
import java.util.Set;

/**
 * Evaluates expressions in course description.
 */
// TODO: Oheisluokka, ei tarvitse muokata, mutta pitää ymmärtää ja käyttää omassa koodissa kuten esimerkissäkin!
public class ValueContext {
    // muuttujien arvot kontekstissa
    private final HashMap<String, ValRange> symbols = new HashMap<>();

    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        for (String key : symbols.keySet())
            tmp.append(key).append(" -> ").append(symbols.get(key)).append("\n");

        return tmp.toString();
    }

    public void put(String key, double value) {
        symbols.put(key, new ValRange(value, value));
    }

    public void put(String key, ValRange value) {
        symbols.put(key, value);
    }

    public ValRange get(String key) throws AppLogicException {
        if (!symbols.containsKey(key))
            throw new AppLogicException("No key " + key + " in context. Only " + String.join("", symbols.keySet()));
        return symbols.get(key);
    }

    public Set<String> keys() {
        return symbols.keySet();
    }

    public ValueContext evaluate(ExerciseSpecs exerciseSpecs) {
        ValueContext newContext = new ValueContext();
        newContext.symbols.putAll(symbols);

        for (ExerciseSpec e : exerciseSpecs.getExerciseDecls()) {
            if (!newContext.keys().contains(e.getId())) {
                ValRange v = e.getRange();
                newContext.put(e.getId(), v);
            }
        }
        for (GradingSpec g : exerciseSpecs.getGradingDecls()) {
            if (!newContext.keys().contains(g.getId())) {
                newContext.put(g.getId(), g.value(newContext));
            }
        }
        return newContext;
    }
}