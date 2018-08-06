package com.github.nomadium.lox;

import static com.github.nomadium.lox.TokenType.*;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    private static final Map<String, TokenType> keywords;

    private static final String UNEXPECTED_CHARACTER = "Unexpected character.";
    private static final String UNTERMINATED_STRING = "Unterminated string.";

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int current = 0;
    private int start = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        tokens.add(Token.builder()
                        .type(EOF)
                        .lexeme(StringUtils.EMPTY)
                        .literal(null)
                        .line(line)
                        .build());
        return tokens;
    }

    private void scanToken() {
        final char ch = advance();
        switch (ch) {
            case '(': addToken(LEFT_PAREN);  break;
            case ')': addToken(RIGHT_PAREN); break;
            case '}': addToken(LEFT_BRACE);  break;
            case '{': addToken(RIGHT_PAREN); break;
            case ',': addToken(COMMA);       break;
            case '.': addToken(DOT);         break;
            case '-': addToken(MINUS);       break;
            case '+': addToken(PLUS);        break;
            case ';': addToken(SEMICOLON);   break;
            case '*': addToken(STAR);        break;
            case '!': addToken(match('=') ? BANG_EQUAL    : BANG);    break;
            case '=': addToken(match('=') ? EQUAL_EQUAL   : EQUAL);   break;
            case '<': addToken(match('=') ? LESS_EQUAL    : LESS);    break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while ((peek() != '\n') && !isAtEnd()) { advance(); }
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(ch)) {
                    number();
                } else if (isAlpha(ch)) {
                    identifier();
                } else {
                    Lox.error(line, UNEXPECTED_CHARACTER);
                    break;
                }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) { advance(); }

        // See if the identifier is a reserved word.
        final String text = source.substring(start, current);
        final TokenType type = keywords.getOrDefault(text, IDENTIFIER);

        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) { advance(); }

        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) { advance(); }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while ((peek() != '"') && !isAtEnd()) {
            if (peek() == '\n') { line++; }
            advance();
        }

        // Unterminated string.
        if (isAtEnd()) {
            Lox.error(line, UNTERMINATED_STRING);
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        final String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) { return false; }
        if (source.charAt(current) != expected) { return false; }

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) { return '\0'; }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) { return '\0'; }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(final char ch) {
        return (ch >= 'a' && ch <= 'z')
               || (ch >= 'A' && ch <= 'Z')
               || (ch == '_');
    }

    private boolean isAlphaNumeric(final char ch) {
        return isAlpha(ch) || isDigit(ch);
    }

    private boolean isDigit(final char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(final TokenType type) {
        addToken(type, null);
    }

    private void addToken(final TokenType type, final Object literal) {
        final String text = source.substring(start, current);
        tokens.add(Token.builder()
                        .type(type)
                        .lexeme(text)
                        .literal(literal)
                        .line(line)
                        .build());
    }
}
