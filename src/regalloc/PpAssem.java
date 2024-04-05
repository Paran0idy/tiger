package regalloc;

import codegen.X64;
import codegen.X64.*;
import control.Control;
import util.Label;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.function.BiFunction;

public class PpAssem {

    static BufferedWriter writer;

    // the pretty printer
    // assembly need tabs, instead of white spaces
    private static int indentLevel = 0;

    private static void indent() {
        indentLevel += 1;
    }

    private static void unIndent() {
        indentLevel -= 1;
    }

    private static void printSpaces() {
        int i = indentLevel;
        while (i-- != 0) {
            say("\t");
        }
    }

    private static <T> void sayln(String s) {
        try {
            writer.write(s);
            writer.newLine();
        } catch (Exception _) {
        }
    }

    private static <T> void say(String s) {
        try {
            writer.write(s);
        } catch (Exception _) {
        }
    }

    // ///////////////////////////////////////////////////
    // declaration

    public void ppDec(X64.Dec.T dec) {
        switch (dec) {
            case X64.Dec.Singleton(X64.Type.T type, String id) -> {
                //Type.pp(type);
                say(" " + id);
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // virtual function table
    public static void ppVtable(X64.Vtable.T vtable) {
        switch (vtable) {
            case X64.Vtable.Singleton(
                    String name,
                    List<String> funcs
            ) -> {
                printSpaces();
                say(STR."""
.V_\{name}:
""");
                // all entries
                indent();
                for (String s : funcs) {
                    printSpaces();
                    sayln(STR.".quad \{s}");
                }
                unIndent();
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // structures

    // /////////////////////////////////////////////////////////
    // virtual regs
    public void ppVirtualReg(X64.VirtualReg.T vt) {
        switch (vt) {
            case X64.VirtualReg.Id(String x, _) -> {
                throw new AssertionError(x);
            }
            case X64.VirtualReg.Reg(String x, X64.Type.T type) -> {
                say(x);
            }
        }
    }
    // end of virtual regs


    // /////////////////////////////////////////////////////////
    // instruction
    public void ppInstr(X64.Instr.T t) {
        switch (t) {
            case X64.Instr.Bop(
                    BiFunction<List<X64.VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn,
                        (List<VirtualReg.T>) uses,
                        (List<VirtualReg.T>) defs);
            }
            case Instr.CallDirect(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.CallIndirect(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.Comment(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                if (Control.Codegen.embedComments)
                    printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.Load(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.Move(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.MoveConst(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            case Instr.Store(
                    BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                    List<VirtualReg.T> uses,
                    List<VirtualReg.T> defs
            ) -> {
                printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
            }
            default -> {
                throw new AssertionError();
            }
        }
    }


    private void printInstrBody(BiFunction<List<VirtualReg.T>,
            List<VirtualReg.T>, String> instrFn,
                                List<VirtualReg.T> uses,
                                List<VirtualReg.T> defs) {
        printSpaces();
        sayln(instrFn.apply(uses, defs));
//        say("\t// uses=[");
//        for (VirtualReg.T use : uses) {
//            VirtualReg.pp(use);
//            say(", ");
//        }
//        say("], defs=[");
//        for (VirtualReg.T def : defs) {
//            VirtualReg.pp(def);
//            say(", ");
//        }
//        sayln("]");
    }
    // end of statement


    // /////////////////////////////////////////////////////////
    // transfer
    public void ppTransfer(X64.Transfer.T t) {
        switch (t) {
            case X64.Transfer.If(String instr, X64.Block.T thenn, X64.Block.T elsee) -> {
                printSpaces();
                say(STR."\{instr} ");
                sayln(X64.Block.getName(thenn));
                printSpaces();
                say("jmp ");
                sayln(X64.Block.getName(elsee));
            }
            case X64.Transfer.Jmp(X64.Block.T target) -> {
                printSpaces();
                sayln(STR."jmp \{Block.getName(target)}");
            }
            case X64.Transfer.Ret() -> {
                printSpaces();
                sayln("ret ");
            }
        }
    }

    // /////////////////////////////////////////////////////////
// block
    public void ppBlock(X64.Block.T b) {
        switch (b) {
            case X64.Block.Singleton(
                    Label label,
                    List<X64.Instr.T> stms,
                    List<X64.Transfer.T> transfers
            ) -> {
                printSpaces();
                say(STR."""
\{label.toString()}:
""");
                indent();
                for (X64.Instr.T s : stms) {
                    ppInstr(s);
                }
                ppTransfer(transfers.getFirst());
                unIndent();
            }
        }
    }// end of basic block

    // /////////////////////////////////////////////////////////
// function
    public void ppFunction(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    Type.T retType, String id, List<Dec.T> formals, List<Dec.T> locals, List<Block.T> blocks
            ) -> {
                printSpaces();
//                Type.pp(retType);
                sayln(STR."\t.globl \{id}");
                printSpaces();
//                Type.pp(retType);
                sayln(STR."\{id}:");
//                for (Dec.T dec : formals) {
//                    Dec.pp(dec);
//                    say(", ");
//                }
//                say("){\n");
//                indent();
//                for (Dec.T dec : locals) {
//                    printSpaces();
//                    Dec.pp(dec);
//                    sayln(";");
//                }
                for (Block.T block : blocks) {
                    ppBlock(block);
                }
//                unIndent();
                printSpaces();
                say("\n\n");
            }

        }
    }

    public void ppProgram(X64.Program.T prog) {
        switch (prog) {
            case X64.Program.Singleton(
                    String entryFuncName,
                    List<X64.Vtable.T> vtables,
                    List<X64.Struct.T> structs,
                    List<X64.Function.T> functions
            ) -> {
                // set up the output stream
                try {
                    writer = new BufferedWriter(new FileWriter(Control.Codegen.assemFile));
                } catch (Exception _) {
                    throw new AssertionError();
                }
                //
                printSpaces();
                sayln("// x64 assembly generated by the Tiger compiler.");
                printSpaces();
                sayln("// Do NOT modify.");
                // vtables
                printSpaces();
                sayln("\t.data");
                for (X64.Vtable.T vtable : vtables) {
                    ppVtable(vtable);
                }
                // structs
//                for (X64.Struct.T struct : structs) {
//                    //ppStruct(struct);
//                }
                printSpaces();
                sayln("\t.text");
                // functions:
                for (X64.Function.T func : functions) {
                    ppFunction(func);
                }
                // an entry:
                printSpaces();
                sayln(STR."\t.globl Tiger_main");
                printSpaces();
                sayln(STR."Tiger_main:");
                indent();
                printSpaces();
                sayln(STR."call\t\{entryFuncName}");
                printSpaces();
                sayln(STR."ret");
                unIndent();

                // extra information to make the assembler happy...

                printSpaces();
                sayln("");
                sayln(STR."\t.ident\t\"Tiger compiler: 0.1\"");
                printSpaces();
                sayln(STR."\t.section\t.note.GNU-stack,\"\",@progbits");

                try {
                    writer.close();
                } catch (Exception _) {
                    throw new AssertionError();
                }
            }
        }
    }
}
