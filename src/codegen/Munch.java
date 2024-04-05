package codegen;

import cfg.Cfg;
import util.Label;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class Munch {

    // data structures to hold the layout information
    Layout layouts;


    public X64.Type.T munchType(Cfg.Type.T type) {
        return new X64.Type.Int();
    }

    public X64.Dec.T munchDec(Cfg.Dec.T dec) {
        switch (dec) {
            case Cfg.Dec.Singleton(Cfg.Type.T type, String id) -> {
                return new X64.Dec.Singleton(munchType(type), id);
            }
        }
    }

    // generate a move instruction
    public void genMove(String dest, Cfg.Value.T value, List<X64.Instr.T> instrs) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                instrs.add(instr);
            }
            case Cfg.Value.Id(String y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Id(y, new X64.Type.Int()));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
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
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."leaq\t$\{label}, %\{darg.getFirst()}",
                uses,
                defs);
        instrs.add(instr);
    }

    // generate a move instruction
    public void genMoveFromReg(String dest, String physicalReg, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(physicalReg, new X64.Type.Int()));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
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
    public void genLoad(String dest, String src, int offset, List<X64.Instr.T> instrs) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Id(src, new X64.Type.Int()));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(%\{uarg.getFirst()}), %\{darg.getFirst()}",
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
                uses = List.of(new X64.VirtualReg.Id(dest, new X64.Type.Int()),
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


    public void munchStm(Cfg.Stm.T s, List<X64.Instr.T> instrs) {
        switch (s) {
            case Cfg.Stm.Assign(String id, Cfg.Value.T value, Cfg.Type.T type) -> {
                genMove(id, value, instrs);
            }
            case Cfg.Stm.AssignBop(
                    String id, Cfg.Value.T left,
                    String bop, Cfg.Value.T right, Cfg.Type.T type
            ) -> {
                switch (bop) {
                    case "+" -> {
                        genMove(id, left, instrs);
                        genBop(id, right, "addq", true, instrs);
                    }
                    case "-" -> {
                        genMove(id, left, instrs);
                        genBop(id, right, "subq", true, instrs);
                    }
                    case "<" -> {
                        genMove(id, left, instrs);
                        genBop(id, right, "cmp", false, instrs);
                        List<X64.VirtualReg.T> uses = List.of();
                        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Id(id, new X64.Type.Int()));
                        X64.Instr.T instr = new X64.Instr.MoveConst((uarg, darg) ->
                                STR."setl\t%\{darg.getFirst()}",
                                uses,
                                defs);
                        instrs.add(instr);
                    }
                    default -> {
                        throw new AssertionError(bop);
                    }
                }
            }
            case Cfg.Stm.GetMethod(
                    String id,
                    Cfg.Value.T value,
                    Cfg.Type.T cls,
                    String methodName
            ) -> {
                // move the object into "id"
                genMove(id, value, instrs);
                // load the virtual method table pointer
                genLoad(id, id, 0, instrs);
                // this should be fixed
                int offset = this.layouts.methodOffset(cls, methodName);
                genLoad(id, id, offset, instrs);
            }
            case Cfg.Stm.AssignCall(
                    String id,
                    String func,
                    List<Cfg.Value.T> args,
                    Cfg.Type.T retType
            ) -> {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 5) {
                        throw new AssertionError("#arguments > 6");
                    }
                    String argReg = X64.Register.argPassingRegs.get(i);
                    genMove(argReg, args.get(i), instrs);
                }
                genCallIndirect(func, instrs);
                String retReg = X64.Register.retReg;
                genMoveFromReg(id, retReg, instrs);
            }
            case Cfg.Stm.AssignNew(String id, String cls) -> {
                // the 1st argument
                String argReg = X64.Register.argPassingRegs.getFirst();
                int sizeOfClass = this.layouts.classSize(cls);
                genMove(argReg, new Cfg.Value.Int(sizeOfClass), instrs);
                // the 2nd argument
                String argReg2 = X64.Register.argPassingRegs.get(1);
                String vtableName = ".V_" + cls;
                genMoveAddr(argReg2, vtableName, instrs);
                // the call
                genCallDirect("Tiger_new", instrs);
                String retReg = X64.Register.retReg;
                genMove(id, new Cfg.Value.Id(retReg, new Cfg.Type.Ptr()), instrs);
            }
            case Cfg.Stm.Print(Cfg.Value.T value) -> {
                String argReg = X64.Register.argPassingRegs.getFirst();
                genMove(argReg, value, instrs);
                genCallDirect("Tiger_print", instrs);
            }
            default -> {
                throw new AssertionError(s);
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

    // this will create empty block first
    public X64.Block.T munchBlock1(Cfg.Block.T block) {
        switch (block) {
            case Cfg.Block.Singleton(
                    Label label,
                    List<Cfg.Stm.T> stms,
                    List<Cfg.Transfer.T> transfer
            ) -> {
                return new X64.Block.Singleton(label,
                        new LinkedList<>(),
                        new LinkedList<>());
            }
        }
    }

    // this will create empty block first
    public void munchBlock2(List<Cfg.Block.T> cfgBlocks, X64.Function.T newFunc) {
        for (Cfg.Block.T cfgBlock : cfgBlocks) {
            switch (cfgBlock) {
                case Cfg.Block.Singleton(
                        Label label,
                        List<Cfg.Stm.T> stms,
                        List<Cfg.Transfer.T> transfer
                ) -> {
                    X64.Block.T x64Block = X64.Function.getBlock(newFunc, label);
                    switch (x64Block) {
                        case X64.Block.Singleton(
                                Label newLabel, List<X64.Instr.T> newStms, List<X64.Transfer.T> newTransfer
                        ) -> {
                            for (Cfg.Stm.T s : stms) {
                                munchStm(s, newStms);
                            }

                            munchTransfer(transfer.getFirst(), newTransfer, newFunc, newStms);
                        }
                    }
                }
            }
        }
    }

    public X64.Function.T munchFunction(Cfg.Function.T f) {
        switch (f) {
            case Cfg.Function.Singleton(
                    Cfg.Type.T retType,
                    String id,
                    List<Cfg.Dec.T> formals,
                    List<Cfg.Dec.T> locals,
                    List<Cfg.Block.T> blocks
            ) -> {
                // the 1st round will translate all statements but not transfer
                X64.Function.T newFunc = new X64.Function.Singleton(munchType(retType),
                        id,
                        formals.stream().map(this::munchDec).collect(Collectors.toList()),
                        locals.stream().map(this::munchDec).collect(Collectors.toList()),
                        blocks.stream().map(this::munchBlock1).collect(Collectors.toList()));
                munchBlock2(blocks, newFunc);
                return newFunc;
            }
        }
    }

    private String munchEntry(Cfg.Vtable.Entry entry) {
        return entry.clsName() + "_" + entry.funcName();
    }

    public X64.Vtable.T munchVtable(Cfg.Vtable.T f) {
        switch (f) {
            case Cfg.Vtable.Singleton(
                    String name,
                    List<Cfg.Vtable.Entry> funcTypes
            ) -> {

                return new X64.Vtable.Singleton(
                        name,
                        funcTypes.stream().map(this::munchEntry).collect(Collectors.toList()));
            }
        }
    }

    public X64.Program.T munchProgram(Cfg.Program.T cfg) {
        // determine the layout of class and virtual method table
        this.layouts = new Layout();
        layouts.layoutProgram(cfg);
        System.out.println(layouts);

        switch (cfg) {
            case Cfg.Program.Singleton(
                    String entryFuncName,
                    List<Cfg.Vtable.T> vtables,
                    List<Cfg.Struct.T> structs,
                    List<Cfg.Function.T> functions
            ) -> {
                return new X64.Program.Singleton(
                        entryFuncName,
                        vtables.stream().map(this::munchVtable).collect(Collectors.toList()),
                        new LinkedList<>(),
                        functions.stream().map(this::munchFunction).collect(Collectors.toList()));
            }
        }
    }
}