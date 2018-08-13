package com.github.nomadium.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private static final String CANNOT_RETURN_A_VALUE_FROM_AN_INITIALIZER
        = "Cannot return a value from an initializer";

    private static final String CANNOT_RETURN_FROM_TOPLEVEL_CODE
        = "Cannot return from top-level code.";

    private static final String CANNOT_READ_LOCAL_VARIABLE_IN_ITS_OWN_INITIALIZER
        = "Cannot read local variable in its own initializer.";

    private static final String CANNOT_USE_THIS_OUTSIDE_OF_A_CLASS
        = "Cannot use 'this' outside of a class.";

    private static final String VARIABLE_ALREADY_DECLARED_IN_THIS_SCOPE
        = "Variable with this name already declared in this scope.";

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS
    }

    @Override
    public Void visitBlockStmt(final Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(final Stmt.Class stmt) {
        final ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        beginScope();
        scopes.peek().put("this", true);

        stmt.methods.stream().forEach(method -> {
            FunctionType declaration = FunctionType.METHOD;

            if (method.name.getLexeme().equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }

            resolveFunction(method, declaration);
        });

        endScope();

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitExpressionStmt(final Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(final Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) { resolve(stmt.elseBranch); }
        return null;
    }

    @Override
    public Void visitPrintStmt(final Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(final Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, CANNOT_RETURN_FROM_TOPLEVEL_CODE);
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, CANNOT_RETURN_A_VALUE_FROM_AN_INITIALIZER);
            }
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(final Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitFunctionStmt(final Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitVarStmt(final Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(final Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.getLexeme()) == Boolean.FALSE) {
            Lox.error(expr.name, CANNOT_READ_LOCAL_VARIABLE_IN_ITS_OWN_INITIALIZER);
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(final Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(final Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(final Expr.Call expr) {
        resolve(expr.callee);
        expr.arguments.stream().forEach(argument -> resolve(argument));
        return null;
    }

    @Override
    public Void visitGetExpr(final Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(final Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(final Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(final Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(final Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(final Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, CANNOT_USE_THIS_OUTSIDE_OF_A_CLASS);
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(final Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    void resolve(final List<Stmt> statements) {
        statements.stream().forEach(stmt -> resolve(stmt));
    }

    private void resolve(final Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(final Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(final Stmt.Function function, final FunctionType type) {
        final FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        function.parameters.stream().forEach(param -> {
            declare(param);
            define(param);
        });
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(final Token name) {
        if (scopes.isEmpty()) { return; }

        final Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.getLexeme())) {
            Lox.error(name, VARIABLE_ALREADY_DECLARED_IN_THIS_SCOPE);
        }

        scope.put(name.getLexeme(), false);
    }

    private void define(final Token name) {
        if (scopes.isEmpty()) { return; }
        scopes.peek().put(name.getLexeme(), true);
    }

    private void resolveLocal(final Expr expr, final Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.getLexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
        // Not found. Assume it is global.
    }
}
