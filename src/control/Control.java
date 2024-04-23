package control;

import ast.Ast;

import java.util.LinkedList;
import java.util.List;

public class Control {

    public enum Verbose {
        LM1(-1),
        L0(0), // top level
        L1(1),
        L2(2);
        public final int order;

        Verbose(int i) {
            this.order = i;
        }
    }

    public static Verbose verbose = Verbose.LM1;

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

