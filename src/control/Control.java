package control;

public class Control {
    public static Object ConAst;
    public static int visualize;
    public static boolean debug = false;

    public enum Verbose {
        SILENT, DETAILED,
    }

    public static Verbose verbose = Verbose.SILENT;

    // the lexer
    public static class Lexer {
        public static boolean dumpToken = false;
    }

    // codegen
    public static class Codegen {
        public static boolean embedComments = true;
        public static boolean finalAssembly = true;
        public static String assemFile = "a.s";
    }
}

