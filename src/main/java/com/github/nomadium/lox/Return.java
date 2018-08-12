package com.github.nomadium.lox;

class Return extends RuntimeException {
    private final Object value;

    Return(final Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    Object getValue() {
        return this.value;
    }
}
