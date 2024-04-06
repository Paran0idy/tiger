package codegen;

import util.Label;

import java.util.List;
import java.util.function.BiFunction;

public class X64 {

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

    private static <T> void sayln(T s) {
        System.out.println(s);
    }

    private static <T> void say(T s) {
        System.out.print(s);
    }

    //  ///////////////////////////////////////////////////////////
    //  word size and alignment
    public static class WordSize {
        public static int bytesOfWord = 8;

    }

    //  ///////////////////////////////////////////////////////////
    //  physical registers
    public static class Register {
        public static List<String> allRegs = List.of(
                "rax",
                "rbx",
                "rcx",
                "rdx",
                "rdi",
                "rsi",
                "rbp",
                "rsp",
                "r8",
                "r9",
                "r10",
                "r11",
                "r12",
                "r13",
                "r14",
                "r15");

        // the first 6 arguments are passed through the following registers:
        public static List<String> argPassingRegs = List.of(
                "rdi",
                "rsi",
                "rdx",
                "rcx",
                "r8",
                "r9");

        // the return value register
        public static String retReg = "rax";

        // callee-saved regs
        public static List<String> calleeSavedRegs = List.of(
                "rdi",
                "rsi",
                "rdx",
                "rcx",
                "r8",
                "r9");

        // caller-saved regs
        public static List<String> callerSavedRegs = List.of(
                "rdi",
                "rsi",
                "rdx",
                "rcx",
                "r8",
                "r9");


    }


    //  ///////////////////////////////////////////////////////////
    //  type
    public static class Type {
        public sealed interface T permits
                ClassType,
                Int,
                IntArray,
                Ptr,
                PtrCode {
        }

        public record Int() implements T {
        }

        public record ClassType(String id) implements T {
        }

        public record IntArray() implements T {
        }

        public record Ptr() implements T {
        }

        // a pointer to code
        public record PtrCode() implements T {
        }

        public static void pp(T ty) {
            switch (ty) {
                case Int() -> {
                    say("int");
                }
                case ClassType(String id) -> {
                    say(id);
                }
                case IntArray() -> {
                    say("int[]");
                }
                case Ptr() -> {
                    say("Ptr");
                }
                case PtrCode() -> {
                    say("PtrCode");
                }
            }
        }
    }

    // ///////////////////////////////////////////////////
    // declaration
    public static class Dec {
        public sealed interface T permits
                Singleton {
        }

        public record Singleton(Type.T type,
                                String id) implements T {
        }

        public static void pp(T dec) {
            switch (dec) {
                case Singleton(Type.T type, String id) -> {
                    Type.pp(type);
                    say(STR." \{id}");
                }
            }
        }

    }

    // /////////////////////////////////////////////////////////
    // virtual function table
    public static class Vtable {
        public sealed interface T permits
                Singleton {
        }

        public record Singleton(String name,
                                List<String> funcs) implements T {
        }

