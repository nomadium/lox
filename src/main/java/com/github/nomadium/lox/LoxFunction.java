package com.github.nomadium.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    LoxFunction(final Stmt.Function declaration, final Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public Object call(final Interpreter interpreter, final List<Object> arguments) {
        final Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.parameters.size(); i++) {
            environment.define(declaration.parameters.get(i).getLexeme(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (final Return returnValue) {
            return returnValue.getValue();
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.getLexeme() + ">";
    }
}
