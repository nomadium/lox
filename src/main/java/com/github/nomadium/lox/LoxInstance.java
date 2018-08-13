package com.github.nomadium.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(final LoxClass klass) {
        this.klass = klass;
    }

    Object get(final Token name) {
        final String lexeme = name.getLexeme();
        if (fields.containsKey(lexeme)) {
            return fields.get(lexeme);
        }

        final LoxFunction method = klass.findMethod(this, name.getLexeme());
        if (method != null) { return method; }

        throw new RuntimeError(name, "Undefined property '" + lexeme + "'.");
    }

    void set(final Token name, final Object value) {
        fields.put(name.getLexeme(), value);
    }

    @Override
    public String toString() {
        return klass.getName() + " instance";
    }
}
