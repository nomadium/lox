package com.github.nomadium.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(final Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(final String name, final Object value) {
        values.put(name, value);
    }

    Object get(final Token name) {
        final String lexeme = name.getLexeme();

        if (values.containsKey(lexeme)) {
            return values.get(lexeme);
        }

        if (enclosing != null) { return enclosing.get(name); }

        final String undefinedVariable = "Undefined variable '" + lexeme + "'.";
        throw new RuntimeError(name, undefinedVariable);
    }

    void assign(final Token name, final Object value) {
        final String lexeme = name.getLexeme();

        if (values.containsKey(lexeme)) {
            values.put(lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        final String undefinedVariable = "Undefined variable '" + lexeme + "'.";
        throw new RuntimeError(name, undefinedVariable);
    }
}
