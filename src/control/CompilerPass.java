package control;

import java.util.function.Function;

// a compiler pass is a type from "FromType" to "ToType"
public class CompilerPass<FromType, ToType> {
    // name of this pass
    private final String passName;
    // when does this pass start
    private long startTime;
    // when does this pass end
    private long endTime;
    // the translator
    private final Function<FromType, ToType> transformation;
    // the source data structure
    private final FromType from;

    // pretty printing
    private static int indentSize = 0;
    private final int steps = 3;

    private void printSpaces() {
        int n = indentSize;
        if (n < 0) {
            throw new util.Error("compiler bug");
        }
        while (n-- != 0) {
            System.out.print(" ");
        }
    }

    private void indent() {
        indentSize += steps;
    }

    private void unindent() {
        indentSize -= steps;
    }

    public CompilerPass(String passName, Function<FromType, ToType> transformation, FromType from) {
        this.passName = passName;
        this.startTime = 0;
        this.endTime = 0;
        this.transformation = transformation;
        this.from = from;
    }

    public ToType apply() {
        if (Control.verbose != Control.Verbose.SILENT) {
            printSpaces();
            indent();
            System.out.println(STR."\{this.passName} starting");
            if (Control.verbose == Control.Verbose.DETAILED) {
                this.startTime = System.nanoTime();
            }
        }

        ToType to = this.transformation.apply(this.from);

        if (Control.verbose != Control.Verbose.SILENT) {
            unindent();
            printSpaces();
            System.out.print(STR."\{this.passName} finished");
            if (Control.verbose == Control.Verbose.DETAILED) {
                this.endTime = System.nanoTime();
                System.out.print(STR.": @ \{(this.endTime - this.startTime) / 1000}ms");
            }
            System.out.println();
        }
        return to;
    }
}

