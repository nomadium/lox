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

    Object getAt(final int distance, final String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(final int distance, final Token name, final Object value) {
        ancestor(distance).values.put(name.getLexeme(), value);
    }

    Environment ancestor(final int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
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
