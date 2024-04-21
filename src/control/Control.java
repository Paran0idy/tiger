package control;

import ast.Ast;

import java.util.LinkedList;
import java.util.List;

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

    static List<String> tracedMethods = new LinkedList<>();

    public static boolean beingTraced(String method) {
        return tracedMethods.contains(method);
    }


    // this is a special hack to test the compiler
    // without hacking the lexer and parser.
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
        public static String dotOutputFormat = "png";
        public static boolean dump = false;

    }

    // the x64
    public static class X64 {
        public static boolean embedComments = false;
        public static String assemFile = null;
        public static boolean dump = false;
    }
}

