package com.github.nomadium.lox;

import static com.github.nomadium.lox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Lox language grammar:
 *
 * program     -> declaration* EOF ;
 *
 * declaration -> funDecl
 *                | varDecl
 *                | statement ;
 *
 * funDecl     -> "func" function;
 * function    -> IDENTIFIER "(" parameters? ")" block ;
 * parameters  -> IDENTIFIER ( "," IDENTIFIER )* ;
 *
 * varDecl     -> "var" IDENTIFIER ( "=" expression )? ";" ;
 *
 * statement   -> exprStmt
 *                | forStmt
 *                | ifStmt
 *                | printStmt
 *                | returnStmt
 *                | whileStmt
 *                | block ;
 *
 * exprStmt    -> expression ";" ;
 *
 * forStmt      -> "for" "(" ( varDecl | exprStmt | ";" )
 *                           expression? ";"
 *                           expression? ")" statement ;
 *
 * ifStmt      -> "if" "(" expression ")" statement ( "else" statement )? ;
 *
 * printStmt   -> "print" expression ";" ;
 *
 * printStmt   -> "return" expression? ";" ;
 *
 * block       -> "{" declaration* "}" ;
 *
 *
 * Lox expression grammar:
 *
 * expression     -> assignment ;
 *
 * assignment     -> identifier "=" assignment | logic_or ;
 *
 * logic_or       -> logic_and ( "or" logic_and)* ;
 *
 * logic_and      -> equality ( "and" equality)* ;
 *
 * equality       -> comparison ( ( "!=" | "==" ) comparison )* ;
 *
 * comparison     -> addition ( ( ">"  | ">=" | "<" | "<=" ) addition )* ;
 *
 * addition       -> multiplication ( ( "-"  | "+" ) multiplication )* ;
 *
 * multiplication -> unary ( ( "/" | "*" ) unary )* ;
 *
 * unary          -> ( "!" | "-" ) unary | call ;
 *
 * call           -> primary ( "(" arguments? ")" )* ;
 *
 * arguments      -> expression ( "," expression )* ;
 *
 * primary        -> "true" | "false" | "nil" | "this"
 *                   | NUMBER | STRING
 *                   | "(" expression ")"
 *                   | IDENTIFIER ;
 *
 */
class Parser {
    private static final int MAX_ARGS_SIZE = 8;
    private static final String CANNOT_HAVE_MORE_THAN_MAX_ARGS_SIZE_ARGUMENTS
        = "Cannot have more than " + MAX_ARGS_SIZE + " arguments.";

    private static final String CANNOT_HAVE_MORE_THAN_MAX_ARGS_SIZE_PARAMETERS
        = "Cannot have more than " + MAX_ARGS_SIZE + " parameters.";

    private static final String EXPECT_EXPRESSION = "Expect expression.";

    private static final String EXPECT_LEFT_PAREN_AFTER_FOR = "Expect '(' after 'for'.";

    private static final String EXPECT_PARAMETER_NAME = "Expect parameter name.";

    private static final String EXPECT_RIGHT_PAREN_AFTER_FOR_CLAUSES
        = "Expect ')' after for clauses.";

    private static final String EXPECT_LEFT_PAREN_AFTER_IF = "Expect '(' after 'if'.";

    private static final String EXPECT_LEFT_PAREN_AFTER_WHILE = "Expect '(' after 'while'.";

    private static final String EXPECT_RIGHT_PAREN_AFTER_IF_CONDITION
        = "Expect ')' after if condition.";

    private static final String EXPECT_RIGHT_PAREN_AFTER_ARGUMENTS = "Expect ')' after arguments.";
    private static final String EXPECT_RIGHT_PAREN_AFTER_CONDITION = "Expect ')' after condition.";
    private static final String EXPECT_RIGHT_PAREN_AFTER_PARAMETERS
        = "Expect ')' after parameters.";