        public static void pp(T vtable) {
            switch (vtable) {
                case Singleton(
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
                        sayln(STR."\t.long long \{s}");
                    }
                    unIndent();
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // structures
    public static class Struct {
        public sealed interface T permits Singleton {
        }

        public record Singleton(String clsName,
                                List<Dec.T> fields) implements T {
        }

        public static void pp(T s) {
            switch (s) {
                case Singleton(String clsName, List<Dec.T> fields) -> {
                    printSpaces();
                    say(STR."""
struct S_\{clsName} {
""");
                    indent();
                    // the first field is special
                    printSpaces();
                    say(STR."""
struct V_\{clsName} *vptr;
""");
                    for (Dec.T dec : fields) {
                        printSpaces();
                        Dec.pp(dec);
                    }
                    unIndent();
                    printSpaces();
                    say(STR."""
} S_\{clsName}_ = {
""");
                    indent();
                    printSpaces();
                    say(STR."""
.vptr = &V_\{clsName}_;
""");
                    unIndent();
                    printSpaces();
                    say("};\n\n");
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // values
    public static class VirtualReg {
        public sealed interface T permits
                Id,
                Reg {
        }

        // variable
        public record Id(String x, Type.T ty) implements T {
        }

        // physical register
        public record Reg(String x, Type.T ty) implements T {
        }

        public static void pp(T ty) {
            switch (ty) {
                case Id(String x, _) -> {
                    say(x);
                }
                case Reg(String x, Type.T type) -> {
                    say(x);
                }
            }
        }
    }
    // end of virtual register

    // /////////////////////////////////////////////////////////
    // instruction
    public static class Instr {
        public sealed interface T permits
                Bop,
                CallDirect,
                CallIndirect,
                Comment,
                Load,
                Move,
                MoveConst {
        }


        // assign
        public record Bop(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                          List<VirtualReg.T> uses,
                          List<VirtualReg.T> defs) implements T {
        }

        // call-direct
        public record CallDirect(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                 List<VirtualReg.T> uses,
                                 List<VirtualReg.T> defs) implements T {
        }

        // call-direct
        public record CallIndirect(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                   List<VirtualReg.T> uses,
                                   List<VirtualReg.T> defs) implements T {
        }

        // comment
        public record Comment(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                              List<VirtualReg.T> uses,
                              List<VirtualReg.T> defs) implements T {
        }


        public record Load(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                           List<VirtualReg.T> uses,
                           List<VirtualReg.T> defs) implements T {
        }

        public record Move(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                           List<VirtualReg.T> uses,
                           List<VirtualReg.T> defs) implements T {
        }

        public record MoveConst(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                List<VirtualReg.T> uses,
                                List<VirtualReg.T> defs) implements T {
        }


        public static void pp(T t) {
            switch (t) {
                case Bop(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                case CallDirect(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                case CallIndirect(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                case Comment(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody((BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String>) instrFn, (List<VirtualReg.T>) uses, (List<VirtualReg.T>) defs);
                }
                case Load(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                case Move(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                case MoveConst(
                        BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instrFn,
                        List<VirtualReg.T> uses,
                        List<VirtualReg.T> defs
                ) -> {
                    printInstrBody(instrFn, uses, defs);
                }
                default -> {
                    throw new AssertionError();
                }
            }
        }

        private static void printInstrBody(BiFunction<List<VirtualReg.T>,
                List<VirtualReg.T>, String> instrFn,
                                           List<VirtualReg.T> uses,
                                           List<VirtualReg.T> defs) {
            printSpaces();
            say(instrFn.apply(uses, defs));
            say("\t// uses=[");
            for (VirtualReg.T use : uses) {
                VirtualReg.pp(use);
                say(", ");
            }
            say("], defs=[");
            for (VirtualReg.T def : defs) {
                VirtualReg.pp(def);
                say(", ");
            }
            sayln("]");
        }
    }
    // end of instructions


    // /////////////////////////////////////////////////////////
    // transfer
    public static class Transfer {
        public sealed interface T permits
                If,
                Jmp,
                Ret {
        }

        public record If(String instr,
                         Block.T trueBlock,
                         Block.T falseBlock) implements T {
        }

        public record Jmp(Block.T target) implements T {
        }

        public record Ret() implements T {
        }

        public static void pp(T t) {
            switch (t) {
                case If(
                        String instr,
                        Block.T thenn,
                        Block.T elsee
                ) -> {
                    printSpaces();
                    say(STR."\{instr} ");
                    sayln(Block.getName(thenn));
                    printSpaces();
                    say("jmp ");
                    sayln(Block.getName(elsee));
                }
                case Jmp(Block.T target) -> {
                    printSpaces();
                    sayln(STR."jmp \{Block.getName(target)}");
                }
                case Ret() -> {
                    printSpaces();
                    sayln("ret ");
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // block
    public static class Block {
        public sealed interface T permits Singleton {
        }

        public record Singleton(Label label,
                                List<Instr.T> instrs,
                                // this is a special hack
                                // the transfer field is final, so that
                                // we use a list instead of a singleton field
                                List<Transfer.T> transfer) implements T {
        }

        public static Label getLabel(Block.T t) {
            switch (t) {
                case Singleton(Label label, _, _) -> {
                    return label;
                }
            }
        }

        public static String getName(Block.T t) {
            switch (t) {
                case Singleton(Label label, _, _) -> {
                    return label.toString();
                }
            }
        }

        public static void pp(T b) {
            switch (b) {
                case Singleton(
                        Label label,
                        List<Instr.T> stms,
                        List<Transfer.T> transfers
                ) -> {
                    printSpaces();
                    sayln(STR."\{label.toString()}:");
                    indent();
                    for (Instr.T s : stms) {
                        Instr.pp(s);
                    }
                    Transfer.pp(transfers.getFirst());
                    unIndent();
                }
            }
        }
    }// end of basic block

    // /////////////////////////////////////////////////////////
    // function
    public static class Function {
        public sealed interface T permits Singleton {
        }

        public record Singleton(Type.T retType,
                                String id,
                                List<Dec.T> formals,
                                List<Dec.T> locals,
                                List<Block.T> blocks) implements T {
        }

        public static Block.T getBlock(T func, Label label) {
            switch (func) {
                case Singleton(
                        Type.T retType,
                        String id,
                        List<Dec.T> formals,
                        List<Dec.T> locals,
                        List<Block.T> blocks
                ) -> {
                    return blocks.stream().filter(x -> X64.Block.getLabel(x).equals(label)).toList().getFirst();
                }
            }
        }

        public static void pp(T f) {
            switch (f) {
                case Singleton(
                        Type.T retType, String id, List<Dec.T> formals, List<Dec.T> locals, List<Block.T> blocks
                ) -> {
                    printSpaces();
                    Type.pp(retType);
                    say(STR." \{id}(");
                    for (X64.Dec.T dec : formals) {
                        Dec.pp(dec);
                        say(", ");
                    }
                    sayln("){");
                    indent();
                    for (Dec.T dec : locals) {
                        printSpaces();
                        Dec.pp(dec);
                        sayln(";");
                    }
                    for (Block.T block : blocks) {
                        Block.pp(block);
                    }
                    unIndent();
                    printSpaces();
                    say("}\n\n");
                }
            }
        }
    }

    // whole program
    public static class Program {
        public sealed interface T permits Singleton {
        }

        public record Singleton(String entryFuncName, // name of the entry function
                                List<Vtable.T> vtables,
                                List<Struct.T> structs,
                                List<Function.T> functions) implements T {
        }

        public static void pp(T prog) {
            switch (prog) {
                case Singleton(
                        String entryFuncName, List<Vtable.T> vtables, List<Struct.T> structs, List<Function.T> functions
                ) -> {
                    printSpaces();
                    sayln("// x64 assembly generated by the Tiger compiler.");
                    printSpaces();
                    sayln(STR."// the entry function: \{entryFuncName}");
                    // vtables
                    for (X64.Vtable.T vtable : vtables) {
                        Vtable.pp(vtable);
                    }
                    // structs
                    for (Struct.T struct : structs) {
                        Struct.pp(struct);
                    }
                    // functions:
                    for (Function.T func : functions) {
                        Function.pp(func);
                    }
                }
            }
        }
    }
}
