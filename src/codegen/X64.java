package codegen;

import util.Id;
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
                "rbx",
                //"rbp", // we reserve rbp as the stack base pointer
                "r12",
                "r13",
                "r14",
                "r15");

        // caller-saved regs
        public static List<String> callerSavedRegs = List.of(
                "rax",
                "rcx",
                "rdx",
                "rdi",
                "rsi",
                //"rsp", // we reserve rsp as the stack top pointer
                "r8",
                "r9",
                "r10",
                "r11");

        // we used these two registers for stack-based allocation
        public static String callerR10 = "r10";
        public static String callerR11 = "r11";
    }


    //  ///////////////////////////////////////////////////////////
    //  type
    public static class Type {
        public sealed interface T permits
                ClassType,
                Int,
                IntArray,
                CodePtr {
        }

        public record Int() implements T {
        }

        public record ClassType(Id id) implements T {
        }

        public record IntArray() implements T {
        }

        // a pointer to code
        public record CodePtr() implements T {
        }

        public static void pp(T ty) {
            switch (ty) {
                case Int() -> {
                    say("int");
                }
                case ClassType(Id id) -> {
                    say(id.toString());
                }
                case IntArray() -> {
                    say("int[]");
                }
                case CodePtr() -> {
                    say("CodePtr");
                }
            }
        }
    }

    // ///////////////////////////////////////////////////
    // declaration
    public static class Dec {
        public sealed interface T permits Singleton {
        }

        public record Singleton(Type.T type,
                                Id id) implements T {
        }

        public static void pp(T dec) {
            switch (dec) {
                case Singleton(Type.T type, Id id) -> {
                    Type.pp(type);
                    say(STR." \{id.toString()}");
                }
            }
        }

    }

    // /////////////////////////////////////////////////////////
    // virtual function table
    public static class Vtable {
        public sealed interface T permits Singleton {
        }

        public record Singleton(Id name,
                                List<String> funcs) implements T {
        }

        public static void pp(T vtable) {
            switch (vtable) {
                case Singleton(
                        Id name,
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
    // a virtual register may be a pseudo- or physical one.
    public static class VirtualReg {
        public sealed interface T
                permits Vid, Reg {
        }

        // variable
        public record Vid(Id id, Type.T ty) implements T {
            @Override
            public String toString() {
                return id.toString();
            }
        }

        // physical register
        public record Reg(String x, Type.T ty) implements T {
            @Override
            public String toString() {
                return x;
            }
        }

        public static void pp(T ty) {
            switch (ty) {
                case Vid(Id x, _) -> {
                    say(x.toString());
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
        // names should be alphabetically ordered
        public sealed interface T permits
                Bop,
                CallDirect,
                CallIndirect,
                Comment,
                Load,
                Move,
                MoveConst,
                Store {
        }


        // binary opertions
        public record Bop(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                          List<VirtualReg.T> uses,
                          List<VirtualReg.T> defs) implements T {
        }

        // call direct
        public record CallDirect(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                 List<VirtualReg.T> uses,
                                 List<VirtualReg.T> defs) implements T {
        }

        // call indirect, that is, the function address is in a register
        public record CallIndirect(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                   List<VirtualReg.T> uses,
                                   List<VirtualReg.T> defs) implements T {
        }

        // comment, for debugging purpose
        public record Comment(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                              List<VirtualReg.T> uses,
                              List<VirtualReg.T> defs) implements T {
        }

        // load memory content into registers
        public record Load(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                           List<VirtualReg.T> uses,
                           List<VirtualReg.T> defs) implements T {
        }

        // move between registers
        public record Move(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                           List<VirtualReg.T> uses,
                           List<VirtualReg.T> defs) implements T {
        }

        // move constants into registers
        public record MoveConst(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
                                List<VirtualReg.T> uses,
                                List<VirtualReg.T> defs) implements T {
        }

        // store into memory address
        public record Store(BiFunction<List<VirtualReg.T>, List<VirtualReg.T>, String> instr,
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
                    printInstrBody(instrFn, uses, defs);
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
                case Store(
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
    // end of instruction


    // /////////////////////////////////////////////////////////
    // transfer
    public static class Transfer {
        public sealed interface T permits If, Jmp, Ret {
        }

        public record If(String instr, Block.T trueBlock, Block.T falseBlock)
                implements T {
        }

        public record Jmp(Block.T target) implements T {
        }

        public record Ret() implements T {
        }

        public static void pp(T t) {
            switch (t) {
                case If(String instr, Block.T thenn, Block.T elsee) -> {
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

        public static void addInstrsFirst(Block.T b, List<Instr.T> ins) {
            switch (b) {
                case Singleton(_, List<Instr.T> instrs, _) -> {
                    for (Instr.T t : ins.reversed()) {
                        instrs.addFirst(t);
                    }
                }
            }
        }

        public static void addInstrsLast(Block.T b, List<Instr.T> ins) {
            switch (b) {
                case Singleton(_, List<Instr.T> instrs, _) -> {
                    for (Instr.T t : ins) {
                        instrs.addLast(t);
                    }
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
                    say(STR."""
\{label.toString()}:
""");
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
                                Id classId,
                                Id methodId,
                                List<Dec.T> formals,
                                List<Dec.T> locals,
                                List<Block.T> blocks) implements T {
        }

        public static Block.T getBlock(T func, Label label) {
            switch (func) {
                case Singleton(
                        Type.T retType,
                        Id classId,
                        Id methodId,
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
                        Type.T retType, Id classId,
                        Id methodId, List<Dec.T> formals, List<Dec.T> locals, List<Block.T> blocks
                ) -> {
                    printSpaces();
                    Type.pp(retType);
                    say(STR." \{classId}_\{methodId}(");
                    for (Dec.T dec : formals) {
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
    }// end of function

    // whole program
    public static class Program {
        public sealed interface T permits Singleton {
        }

        public record Singleton(Id entryFuncName, // name of the entry function
                                List<Vtable.T> vtables,
                                List<Struct.T> structs,
                                List<Function.T> functions) implements T {
        }

        public static void pp(T prog) {
            switch (prog) {
                case Singleton(
                        Id entryFuncName, List<Vtable.T> vtables, List<Struct.T> structs, List<Function.T> functions
                ) -> {
                    printSpaces();
                    sayln("// x64 assembly generated by the Tiger compiler.");
                    printSpaces();
                    sayln(STR."// the entry function: \{entryFuncName.toString()}");
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
    }// end of programs
}