    private static final String EXPECT_RIGHT_BRACE_AFTER_BLOCK = "Expect '}' after block.";

    private static final String EXPECT_RIGHT_PAREN = "Expect ')' after expression.";

    private static final String EXPECT_SEMICOLON_AFTER_EXPRESSION = "Expect ';' after expression.";

    private static final String EXPECT_SEMICOLON_AFTER_LOOP_CONDITION
        = "Expect ';' after loop condition.";

    private static final String EXPECT_SEMICOLON_AFTER_RETURN_VALUE
        = "Expect ';' after return value.";

    private static final String EXPECT_SEMICOLON_AFTER_VALUE = "Expect ';' after value.";

    private static final String EXPECT_SEMICOLON_AFTER_VAR_DECLARATION
        = "Expect ';' after variable declaration.";

    private static final String EXPECT_VARIABLE_NAME = "Expect variable name.";

    private static final String INVALID_ASSIGNMENT_TARGET = "Invalid assignment target.";

    private static final class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        final List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(FUN)) { return function("function"); }
            if (match(VAR)) { return varDeclaration(); }
            return statement();
        } catch (final ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR))        { return forStatement(); }
        if (match(IF))         { return ifStatement(); }
        if (match(PRINT))      { return printStatement(); }
        if (match(RETURN))     { return returnStatement(); }
        if (match(WHILE))      { return whileStatement(); }
        if (match(LEFT_BRACE)) { return new Stmt.Block(block()); }
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, EXPECT_LEFT_PAREN_AFTER_FOR);

        final Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, EXPECT_SEMICOLON_AFTER_LOOP_CONDITION);

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN_AFTER_FOR_CLAUSES);

        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body,
                                                new Stmt.Expression(increment)));
        }

        if (condition == null) { condition = new Expr.Literal(true); }
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, EXPECT_LEFT_PAREN_AFTER_IF);
        final Expr condition = expression();
        consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN_AFTER_IF_CONDITION);

        final Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        final Expr value = expression();
        consume(SEMICOLON, EXPECT_SEMICOLON_AFTER_VALUE);
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        final Token keyword = previous();

        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, EXPECT_SEMICOLON_AFTER_RETURN_VALUE);
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        final Token name = consume(IDENTIFIER, EXPECT_VARIABLE_NAME);

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, EXPECT_SEMICOLON_AFTER_VAR_DECLARATION);
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, EXPECT_LEFT_PAREN_AFTER_WHILE);
        final Expr condition = expression();
        consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN_AFTER_CONDITION);
        final Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        final Expr expr = expression();
        consume(SEMICOLON, EXPECT_SEMICOLON_AFTER_EXPRESSION);
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(final String kind) {
        final Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        final List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= MAX_ARGS_SIZE) {
                    error(peek(), CANNOT_HAVE_MORE_THAN_MAX_ARGS_SIZE_PARAMETERS);
                }

                parameters.add(consume(IDENTIFIER, EXPECT_PARAMETER_NAME));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN_AFTER_PARAMETERS);

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        final List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        final List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, EXPECT_RIGHT_BRACE_AFTER_BLOCK);
        return statements;
    }

    private Expr assignment() {
        final Expr expr = or();

        if (match(EQUAL)) {
            final Token equals = previous();
            final Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                final Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, INVALID_ASSIGNMENT_TARGET);
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            final Token operator = previous();
            final Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            final Token operator = previous();
            final Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
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

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(final Expr callee) {
        final List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= MAX_ARGS_SIZE) {
                    error(peek(), CANNOT_HAVE_MORE_THAN_MAX_ARGS_SIZE_ARGUMENTS);
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        final Token paren = consume(RIGHT_PAREN, EXPECT_RIGHT_PAREN_AFTER_ARGUMENTS);

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) { return new Expr.Literal(false); }
        if (match(TRUE))  { return new Expr.Literal(true);  }
        if (match(NIL))   { return new Expr.Literal(null);  }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().getLiteral());
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
