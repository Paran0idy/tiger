package regalloc;

import codegen.X64;
import control.Control;
import util.Error;
import util.Id;
import util.Label;
import util.Trace;
import util.Tuple.Two;

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
        private static final List<Id> tempRegs = List.of(Id.newName("%r10"),
                Id.newName("%r11"));

        public static Id next() {
            return tempRegs.get(counter++);
        }

        public static void reset() {
            counter = 0;
        }
    }

    // generate a load instruction
    public void genLoadToReg(Id destReg, Id srcReg, int offset, X64.Type.T ty) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(srcReg, ty));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(destReg, ty));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(\{uarg.getFirst()}), \{darg.get(0)}",
                uses,
                defs);
        this.newInstrs.add(instr);
    }

    // generate a store instruction
    public void genStore(Id reg, Id baseReg, int offset, X64.Type.T ty) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(baseReg, ty),
                new X64.VirtualReg.Reg(reg, ty));
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.Store(
                (uarg, darg) ->
                        STR."movq\t\{uarg.get(1)}, \{offset}(\{uarg.get(0)})",
                uses,
                defs);
        this.newInstrs.add(instr);
    }

    // return the new regs, along with v
    public Two<List<X64.VirtualReg.T>, HashMap<Id, TempMap.Position.T>> mapVirtualRegs
    (List<X64.VirtualReg.T> virtualRegs) {
        TempRegs.reset();
        List<X64.VirtualReg.T> newRegs = new LinkedList<>();
        // the order is not important, so we can use a map instead of a list
        HashMap<Id, TempMap.Position.T> map = new HashMap<>();

        for (X64.VirtualReg.T vr : virtualRegs) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                    newRegs.add(vr);
                }
                case X64.VirtualReg.Vid(Id x, X64.Type.T ty) -> {
                    // get the position
                    TempMap.Position.T pos = this.tempMap.get(x);
                    switch (pos) {
                        case TempMap.Position.InReg(Id reg) -> {
                            throw new Error(reg);
                        }
                        case TempMap.Position.InStack(int offset) -> {
                            Id r1 = TempRegs.next();
                            newRegs.add(new X64.VirtualReg.Reg(r1, ty));
                            map.put(r1, pos);
                        }
                    }
                }
            }
        }
        return new Two<>(newRegs, map);
    }

    public void allocInstr(X64.Instr.T s) {
        Id frameBaseReg = Id.newName("%rbp");

        switch (s) {
            case X64.Instr.Bop(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                var newUsesAndMap = mapVirtualRegs(uses);
                var newDefsAndMap = mapVirtualRegs(defs);
                // generate load instructions to load the uses
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newUsesAndMap.second().entrySet())) {
                    genLoadToReg(entry.getKey(), frameBaseReg,
                            ((TempMap.Position.InStack) entry.getValue()).offset(), new X64.Type.Int());
                }
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUsesAndMap.first(),
                        newDefsAndMap.first()));
                // generate store instructions to store the defs
                for (HashMap.Entry<Id, TempMap.Position.T> entry : (newDefsAndMap.second().entrySet())) {
                    genStore(entry.getKey(), frameBaseReg,
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

    private X64.Program.T allocProgram0(X64.Program.T x64) {
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

    public X64.Program.T allocProgram(X64.Program.T x64) {
        Trace<X64.Program.T, X64.Program.T> trace =
                new Trace<>("codegen.Munch.munchProgram",
                        this::allocProgram0,
                        x64,
                        X64.Program::pp,
                        X64.Program::pp);
        X64.Program.T result = trace.doit();
        if (Control.X64.assemFile != null) {
            new PpAssem().ppProgram(result);
        }
        return result;
    }

}