package com.github.nomadium.lox;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    private static final String GROUP = "group";
    private static final String LEFT_PAREN = "(";
    private static final String NIL = "nil";
    private static final String RIGHT_PAREN = ")";

    String print(final Expr expr) {
        return expr.accept(this);
    }

    String print(final Stmt stmt) {
        return stmt.accept(this);
    }

    @Override
    public String visitBinaryExpr(final Expr.Binary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(final Expr.Call expr) {
        return parenthesize2("call", expr.callee, expr.arguments);
    }

    @Override
    public String visitGroupingExpr(final Expr.Grouping expr) {
        return parenthesize(GROUP, expr.expression);
    }

    @Override
    public String visitLiteralExpr(final Expr.Literal expr) {
        if (expr.value == null) { return NIL; }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(final Expr.Logical expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitGetExpr(final Expr.Get expr) {
        return parenthesize2(".", expr.object, expr.name.getLexeme());
    }

    @Override
    public String visitSetExpr(final Expr.Set expr) {
        return parenthesize2("=", expr.object, expr.name.getLexeme(), expr.value);
    }

    @Override
    public String visitThisExpr(final Expr.This expr) {
        return "this";
    }

    @Override
    public String visitUnaryExpr(final Expr.Unary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.right);
    }

    @Override
    public String visitVariableExpr(final Expr.Variable expr) {
        return expr.name.getLexeme();
    }

    @Override
    public String visitAssignExpr(final Expr.Assign expr) {
        return parenthesize2("=", expr.name.getLexeme(), expr.value);
    }

    @Override
    public String visitBlockStmt(final Stmt.Block stmt) {
        final StringBuilder builder = new StringBuilder();

        builder.append(LEFT_PAREN)
               .append("block ");

        stmt.statements.stream()
                       .forEach(statement -> builder.append(statement.accept(this)));

        builder.append(RIGHT_PAREN);

        return builder.toString();
    }

    @Override
    public String visitClassStmt(final Stmt.Class stmt) {
        final StringBuilder builder = new StringBuilder();

        builder.append(LEFT_PAREN)
               .append("class ")
               .append(stmt.name.getLexeme());

        stmt.methods.stream()
                    .forEach(method -> builder.append(" " + print(method)));

        builder.append(RIGHT_PAREN);

        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(final Stmt.Expression stmt) {
        return parenthesize(";", stmt.expression);
    }

    @Override
    public String visitFunctionStmt(final Stmt.Function stmt) {
        final StringBuilder builder = new StringBuilder();
        builder.append("(fun " + stmt.name.getLexeme() + "(");

        stmt.parameters.stream().forEach(param -> {
            if (param != stmt.parameters.get(0)) { builder.append(" "); }
            builder.append(param.getLexeme());
        });
        builder.append(") ");

        stmt.body.stream().forEach(body -> builder.append(body.accept(this)));

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(final Stmt.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize2("if", stmt.condition, stmt.thenBranch);
        }
        return parenthesize2("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
    }

    @Override
    public String visitPrintStmt(final Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitReturnStmt(final Stmt.Return stmt) {
        if (stmt.value == null) { return "(return)"; }
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(final Stmt.Var stmt) {
        if (stmt.initializer == null) {
            return parenthesize2("var", stmt.name);
        }
        return parenthesize2("var", stmt.name, "=", stmt.initializer);
    }

    @Override
    public String visitWhileStmt(final Stmt.While stmt) {
        return parenthesize2("while", stmt.condition, stmt.body);
    }

    private String parenthesize(final String name, final Expr... exprs) {
        final StringBuilder builder = new StringBuilder();

        builder.append(LEFT_PAREN);
        builder.append(name);

        Arrays.stream(exprs).forEach(expr -> {
            builder.append(StringUtils.SPACE);
            builder.append(expr.accept(this));
        });
        builder.append(RIGHT_PAREN);

        return builder.toString();
    }

    private String parenthesize2(final String name, final Object... parts) {
        final StringBuilder builder = new StringBuilder();

        builder.append(LEFT_PAREN);
        builder.append(name);

        Arrays.stream(parts).forEach(part -> {
            builder.append(StringUtils.SPACE);
            if (part instanceof Expr) {
                builder.append(((Expr)part).accept(this));
            } else if (part instanceof Stmt) {
                builder.append(((Stmt) part).accept(this));
            } else if (part instanceof Token) {
                builder.append(((Token) part).getLexeme());
            } else {
               builder.append(part);
            }
        });
        builder.append(RIGHT_PAREN);

        return builder.toString();
    }
}
