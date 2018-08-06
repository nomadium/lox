package com.github.nomadium.lox;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

class AstPrinter implements Expr.Visitor<String> {

    private static final String GROUP = "group";
    private static final String LEFT_PAREN = "(";
    private static final String NIL = "nil";
    private static final String RIGHT_PAREN = ")";

    String print(final Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(final Expr.Binary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
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
    public String visitUnaryExpr(final Expr.Unary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.right);
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
}
