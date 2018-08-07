package com.github.nomadium.lox;

class Interpreter implements Expr.Visitor<Object> {
    private static final String OPERAND_MUST_BE_A_NUMBER = "Operand must be a number.";
    private static final String OPERANDS_MUST_BE_NUMBERS = "Operands must be numbers.";
    private static final String OPERANDS_MUST_BE_NUMBERS_OR_STRINGS
        = "Operands must be two numbers or two strings.";

    void interpret(Expr expression) {
        try {
            final Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (final RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(final Expr.Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.getType()) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, OPERANDS_MUST_BE_NUMBERS_OR_STRINGS);
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            default:
                return null;
        }
    }

    @Override
    public Object visitGroupingExpr(final Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(final Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(final Expr.Unary expr) {
        final Object right = evaluate(expr.right);
        switch (expr.operator.getType()) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
            default:
                return null;
        }
    }

    private void checkNumberOperand(final Token operator, final Object operand) {
        if (operand instanceof Double) { return; }
        throw new RuntimeError(operator, OPERAND_MUST_BE_A_NUMBER);
    }

    private void checkNumberOperands(final Token operator, final Object left, final Object right) {
        if (left instanceof Double && right instanceof Double) { return; }
        throw new RuntimeError(operator, OPERANDS_MUST_BE_NUMBERS);
    }

    private boolean isTruthy(final Object object) {
        if (object == null) { return false; }
        if (object instanceof Boolean) { return (boolean)object; }
        return true;
    }

    private boolean isEqual(final Object o1, final Object o2) {
        // nil is only equal to nil.
        if (o1 == null && o2 == null) { return true; }
        if (o1 == null) { return false; }
        return o1.equals(o2);
    }

    private String stringify(final Object object) {
        if (object == null) { return "nil"; }

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(final Expr expr) {
        return expr.accept(this);
    }
}
