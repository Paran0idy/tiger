package regalloc;

import codegen.X64;
import control.Control;
import util.Id;
import util.Label;
import util.Trace;
import util.Tuple;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// a register allocator to allocate each virtual register to a physical one,
// using a stack-based approach.
public class RegAllocStack {
    private final Id frameBaseReg = Id.newName("%rbp");
    // data structures to hold new instructions in a block
    TempMap tempMap;
    List<X64.Instr.T> newInstrs;

    public static class AllocReg {
        private int counter = 0;
        // map
        private final HashMap<Id, Id> map;

        private final List<Id> tempRegs;

        AllocReg() {
            this.map = new HashMap<>();
            // we reserve two caller-saved registers for variable load/store
            this.tempRegs =
                    List.of(Id.newName("%r10"),
                            Id.newName("%r11"));
        }

        private void allocOne(List<X64.VirtualReg.T> one) {
            one.forEach((X64.VirtualReg.T vr) -> {
                switch (vr) {
                    case X64.VirtualReg.Reg(_, _) -> {
                        // does not matter
                    }
                    case X64.VirtualReg.Vid(Id x, _) -> {
                        if (!this.map.containsKey(x)) {
                            this.map.put(x, tempRegs.get(counter++));
                        }
                    }
                }
            });
        }

        private X64.VirtualReg.T mapOne(X64.VirtualReg.T one) {
            switch (one) {
                case X64.VirtualReg.Reg(_, _) -> {
                    return one;
                }
                case X64.VirtualReg.Vid(Id x, X64.Type.T type) -> {
                    Id reg = this.map.get(x);
                    return new X64.VirtualReg.Reg(reg, type);
                }
            }
        }

        public Tuple.Two<List<X64.VirtualReg.T>,
                List<X64.VirtualReg.T>> allocUseDef(List<X64.VirtualReg.T> uses,
                                                    List<X64.VirtualReg.T> defs) {
            uses.forEach((X64.VirtualReg.T vr) -> {
                switch (vr) {
                    case X64.VirtualReg.Reg(_, _) -> {
                        // does not matter
                    }
                    case X64.VirtualReg.Vid(Id x, _) -> {
                        this.map.put(x, tempRegs.get(counter++));
                    }
                }
            });
            defs.forEach((X64.VirtualReg.T vr) -> {
                switch (vr) {
                    case X64.VirtualReg.Reg(_, _) -> {
                        // does not matter
                    }
                    case X64.VirtualReg.Vid(Id x, _) -> {
                        // reset the counter
                        this.counter = 0;
                        if (!this.map.containsKey(x))
                            this.map.put(x, tempRegs.get(counter++));
                    }
                }
            });
            var newUses = uses.stream().map(this::mapOne).toList();
            var newDefs = defs.stream().map(this::mapOne).toList();
            return new Tuple.Two<>(newUses, newDefs);
        }

        public Id getReg(Id x) {
            return this.map.get(x);
        }
    }

    public void genLoadToReg(List<X64.VirtualReg.T> uses,
                             AllocReg allocReg) {
        for (X64.VirtualReg.T vr : uses) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                }
                case X64.VirtualReg.Vid(Id x, _) -> {
                    Id reg = allocReg.getReg(x);
                    int offset = this.tempMap.getOffset(x);
                    List<X64.VirtualReg.T> newUses, newDefs;
                    newUses = List.of(new X64.VirtualReg.Reg(frameBaseReg, new X64.Type.Int()));
                    newDefs = List.of(new X64.VirtualReg.Reg(reg, new X64.Type.Int()));
                    X64.Instr.T instr = new X64.Instr.Load(
                            (uarg, darg) ->
                                    STR."movq\t\{offset}(\{uarg.getFirst()}), \{darg.get(0)}",
                            newUses,
                            newDefs);

                    this.newInstrs.add(instr);
                }
            }
        }
    }

    // generate a store instruction
    public void genStore(List<X64.VirtualReg.T> defs, AllocReg allocReg) {
        for (X64.VirtualReg.T vr : defs) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                }
                case X64.VirtualReg.Vid(Id x, _) -> {
                    Id reg = allocReg.getReg(x);
                    int offset = this.tempMap.getOffset(x);
                    List<X64.VirtualReg.T> newUses, newDefs;
                    newUses = List.of(new X64.VirtualReg.Reg(frameBaseReg, new X64.Type.Int()));
                    newDefs = List.of(new X64.VirtualReg.Reg(reg, new X64.Type.Int()));
                    X64.Instr.T instr = new X64.Instr.Load(
                            (uarg, darg) ->
                                    STR."movq\t\{darg.get(0)}, \{offset}(\{uarg.getFirst()})",
                            newUses,
                            newDefs);

                    this.newInstrs.add(instr);
                }
            }
        }
    }

    public void allocInstr(X64.Instr.T s) {
        switch (s) {
            case X64.Instr.Bop(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.CallDirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>,
                            List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.CallIndirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.Comment(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.Load(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.Move(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.MoveConst(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
            }
            case X64.Instr.Store(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                AllocReg allocReg = new AllocReg();
                var newUseDefs = allocReg.allocUseDef(uses, defs);
                // generate load instructions to load the uses
                genLoadToReg(uses, allocReg);
                this.newInstrs.add(new X64.Instr.Bop(
                        instr,
                        newUseDefs.first(),
                        newUseDefs.second()));
                // generate store instructions to store the defs
                genStore(defs, allocReg);
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
                // allocate stack frame spaces for formals
                for (X64.Dec.T formal : formals) {
                    int offset = frame.alloc();
                    tempMap.put(((X64.Dec.Singleton) formal).id(), new TempMap.Position.InStack(offset));
                }
                // allocate stack frame spaces for locals
                for (X64.Dec.T local : locals) {
                    int offset = frame.alloc();
                    tempMap.put(((X64.Dec.Singleton) local).id(), new TempMap.Position.InStack(offset));
                }
                // generate prolog
                int totalSize = frame.size();
                X64.Block.T entryBlock = blocks.getFirst();
                List<X64.Instr.T> prolog = List.of(
                        new X64.Instr.Move(
                                (uarg, darg) -> "pushq\t%rbp",
                                List.of(), // this does not matter
                                List.of()
                        ),
                        new X64.Instr.Move(
                                (uarg, darg) -> "movq\t%rsp, %rbp",
                                List.of(), // this does not matter
                                List.of()
                        ),
                        new X64.Instr.Bop(
                                (uarg, darg) -> STR."subq\t$\{totalSize}, %rsp",
                                List.of(), // this does not matter
                                List.of()
                        )
                );
                X64.Block.addInstrsFirst(entryBlock, prolog);
                // epilogue
                X64.Block.T exitBlock = blocks.getLast();
                List<X64.Instr.T> epilogue = List.of(
                        new X64.Instr.Move(
                                (uarg, darg) -> "leave",
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
        // this should not be controlled by trace
        if (Control.X64.assemFile != null) {
            new PpAssem().ppProgram(result);
        }
        return result;
    }

}