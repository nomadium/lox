package com.github.nomadium.lox;

import static com.github.nomadium.lox.TokenType.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/*
 * Lox expressions grammar:
 *
 * expression     -> equality ;
 * equality       -> comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     -> addition ( ( ">"  | ">=" | "<" | "<=" ) addition )* ;
 * addition       -> multiplication ( ( "-"  | "+" ) multiplication )* ;
 * multiplication -> unary ( ( "/" | "*" ) unary )* ;
 * unary          -> ( "!" | "-" ) unary | primary ;
 * primary        -> NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" ;
 *
 */
class Parser {
    private static final String EXPECT_RIGHT_PAREN = "Expect ')' after expression.";
    private static final String EXPECT_EXPRESSION = "Expect expression.";

    private static final class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    Optional<Expr> parse() {
        try {
            return Optional.of(expression());
        } catch (ParseError error) {
            return Optional.empty();
        }
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            final Token operator = previous();
            final Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            final Token operator = previous();
            final Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();

        while (match(MINUS, PLUS)) {
            final Token operator = previous();
            final Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            final Token operator = previous();
            final Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            final Token operator = previous();
            final Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) { return new Expr.Literal(false); }
        if (match(TRUE))  { return new Expr.Literal(true);  }
        if (match(NIL))   { return new Expr.Literal(null);  }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().getLiteral());
        }

        if (match(LEFT_PAREN)) {
            final Expr expr = expression();
            consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN);
            return new Expr.Grouping(expr);
        }

        throw error(peek(), EXPECT_EXPRESSION);
    }

    private boolean match(final TokenType... types) {
        return Arrays.stream(types)
                     .filter(type -> check(type))
                     .findFirst()
                     .map(type -> {
                         advance();
                         return type;
                     })
                     .isPresent();
    }

    private Token consume(final TokenType type, final String message) {
        if (check(type)) { return advance(); }
        throw error(peek(), message);
    }

    private boolean check(final TokenType tokenType) {
        if (isAtEnd()) { return false; }
        return peek().getType() == tokenType;
    }

    private Token advance() {
        if (!isAtEnd()) { current++; }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(final Token token, final String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().getType() == SEMICOLON) { return; }

            switch (peek().getType()) {
                case  CLASS:
                case    FUN:
                case    VAR:
                case    FOR:
                case     IF:
                case  WHILE:
                case  PRINT:
                case RETURN:
                    return;
                default:
            }

            advance();
        }
    }
}
