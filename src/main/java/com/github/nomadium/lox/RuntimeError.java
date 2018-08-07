package com.github.nomadium.lox;

class RuntimeError extends RuntimeException {
    private final Token token;

    RuntimeError(final Token token, final String message) {
        super(message);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
