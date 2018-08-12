package com.github.nomadium.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String USAGE = "Usage: generate_ast <output directory>";

    private static final String EXPR_CLASS_NAME = "Expr";

    private static final String STMT_CLASS_NAME = "Stmt";

    private static final List<String> EXPR_TYPES_LIST = Arrays.asList(
        "Assign   : Token name, Expr value",
        "Binary   : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal  : Object value",
        "Logical  : Expr left, Token operator, Expr right",
        "Unary    : Token operator, Expr right",
        "Variable : Token name"
    );

    private static final List<String> STMT_TYPES_LIST = Arrays.asList(
        "Block      : List<Stmt> statements",
        "Expression : Expr expression",
        "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
        "Print      : Expr expression",
        "Var        : Token name, Expr initializer",
        "While      : Expr condition, Stmt body"
    );

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println(USAGE);
            System.exit(1);
        }

        final String outputDir = args[0];

        defineAst(outputDir, EXPR_CLASS_NAME, EXPR_TYPES_LIST);

        defineAst(outputDir, STMT_CLASS_NAME, STMT_TYPES_LIST);
    }

    private static void defineAst(final String outputDir,
                                  final String baseName,
                                  final List<String> types) throws IOException {
        final String path = outputDir + File.separator + baseName + ".java";
        final PrintWriter writer = new PrintWriter(path, DEFAULT_ENCODING);

        writer.println("package com.github.nomadium.lox;");
        writer.println("");
        writer.println("import java.util.List;");
        writer.println("");
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // The AST classes.
        types.stream().forEach(type -> {
            final String className = type.split(":")[0].trim();
            final String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        });

        // The base accept() method.
        writer.println("");
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");

        writer.close();
    }

    private static void defineType(final PrintWriter writer,
                                   final String baseName,
                                   final String className,
                                   final String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");

        // Constructor.
        writer.println("    " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        Arrays.stream(fieldList.split(", ")).forEach(field -> {
            final String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        });
        writer.println("    }");

        // Visitor pattern.
        writer.println();
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // Fields.
        writer.println();
        Arrays.stream(fieldList.split(", ")).forEach(field -> {
            writer.println("    final " + field + ";");
        });

        writer.println("  }");
    }

    private static void defineVisitor(final PrintWriter writer,
                                      final String baseName,
                                      final List<String> types) {
        writer.println("  interface Visitor<R> {");

        types.stream().forEach(type -> {
            final String typeName = type.split(":")[0].trim();
            writer.println(new StringBuilder().append("    R visit")
                                              .append(typeName)
                                              .append(baseName)
                                              .append("(")
                                              .append(typeName)
                                              .append(" ")
                                              .append(baseName.toLowerCase())
                                              .append(");")
                                              .toString());
        });

        writer.println("  }");
    }
}
