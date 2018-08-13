package com.github.nomadium.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private static final String CANNOT_CALL_A_NON_CALLABLE = "Can only call functions and classes.";

    private static final String ONLY_INSTANCES_HAVE_FIELDS = "Only instances have fields.";
    private static final String ONLY_INSTANCES_HAVE_PROPERTIES = "Only instances have properties.";

    private static final String OPERAND_MUST_BE_A_NUMBER = "Operand must be a number.";
    private static final String OPERANDS_MUST_BE_NUMBERS = "Operands must be numbers.";
    private static final String OPERANDS_MUST_BE_NUMBERS_OR_STRINGS
        = "Operands must be two numbers or two strings.";

    private final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    private boolean repl = false;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(final Interpreter interpreter, final List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    Environment getGlobals() {
        return globals;
    }

    void interpret(final List<Stmt> statements, final boolean repl) {
        this.repl = repl;

        try {
            for (final Stmt statement : statements) {
                execute(statement);
            }
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
    public Object visitCallExpr(final Expr.Call expr) {
        final Object callee = evaluate(expr.callee);

        final List<Object> arguments = new ArrayList<>();
        expr.arguments.stream()
                      .forEach(argument -> arguments.add(evaluate(argument)));

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, CANNOT_CALL_A_NON_CALLABLE);
        }

        final LoxCallable function = (LoxCallable)callee;

        if (arguments.size() != function.arity()) {
            final String arityError = new StringBuilder().append("Expected ")
                                                         .append(function.arity())
                                                         .append(" arguments but got ")
                                                         .append(arguments.size())
                                                         .append(".")
                                                         .toString();
            throw new RuntimeError(expr.paren, arityError);
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(final Expr.Get expr) {
        final Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance)object).get(expr.name);
        }

        throw new RuntimeError(expr.name, ONLY_INSTANCES_HAVE_PROPERTIES);
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
    public Object visitLogicalExpr(final Expr.Logical expr) {
        final Object left = evaluate(expr.left);

        if (expr.operator.getType() == TokenType.OR) {
            if (isTruthy(left)) { return left; }
        } else {
            if (!isTruthy(left)) { return left; }
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(final Expr.Set expr) {
        final Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, ONLY_INSTANCES_HAVE_FIELDS);
        }

        final Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(final Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
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

    @Override
    public Object visitVariableExpr(final Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(final Token name, final Expr expr) {
        final Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.getLexeme());
        } else {
            return globals.get(name);
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

    private void execute(final Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(final Expr expr, final int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(final List<Stmt> statements, final Environment environment) {
        final Environment previous = this.environment;
        try {
            this.environment = environment;

            for (final Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(final Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(final Stmt.Class stmt) {
        environment.define(stmt.name.getLexeme(), null);

        final Map<String, LoxFunction> methods = new HashMap<>();
        stmt.methods.stream().forEach(method -> {
            final LoxFunction function = new LoxFunction(method,
                                                         environment,
                                                         method.name.getLexeme().equals("init"));
            methods.put(method.name.getLexeme(), function);
        });

        final LoxClass klass = new LoxClass(stmt.name.getLexeme(), methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(final Stmt.Expression stmt) {
        final Object value = evaluate(stmt.expression);
        if (this.repl) { System.out.println("=> " + stringify(value)); }
        return null;
    }

    @Override
    public Void visitFunctionStmt(final Stmt.Function stmt) {
        final LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.getLexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(final Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(final Stmt.Print stmt) {
        final Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(final Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) { value = evaluate(stmt.value); }
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(final Stmt.Var stmt) {
        Object value = null;

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.getLexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(final Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(final Expr.Assign expr) {
        final Object value = evaluate(expr.value);

        final Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }
}
