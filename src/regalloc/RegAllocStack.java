package regalloc;

import cfg.Cfg;
import codegen.X64;
import util.Id;
import util.Label;
import util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// a register allocator to allocate each virtual register to a physical one,
// using a stack-based approach.
public class RegAllocStack {

    // data structures to hold new instructions in a block
    TempMap tempMap;
    List<X64.Instr.T> newInstrs;

    // we use two caller-saved registers for load/store variables
    public static class TempRegs {
        private static int counter = 0;
        private static final List<String> tempRegs = List.of("r10", "r11");

        public static String next() {
            return tempRegs.get(counter++);
        }

        public static void reset() {
            counter = 0;
        }
    }


    public X64.Type.T munchType(Cfg.Type.T type) {
        switch (type) {
            case Cfg.Type.ClassType(Id id) -> {
                return new X64.Type.ClassType(id);
            }
            case Cfg.Type.Int() -> {
                return new X64.Type.Int();
            }
            case Cfg.Type.IntArray() -> {
                return new X64.Type.IntArray();
            }
            case Cfg.Type.CodePtr() -> {
                return new X64.Type.CodePtr();
            }
        }
    }

    public X64.Dec.T munchDec(Cfg.Dec.T dec) {
        switch (dec) {
            case Cfg.Dec.Singleton(Cfg.Type.T type, Id id) -> {
                return new X64.Dec.Singleton(munchType(type), id);
            }
        }
    }

