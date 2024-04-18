package control;

import ast.Ast;

public class Control {

    public enum Verbose {
        SILENT(0),
        PASS(1),
        DETAIL(2);
        final int order;

        Verbose(int i) {
            this.order = i;
        }
    }

    public static Verbose verbose = Verbose.SILENT;

    // this is a special hack
    public static Ast.Program.T bultinAst = null;

    // the lexer
    public static class Lexer {
        public static boolean dumpToken = false;
    }

    // the parser
    public static class Parser {
        public static boolean dump = false;
    }

    // the type checker
    public static class Type {
        public static boolean dump = false;
    }

    // the CFG
    public static class Cfg {
        public static boolean dump = false;
    }
}

