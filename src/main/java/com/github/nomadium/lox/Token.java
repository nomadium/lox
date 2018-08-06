package com.github.nomadium.lox;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
class Token {
    TokenType type;
    String lexeme;
    Object literal;
    int line;

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
