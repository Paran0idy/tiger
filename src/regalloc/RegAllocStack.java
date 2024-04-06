package regalloc;

import cfg.Cfg;
import codegen.X64;
import util.Label;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// a register allocator to allocate each virtual register to a physical one,
// using a stack-based approach.
class RegAllocStack {

    // data structures to hold new instructions in a block
    TempMap tempMap;
    List<X64.Instr.T> newInstrs;
    // we use two caller-saved registers for load/store variables
    List<String> tempRegs = List.of("r10", "r11");


    public X64.Type.T munchType(Cfg.Type.T type) {
        switch (type) {
            case Cfg.Type.ClassType(String id) -> {
                return new X64.Type.ClassType(id);
            }
            case Cfg.Type.Int() -> {
                return new X64.Type.Int();
            }
            case Cfg.Type.IntArray() -> {
                return new X64.Type.IntArray();
            }
            case Cfg.Type.Ptr() -> {
                return new X64.Type.Ptr();
            }
        }
    }

    public X64.Dec.T munchDec(Cfg.Dec.T dec) {
        switch (dec) {
            case Cfg.Dec.Singleton(Cfg.Type.T type, String id) -> {
                return new X64.Dec.Singleton(munchType(type), id);
            }
        }
    }

    // generate a move instruction
    public void genMove(String dest,
                        Cfg.Value.T value,
                        X64.Type.T targetType, // type of the "dest"
                        List<X64.Instr.T> instrs) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, targetType));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Id(String y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Id(y, munchType(ty)));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, targetType));
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
    public void genMoveAddr(String dest, String label, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.PtrCode()));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."leaq\t$\{label}, %\{darg.getFirst()}",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a move instruction
    public void genMoveFromReg(String dest,
                               String physicalReg,
                               X64.Type.T srcType,
                               List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(physicalReg, srcType));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, srcType));
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
            case Cfg.Value.Id(String y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Id(y, new X64.Type.Int()));
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
    public void genLoad(String dest, String src, int offset, X64.Type.T ty) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(src, ty));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, ty));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(%\{uarg.getFirst()}), %\{darg.get(0)}",
                uses,
                defs);
        this.newInstrs.add(instr);
    }

    // generate a store instruction
    public void genStore(String reg, String baseReg, int offset, X64.Type.T ty, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(baseReg, ty),
                new X64.VirtualReg.Reg(reg, ty));
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.Store(
                (uarg, darg) ->
                        STR."movq\t%\{uarg.get(1)}, \{offset}(%\{uarg.get(0)})",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a binary instruction
    public void genBop(String dest,
                       Cfg.Value.T value,
                       String bop,
                       // whether the value will be assigned to "dest"
                       boolean assigned,
                       List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses, defs;
        if (assigned)
            defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
        else
            defs = List.of();

        switch (value) {
            case Cfg.Value.Int(int n) -> {
                uses = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.Bop(
                        (uarg, darg) ->
                                STR."\{bop}\t$\{n}, %\{uarg.getFirst()}",
                        uses,
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Id(String y, Cfg.Type.T ty) -> {
                uses = List.of(new X64.VirtualReg.Id(dest, munchType(ty)),
                        new X64.VirtualReg.Id(y, new X64.Type.Int()));
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

    public List<X64.VirtualReg.T> mapVirtualRegs(List<X64.VirtualReg.T> virtualRegs,
                                                 HashMap<String, String> allocated) {
        int i = 0;
        List<X64.VirtualReg.T> newRegs = new LinkedList<>();
        for (X64.VirtualReg.T vr : virtualRegs) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                    newRegs.add(vr);
                }
                case X64.VirtualReg.Id(String x, X64.Type.T ty) -> {
                    // get the position
                    TempMap.Position.T pos = this.tempMap.get(x);
                    switch (pos) {
                        case TempMap.Position.InReg(String reg) -> {
                            throw new AssertionError(reg);
                        }
                        case TempMap.Position.InStack(int offset) -> {
                            String r1 = this.tempRegs.get(i++);
                            newRegs.add(new X64.VirtualReg.Reg(r1, ty));
                            allocated.put(x, r1);
                            // generate load instructions to load the uses
                            genLoad(r1, "rbp", ((TempMap.Position.InStack) pos).offset(), ty);
                        }
                    }
                }
            }
        }
        return newRegs;
    }

    public List<X64.VirtualReg.T> mapVirtualDefRegs
            (List<X64.VirtualReg.T> virtualRegs,
             HashMap<String, String> allocated,
             List<X64.Instr.T> outInstrs) {
        int i = 0;
        List<X64.VirtualReg.T> newRegs = new LinkedList<>();
        for (X64.VirtualReg.T vr : virtualRegs) {
            switch (vr) {
                case X64.VirtualReg.Reg(_, _) -> {
                    newRegs.add(vr);
                }
                case X64.VirtualReg.Id(String x, X64.Type.T ty) -> {
                    // get the position
                    TempMap.Position.T pos = this.tempMap.get(x);
                    if (pos == null)
                        throw new AssertionError(x);
                    String allocReg = allocated.getOrDefault(x, null);
                    if (allocReg == null) {
                        allocReg = this.tempRegs.get(i++);
                    } else {
                    }
                    newRegs.add(new X64.VirtualReg.Reg(allocReg, ty));
                    // generate store instructions to store the defs
                    genStore(allocReg, "rbp", ((TempMap.Position.InStack) pos).offset(), ty, outInstrs);
                }
            }
        }
        return newRegs;
    }

    public void allocInstr(X64.Instr.T s) {
        switch (s) {
            case X64.Instr.Bop(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                // we track the variables that allocated in the uses
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.Bop(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);

            }
            case X64.Instr.CallDirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>,
                            List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.CallDirect(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.CallIndirect(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.CallIndirect(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.Comment(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.Comment(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.Load(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.Load(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.Move(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.Move(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.MoveConst(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.MoveConst(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
            case X64.Instr.Store(
                    java.util.function.BiFunction<List<X64.VirtualReg.T>, List<X64.VirtualReg.T>, String> instr,
                    List<X64.VirtualReg.T> uses,
                    List<X64.VirtualReg.T> defs
            ) -> {
                HashMap<String, String> allocated = new HashMap<>();
                var newUses = mapVirtualRegs(uses, allocated);
                var localInstrs = new LinkedList<X64.Instr.T>();
                var newDefs = mapVirtualDefRegs(defs, allocated, localInstrs);
                var newInstr = instr.apply(newUses, newDefs);
                this.newInstrs.add(new X64.Instr.Store(
                        (uarg, darg) -> newInstr,
                        newUses,
                        newDefs));
                this.newInstrs.addAll(localInstrs);
            }
        }
    }

    public void munchTransfer(Cfg.Transfer.T transfer,
                              List<X64.Transfer.T> newTransfer,
                              X64.Function.T newFunc,
                              List<X64.Instr.T> instrs) {
        switch (transfer) {
            case Cfg.Transfer.Jmp(Cfg.Block.T target) -> {
                Label label = Cfg.Block.getLabel(target);
                X64.Block.T newBlock = X64.Function.getBlock(newFunc, label);
                newTransfer.add(new X64.Transfer.Jmp(newBlock));
            }
            case Cfg.Transfer.If(
                    Cfg.Value.T value,
                    Cfg.Block.T trueBlock,
                    Cfg.Block.T falseBlocks
            ) -> {
                Label trueLabel = Cfg.Block.getLabel(trueBlock);
                Label falseLabel = Cfg.Block.getLabel(falseBlocks);
                X64.Block.T newTrueBlock = X64.Function.getBlock(newFunc, trueLabel);
                X64.Block.T newFalseBlock = X64.Function.getBlock(newFunc, falseLabel);
                switch (value) {
                    case Cfg.Value.Id(String id, _) -> {
                        genBop(id, new Cfg.Value.Int(1), "test", false, instrs);
                        newTransfer.add(new X64.Transfer.If("je", newTrueBlock, newFalseBlock));
                    }
                    case Cfg.Value.Int(_) -> {
                        throw new AssertionError();
                    }
                }
            }
            case Cfg.Transfer.Ret(Cfg.Value.T retValue) -> {
                String retReg = X64.Register.retReg;
                genMoveToReg(retReg, retValue, instrs);
                newTransfer.add(new X64.Transfer.Ret());
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
                this.newInstrs = new LinkedList<>();
                for (X64.Instr.T s : instrs) {
                    allocInstr(s);
                }
                return new X64.Block.Singleton(label,
                        this.newInstrs,
                        transfer);
            }
        }
    }

    // to implement the calling convention: argument passing
    public void callingConvention(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    X64.Type.T retType,
                    String id,
                    List<X64.Dec.T> formals,
                    List<X64.Dec.T> locals,
                    List<X64.Block.T> blocks
            ) -> {
                X64.Block.T newEntryBlock = new X64.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());
                switch (newEntryBlock) {
                    case X64.Block.Singleton(
                            Label _,
                            List<X64.Instr.T> instrs,
                            List<X64.Transfer.T> transfer
                    ) -> {
                        transfer.add(new X64.Transfer.Jmp(blocks.getFirst()));
                        blocks.addFirst(newEntryBlock);
                        // to move arguments:
                        int index = 0;
                        for (X64.Dec.T formal : formals) {
                            switch (formal) {
                                case X64.Dec.Singleton(
                                        X64.Type.T type,
                                        String id1
                                ) -> {
                                    genMoveFromReg(id1,
                                            X64.Register.argPassingRegs.get(index),
                                            type,
                                            instrs);
                                }
                            }
                            index++;
                        }
                    }
                }
            }
        }
    }

    public X64.Function.T allocFunction(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    X64.Type.T retType,
                    String id,
                    List<X64.Dec.T> formals,
                    List<X64.Dec.T> locals,
                    List<X64.Block.T> blocks
            ) -> {
                Frame frame = new Frame(id);
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
                        id,
                        formals,
                        locals,
                        blocks.stream().map(this::allocBlock).collect(Collectors.toList()));
            }
        }
    }

    public X64.Program.T allocProgram(X64.Program.T x64) {
        switch (x64) {
            case X64.Program.Singleton(
                    String entryFuncName,
                    List<X64.Vtable.T> vtables,
                    List<X64.Struct.T> structs,
                    List<X64.Function.T> functions
            ) -> {
                return new X64.Program.Singleton(
                        entryFuncName,
                        vtables,
                        structs,
                        functions.stream().map(this::allocFunction).collect(Collectors.toList()));
            }
        }
    }
}