package codegen;

import cfg.Cfg;
import control.Control;
import util.Id;
import util.Label;
import util.Todo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Munch {
    // data structures to hold the layout information for class
    Layout layouts;
    // all parameters and locals in a function
    HashMap<Id, X64.Type.T> allVars;
    // current locals, we will append freshly generated locals into it
    List<X64.Dec.T> currentLocals;
    // points to the current instruction list in a x64 block
    private List<X64.Instr.T> currentInstrs;


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
                        X64.Type.T targetType // type of the "dest"
    ) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                this.currentInstrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(y, munchType(ty)));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                        uses,
                        defs);
                this.currentInstrs.add(instr);
            }
        }
    }

    // generate a move instruction
    public void genMoveAddrToReg(String dest, String label) {
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(dest, new X64.Type.CodePtr()));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."leaq\t\{label}(%rip), %\{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a move instruction
    public void genMoveFromReg(Id dest,
                               String physicalReg,
                               X64.Type.T srcType) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(physicalReg, srcType));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, srcType));
        X64.Instr.T instr = new X64.Instr.Move(
                (uarg, darg) ->
                        STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    public void genMoveToReg(String physicalReg, Cfg.Value.T value) {
        switch (value) {
            case Cfg.Value.Int(int n) -> {
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(physicalReg, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t$\{n}, %\{darg.getFirst()}",
                        new LinkedList<>(),
                        defs);
                this.currentInstrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(y, new X64.Type.Int()));
                List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(physicalReg, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.MoveConst(
                        (uarg, darg) ->
                                STR."movq\t%\{uarg.getFirst()}, %\{darg.getFirst()}",
                        uses,
                        defs);
                this.currentInstrs.add(instr);
            }
        }
    }

    // generate a load instruction
    public void genLoad(Id dest, Id src, int offset) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(src, new X64.Type.Int()));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(%\{uarg.getFirst()}), %\{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a binary instruction
    public void genBop(Id dest,
                       Cfg.Value.T value,
                       String bop) {
        List<X64.VirtualReg.T> uses, defs;
        defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));

        switch (value) {
            case Cfg.Value.Int(int n) -> {
                uses = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.Bop(
                        (uarg, darg) ->
                                STR."\{bop}\t$\{n}, %\{uarg.getFirst()}",
                        uses,
                        defs);
                this.currentInstrs.add(instr);
            }
            case Cfg.Value.Vid(Id y, Cfg.Type.T ty) -> {
                uses = List.of(new X64.VirtualReg.Vid(dest, munchType(ty)),
                        new X64.VirtualReg.Vid(y, new X64.Type.Int()));
                X64.Instr.T instr = new X64.Instr.Bop(
                        (uarg, darg) ->
                                STR."\{bop}\t%\{uarg.get(1)}, %\{uarg.get(0)}",
                        uses,
                        defs);
                this.currentInstrs.add(instr);
            }
        }
    }

    // generate an indirect call
    public void genCallIndirect(Id fname) {
        // this should be fixed
        // all caller-saved registers should be in the defs...
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Vid(fname, new X64.Type.CodePtr()));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg(X64.Register.retReg, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.CallIndirect(
                (uarg, darg) ->
                        STR."call\t*%\{uarg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a direct call
    public void genCallDirect(String fname) {
        // this should be fixed...
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.CallDirect(
                (uarg, darg) ->
                        STR."call\t\{fname}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a comment
    public void genComment(String comment) {
        List<X64.VirtualReg.T> uses = List.of();
        List<X64.VirtualReg.T> defs = List.of();
        X64.Instr.T instr = new X64.Instr.Comment(
                (uarg, darg) ->
                        STR."//\{comment}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    public void genCmp(Cfg.Value.T left, Cfg.Value.T right) {
        switch (left) {
            case Cfg.Value.Vid(Id x, Cfg.Type.T ty) -> {
                switch (right) {
                    case Cfg.Value.Vid(Id x1, Cfg.Type.T ty1) -> {
                        List<X64.VirtualReg.T> uses = List.of(
                                new X64.VirtualReg.Vid(x, new X64.Type.Int()),
                                new X64.VirtualReg.Vid(x1, new X64.Type.Int())
                        );
                        List<X64.VirtualReg.T> defs = List.of();
                        X64.Instr.T instr = new X64.Instr.Comment(
                                (uarg, darg) ->
                                        STR."cmpq\t%\{uarg.get(1)}, %\{uarg.get(0)}",
                                uses,
                                defs);
                        this.currentInstrs.add(instr);
                    }
                    case Cfg.Value.Int(int n) -> {
                        List<X64.VirtualReg.T> uses = List.of(
                                new X64.VirtualReg.Vid(x, new X64.Type.Int())
                        );
                        List<X64.VirtualReg.T> defs = List.of();
                        X64.Instr.T instr = new X64.Instr.Comment(
                                (uarg, darg) ->
                                        STR."cmpq\t$\{n}, %\{uarg.get(0)}",
                                uses,
                                defs);
                        this.currentInstrs.add(instr);
                    }
                }
            }
            case Cfg.Value.Int(_) -> {
                throw new AssertionError();
            }
        }
    }


    public void munchStm(Cfg.Stm.T s) {
        switch (s) {
            case Cfg.Stm.Assign(Id id, Cfg.Value.T value, Cfg.Type.T type) -> {
                genMove(id, value, munchType(type));
            }
            case Cfg.Stm.AssignBop(
                    Id id,
                    Cfg.Value.T left,
                    String bop,
                    Cfg.Value.T right,
                    Cfg.Type.T type
            ) -> {
                X64.Type.T targetType = munchType(type);
                switch (bop) {
                    case "+" -> {
                        genMove(id, left, targetType);
                        genBop(id, right, "addq");
                    }
                    case "-" -> {
                        genMove(id, left, targetType);
                        genBop(id, right, "subq");
                    }
                    case "<" -> {
//                        genMove(id, left, targetType, instrs);
                        genCmp(left, right);
                        // the first instruction
                        List<X64.VirtualReg.T> uses = List.of();
                        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Reg("rax", new X64.Type.Int()));
                        X64.Instr.T instr = new X64.Instr.MoveConst((uarg, darg) ->
                                STR."setl\t%al",
                                uses,
                                defs);
                        this.currentInstrs.add(instr);
                        uses = List.of(new X64.VirtualReg.Reg("rax", new X64.Type.Int()));
                        instr = new X64.Instr.MoveConst((uarg, darg) ->
                                STR."movzbq\t%al, %rax",
                                uses,
                                defs);
                        this.currentInstrs.add(instr);
                        // the second instruction
                        uses = List.of();
                        defs = List.of(new X64.VirtualReg.Vid(id, new X64.Type.Int()));
                        instr = new X64.Instr.MoveConst((uarg, darg) ->
                                STR."movq\t%rax, %\{darg.getFirst()}",
                                uses,
                                defs);
                        this.currentInstrs.add(instr);
                    }
                    default -> {
                        throw new AssertionError(bop);
                    }
                }
            }
            // id = GetMethod(value, clsName, methodName)
            case Cfg.Stm.GetMethod(
                    Id id,
                    Cfg.Value.T value,
                    Id clsName,
                    Id methodName
            ) -> {
                // move the object into the first arg
                genMoveToReg(X64.Register.argPassingRegs.get(0), value);
                // load the virtual method table pointer
                genMoveToReg(X64.Register.argPassingRegs.get(1),
                        new Cfg.Value.Int(this.layouts.vtablePtrOffsetInObject));
                genMoveToReg(X64.Register.argPassingRegs.get(2),
                        new Cfg.Value.Int(this.layouts.methodOffset(clsName, methodName)));
                genCallDirect("Tiger_getVirtualMethod");
                genMoveFromReg(id, X64.Register.retReg, new X64.Type.CodePtr());
            }
            case Cfg.Stm.AssignCall(
                    Id id,
                    Id func,
                    List<Cfg.Value.T> args,
                    Cfg.Type.T retType
            ) -> {
                X64.Type.T targetType = this.allVars.get(id);
                for (int i = 0; i < args.size(); i++) {
                    // we only process no more than 6 arguments
                    if (i > 5) {
                        throw new Todo("#arguments > 6");
                    }
                    Cfg.Value.T value = args.get(i);
                    String argReg = X64.Register.argPassingRegs.get(i);
                    genMoveToReg(argReg, value);
                }
                genCallIndirect(func);
                String retReg = X64.Register.retReg;
                genMoveFromReg(id, retReg, munchType(retType));
            }
            case Cfg.Stm.AssignNew(Id id, Id clsName) -> {
                X64.Type.T type = this.allVars.get(id);
                // the 1st argument: virtual table pointer
                String argReg = X64.Register.argPassingRegs.getFirst();
                int sizeOfClass = this.layouts.classSize(clsName);
                genMoveToReg(argReg, new Cfg.Value.Int(sizeOfClass));
                // the 2nd argument
                String argReg2 = X64.Register.argPassingRegs.get(1);
                String vtableName = STR.".V_\{clsName}";
                genMoveAddrToReg(argReg2, vtableName);
                // the call
                genCallDirect("Tiger_new");
                String retReg = X64.Register.retReg;
                genMoveFromReg(id, retReg, new X64.Type.ClassType(clsName));
            }
            case Cfg.Stm.Print(Cfg.Value.T value) -> {
                String argReg = X64.Register.argPassingRegs.getFirst();
                genMoveToReg(argReg, value);
                genCallDirect("Tiger_print");
            }
            default -> {
                throw new AssertionError(s);
            }
        }
    }

    public void munchStmTraced(Cfg.Stm.T stm) {
        genComment(STR."stm start: \{stm.getClass()}");
        munchStm(stm);
        genComment(STR."stm finished: \{stm.getClass()}");

    }

    public void munchTransfer(Cfg.Transfer.T transfer,
                              List<X64.Transfer.T> newTransfer,
                              X64.Function.T newFunc) {
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
                    case Cfg.Value.Vid(Id id, _) -> {
                        genCmp(value, new Cfg.Value.Int(1));
                        newTransfer.add(new X64.Transfer.If("je", newTrueBlock, newFalseBlock));
                    }
                    case Cfg.Value.Int(_) -> {
                        throw new AssertionError();
                    }
                }
            }
            case Cfg.Transfer.Ret(Cfg.Value.T retValue) -> {
                String retReg = X64.Register.retReg;
                genMoveToReg(retReg, retValue);
                newTransfer.add(new X64.Transfer.Ret());
            }
        }
    }

    // this pass will create empty block first
    public X64.Block.T munchBlock1(Cfg.Block.T block) {
        switch (block) {
            case Cfg.Block.Singleton(
                    Label label,
                    List<Cfg.Stm.T> _,
                    List<Cfg.Transfer.T> _
            ) -> {
                return new X64.Block.Singleton(label,
                        new LinkedList<>(),
                        new LinkedList<>());
            }
        }
    }

    public void munchBlockPass2(List<Cfg.Block.T> cfgBlocks,
                                X64.Function.T newFunc) {
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
                                Label _,
                                List<X64.Instr.T> newInstrs,
                                List<X64.Transfer.T> newTransfer
                        ) -> {
                            this.currentInstrs = newInstrs;
                            for (Cfg.Stm.T s : stms) {
                                munchStmTraced(s);
                            }
                            munchTransfer(transfer.getFirst(),
                                    newTransfer,
                                    newFunc);
                        }
                    }
                }
            }
        }
    }

    // to implement the calling convention: argument passing
    public void callingConvention(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    X64.Type.T retType,
                    Id classId,
                    Id methodId,
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
                            if (index > 5) {
                                throw new Todo("arguments > 6");
                            }
                            switch (formal) {
                                case X64.Dec.Singleton(
                                        X64.Type.T type,
                                        Id id1
                                ) -> {
                                    genMoveFromReg(id1,
                                            X64.Register.argPassingRegs.get(index),
                                            type);
                                }
                            }
                            index++;
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
                    Id classId,
                    Id functionId,
                    List<Cfg.Dec.T> formals,
                    List<Cfg.Dec.T> locals,
                    List<Cfg.Block.T> blocks
            ) -> {
                // the 1st round will create empty blocks first
                List<X64.Dec.T> newFormals =
                        formals.stream().map(this::munchDec).collect(Collectors.toList());
                List<X64.Dec.T> newLocals =
                        locals.stream().map(this::munchDec).collect(Collectors.toList());

                X64.Function.T newFunc = new X64.Function.Singleton(munchType(retType),
                        classId,
                        functionId,
                        newFormals,
                        newLocals,
                        blocks.stream().map(this::munchBlock1).collect(Collectors.toList()));
                // the 2nd round will translate all blocks
                // record all arguments and locals, which will be used to
                // generate typed variables during code generation.
                this.allVars = new HashMap<>();
                for (X64.Dec.T dec : newFormals) {
                    switch (dec) {
                        case X64.Dec.Singleton(X64.Type.T type, Id id1) -> {
                            this.allVars.put(id1, type);
                        }
                    }
                }
                for (X64.Dec.T dec : newLocals) {
                    switch (dec) {
                        case X64.Dec.Singleton(X64.Type.T type, Id id1) -> {
                            this.allVars.put(id1, type);
                        }
                    }
                }
                this.currentLocals = newLocals;

                munchBlockPass2(blocks, newFunc);
                // implement the calling convention
                callingConvention(newFunc);
                return newFunc;
            }
        }
    }

    private String munchEntry(Cfg.Vtable.Entry entry) {
        return STR."\{entry.classId()}_\{entry.functionId()}";
    }

    public X64.Vtable.T munchVtable(Cfg.Vtable.T f) {
        switch (f) {
            case Cfg.Vtable.Singleton(
                    Id name,
                    List<Cfg.Vtable.Entry> funcTypes
            ) -> {

                return new X64.Vtable.Singleton(
                        name,
                        funcTypes.stream().map(this::munchEntry).collect(Collectors.toList()));
            }
        }
    }

    private X64.Program.T munchProgram0(Cfg.Program.T cfg) {
        // determine the layout of class and virtual method table
        this.layouts = new Layout();
        layouts.layoutProgram(cfg);

        switch (cfg) {
            case Cfg.Program.Singleton(
                    Id entryClassName,
                    Id entryFuncName,
                    List<Cfg.Vtable.T> vtables,
                    List<Cfg.Struct.T> _,
                    List<Cfg.Function.T> functions
            ) -> {
                return new X64.Program.Singleton(
                        entryClassName,
                        entryFuncName,
                        vtables.stream().map(this::munchVtable).collect(Collectors.toList()),
                        new LinkedList<>(),
                        functions.stream().map(this::munchFunction).collect(Collectors.toList()));
            }
        }
    }

    public X64.Program.T munchProgram(Cfg.Program.T cfg) {
        X64.Program.T x64 = munchProgram0(cfg);
        if (Control.X64.dump) {
            X64.Program.pp(x64);
        }
        return x64;
    }
}