    // generate a move instruction
    public void genMove(Id dest,
                        Cfg.Value.T value,
                        X64.Type.T targetType, // type of the "dest"
                        List<X64.Instr.T> instrs) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(y, munchType(ty)));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                        uses,
                        defs);
                instrs.add(instr);
            }
        }
    }

    // generate a move instruction
    public void genMoveAddr(Id dest, Id label, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.CodePtr()));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."leaq\t$\{label}, %\{darg.getFirst()}",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a move instruction
    public void genMoveFromReg(Id dest,
                               String physicalReg,
                               X64.Type.T srcType,
                               List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(physicalReg, srcType));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, srcType));
        X64.Instr.T instr = new X64.Instr.Move(
                (uarg, darg) ->
                        STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                uses,
                defs);
        instrs.add(instr);
    }

    public void genMoveToReg(String physicalReg, Cfg.Value.T value, List<X64.Instr.T> instrs) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(physicalReg, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(y, new X64.Type.Int()));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(physicalReg, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                        uses,
                        defs);
                instrs.add(instr);
            }
        }
    }

    // generate a load instruction
    public void genLoadToReg(String destReg, String srcReg, int offset, X64.Type.T ty) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(srcReg, ty));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(destReg, ty));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(%\{uarg.getFirst()}), %\{darg.get(0)}",
                uses,
                defs);
        this.newInstrs.add(instr);
    }

    // generate a store instruction
    public void genStore(String reg, String baseReg, int offset, X64.Type.T ty) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(baseReg, ty),
                new X64.VirtualReg.Reg(reg, ty));
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.Store(
                (uarg, darg) ->
                        STR."movq\t%\{uarg.get(1)}, \{offset}(%\{uarg.get(0)})",
                uses,
                defs);
        this.newInstrs.add(instr);
    }

    // generate a binary instruction
    public void genBop(Id dest,
                       Cfg.Value.T value,
                       String bop,
                       // whether the value will be assigned to "dest"
                       boolean assigned,
                       List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses, defs;
        if (assigned)
            defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
        else
            defs = List.of();

        switch (value) {
            case Cfg.Value.Int(int n) -> {
                uses = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.Bop(
                        (uarg, darg) ->
                                STR."\{bop}\t$\{n}, %\{uarg.getFirst()}",
                        uses,
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                uses = List.of(new X64.VirtualReg.Vid(dest, munchType(ty)),
                        new X64.VirtualReg.Vid(y, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.Bop(
                        (uarg, darg) ->
                                STR."\{bop}\t%\{uarg.get(1)}, %\{uarg.get(0)}",
                        uses,
                        defs);
                instrs.add(instr);
            }
        }
    }

    // generate an indirect call
    public void genCallIndirect(String fname, List<X64.Instr.T> instrs) {
        // this should be fixed...
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.CallDirect(
                (uarg, darg) ->
                        STR."call\t*\{fname}",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a direct call
    public void genCallDirect(String fname, List<X64.Instr.T> instrs) {
        // this should be fixed...
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.CallDirect(
                (uarg, darg) ->
                        STR."call\t\{fname}",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a comment
    public void genComment(String comment, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.Comment(
                (uarg, darg) ->
                        STR."//\{comment}",
                uses,
                defs);
        instrs.add(instr);
    }

    // return the new regs, along with v
    public Pair<List<X64.VirtualReg.T>, HashMap<String, TempMap.Position.T>> mapVirtualRegs
    (List<X64.VirtualReg.T> virtualRegs) {
        TempRegs.reset();
        List<X64.VirtualReg.T> newRegs = new LinkedList<>();
        // the order is not important, so we can use a map instead of a list
        HashMap<String, TempMap.Position.T> map = new HashMap<>();

        for (X64.VirtualReg.T vr : virtualRegs) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                    newRegs.add(vr);
                }
                case X64.VirtualReg.Vid(Id x, X64.Type.T ty) -> {
                    // get the position
                    TempMap.Position.T pos = this.tempMap.get(x);
                    switch (pos) {
                        case TempMap.Position.InReg(String reg) -> {
                            throw new AssertionError(reg);
                        }
                        case TempMap.Position.InStack(int offset) -> {
                            String r1 = TempRegs.next();
                            newRegs.add(new X64.VirtualReg.Reg(r1, ty));
                            map.put(r1, pos);
                        }
                    }
                }
            }
        }
        return new Pair<>(newRegs, map);
    }

    public void allocInstr(X64.Instr.T s) {
        switch (s) {
            case X64.Instr.Bop(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.CallDirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>,
                            List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.CallIndirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.Comment(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.Load(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.Move(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.MoveConst(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
            case X64.Instr.Store(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<String, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), "rbp",
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
            }
        }
    }

    public X64.Block.T allocBlock(X64.Block.T block) {
        switch (block) {
            case X64.Block.Singleton(
                    Label label,
                    List<X64.Instr.T> instrs,
                    List<X64.Transfer.T> transfer
            ) -> {
                // results will be appended to this list
                this.newInstrs = new LinkedList<>();
                instrs.forEach(this::allocInstr);
                return new X64.Block.Singleton(label,
                        this.newInstrs,
                        transfer);
            }
        }
    }

    public X64.Function.T allocFunction(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    X64.Type.T retType,
                    Id classId,
                    Id functionId,
                    List<X64.Dec.T> formals,
                    List<X64.Dec.T> locals,
                    List<X64.Block.T> blocks
            ) -> {
                Frame frame = new Frame(STR."\{classId}_\{functionId}");
                this.tempMap = new TempMap();
                // allocate spaces for formals
                for (X64.Dec.T formal : formals) {
                    int offset = frame.alloc();
                    tempMap.put(((X64.Dec.Singleton) formal).id(), new TempMap.Position.InStack(offset));
                }
                // allocate spaces for locals
                for (X64.Dec.T local : locals) {
                    int offset = frame.alloc();
                    tempMap.put(((X64.Dec.Singleton) local).id(), new TempMap.Position.InStack(offset));
                }
                // generate prolog
                int totalSize = frame.size();
                X64.Block.T entryBlock = blocks.getFirst();
                List<X64.Instr.T> prolog = List.of(
                        new X64.Instr.Move(
                                (uarg, darg) -> {
                                    return "pushq\t%rbp";
                                },
                                List.of(), // this does not matter
                                List.of()
                        ),
                        new X64.Instr.Move(
                                (uarg, darg) -> {
                                    return "movq\t%rsp, %rbp";
                                },
                                List.of(), // this does not matter
                                List.of()
                        ),
                        new X64.Instr.Bop(
                                (uarg, darg) -> {
                                    return STR."subq\t$\{totalSize}, %rsp";
                                },
                                List.of(), // this does not matter
                                List.of()
                        )
                );
                X64.Block.addInstrsFirst(entryBlock, prolog);
                // epilogue
                X64.Block.T exitBlock = blocks.getLast();
                List<X64.Instr.T> epilogue = List.of(
                        new X64.Instr.Move(
                                (uarg, darg) -> {
                                    return "leave";
                                },
                                List.of(), // this does not matter
                                List.of()
                        )
                );
                X64.Block.addInstrsLast(exitBlock, epilogue);

                return new X64.Function.Singleton(
                        retType,
                        classId,
                        functionId,
                        formals,
                        locals,
                        blocks.stream().map(this::allocBlock).collect(Collectors.toList()));
            }
        }
    }

    public X64.Program.T allocProgram(X64.Program.T x64) {
        switch (x64) {
            case X64.Program.Singleton(
                    Id entryClassName,
                    Id entryFuncName,
                    List<X64.Vtable.T> vtables,
                    List<X64.Struct.T> structs,
                    List<X64.Function.T> functions
            ) -> {
                return new X64.Program.Singleton(
                        entryClassName,
                        entryFuncName,
                        vtables,
                        structs,
                        functions.stream().map(this::allocFunction).collect(Collectors.toList()));
            }
        }
    }
}