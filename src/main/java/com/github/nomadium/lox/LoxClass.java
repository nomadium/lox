package com.github.nomadium.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
    private final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(final String name, final Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(final LoxInstance instance, final String name) {
        if (methods.containsKey(name)) {
            return methods.get(name).bind(instance);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    String getName() {
        return name;
    }

    @Override
    public Object call(final Interpreter interpreter, final List<Object> arguments) {
        final LoxInstance instance = new LoxInstance(this);
        final LoxFunction initializer = methods.get("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        final LoxFunction initializer = methods.get("init");
        if (initializer == null) { return 0; }
        return initializer.arity();
    }
}
