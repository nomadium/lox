package com.github.nomadium.lox;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// put a javadoc for the class
public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    // https://www.freebsd.org/cgi/man.cgi?query=sysexits&apropos=0&sektion=0&manpath=FreeBSD+4.3-RELEASE&format=html
    private static final int EX_USAGE = 64;
    private static final int EX_DATAERR = 65;
    private static final int EX_SOFTWARE = 70;

    private static final String DEFAULT_CHARSET_NAME = "UTF-8";
    private static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);

    private static final String USAGE = "Usage: jlox [script]";
    private static final String PROMPT = "> ";

    private static final Expr NIL = new Expr.Literal(null);

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println(USAGE);
            System.exit(EX_USAGE);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(final String path) throws IOException {
        final byte[] bytes = Files.readAllBytes(Paths.get(path));
        final boolean repl = false;
        run(new String(bytes, DEFAULT_CHARSET), repl);

        // Indicate an error in the exit code.
        if (hadError) { System.exit(EX_DATAERR); }
        if (hadRuntimeError) { System.exit(EX_SOFTWARE); }
    }

    private static void runPrompt() throws IOException {
        final InputStreamReader input = new InputStreamReader(System.in, DEFAULT_CHARSET);
        final BufferedReader reader = new BufferedReader(input);
        final boolean repl = true;

        for (;;) {
            System.out.print(PROMPT);
            // pass a flag here to the interpreter to signal repl mode and print the results of expressions...
            run(reader.readLine(), repl);
            hadError = false;
        }
    }

    static void run(final String source, final boolean repl) {
        final Scanner scanner = new Scanner(source);
        final List<Token> tokens = scanner.scanTokens();
        final Parser parser = new Parser(tokens);
        final List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) { return; }

        final Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError) { return; }

        interpreter.interpret(statements, repl);
    }

    static void error(final int line, final String message) {
        report(line, StringUtils.EMPTY, message);
    }

    static void error(final Token token, final String message) {
        if (token.getType() == TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }

    static void runtimeError(final RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.getToken().getLine() + "]");
        hadRuntimeError = true;
    }

    private static void report(final int line, final String where, final String message) {
        final String fullReportMessage = "[line " + line + "] Error" + where + ": " + message;
        System.err.println(fullReportMessage);
        hadError = true;
    }
}
