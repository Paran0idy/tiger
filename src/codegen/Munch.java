package codegen;

import cfg.Cfg;
import control.Control;
import util.Error;
import util.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Munch {
    // data structures to hold the layout information for class
    private Layout layouts;
    // all parameters and locals in a function
    private HashMap<Id, X64.Type.T> allVars;
    // current locals, we will append freshly generated locals into it
    private List<X64.Dec.T> currentLocals;
    // points to the current instruction list in a x64 block
    private List<X64.Instr.T> currentInstrs;


    private X64.Type.T munchType(Cfg.Type.T type) {
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

    private X64.Dec.T munchDec(Cfg.Dec.T dec) {
        switch (dec) {
            case Cfg.Dec.Singleton(Cfg.Type.T type, Id id) -> {
                return new X64.Dec.Singleton(munchType(type), id);
            }
        }
    }

    // generate a move instruction
    private void genMoveReg2Id(Id dest,
                               Id reg,
                               // type of the "dest"
                               X64.Type.T targetType) {
        List<X64.VirtualReg.T> uses = List.of(new X64.VirtualReg.Reg(reg, targetType));
        List<X64.VirtualReg.T> defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
        X64.Instr.T instr = new X64.Instr.Move(
                (uarg, darg) ->
                        STR."movq\t\{uarg.getFirst()}, \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a move instruction
    private void genMoveId2Id(Id dest,
                              Id x,
                              // type of the "dest"
                              X64.Type.T targetType) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(x, targetType));
        defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
        X64.Instr.T instr = new X64.Instr.Move(
                (uarg, darg) ->
                        STR."movq\t\{uarg.getFirst()}, \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    private void genMoveConst2Reg(Id reg,
                                  int n,
                                  // type of the "dest"
                                  X64.Type.T targetType) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of();
        defs = List.of(new X64.VirtualReg.Reg(reg, targetType));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."movq\t$\{n}, \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a move instruction
    private void genMoveConst2Reg(Id dest, String label) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of();
        defs = List.of(new X64.VirtualReg.Reg(dest, new X64.Type.CodePtr()));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."leaq\t\{label}(%rip), \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a move instruction
    private void genMoveConst2Id(Id dest,
                                 int n,
                                 X64.Type.T targetType) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of();
        defs = List.of(new X64.VirtualReg.Vid(dest, targetType));
        X64.Instr.T instr = new X64.Instr.MoveConst(
                (uarg, darg) ->
                        STR."movq\t$\{n}, \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    private void genMoveId2Reg(Id reg,
                               Id id,
                               X64.Type.T type) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(id,
                type));
        defs = List.of(new X64.VirtualReg.Reg(reg,
                type));
        X64.Instr.T instr = new X64.Instr.Move(
                (uarg, darg) ->
                        STR."movq\t\{uarg.getFirst()}, \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a load instruction
    private void genLoad(Id dest,
                         Id src,
                         int offset) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(src, new X64.Type.Int()));
        defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.Load(
                (uarg, darg) ->
                        STR."movq\t\{offset}(\{uarg.getFirst()}), \{darg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a binary instruction
    private void genBop(String bop,
                        Id dest,
                        Id src) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(src, new X64.Type.Int()),
                new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
        defs = List.of(new X64.VirtualReg.Vid(dest, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.Bop(
                (uarg, darg) ->
                        STR."\{bop}\t\{uarg.get(0)}, \{uarg.get(1)}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate an indirect call
    private void genCallIndirect(Id fname) {
        // this should be fixed
        // all caller-saved registers should be in the defs...
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(fname, new X64.Type.CodePtr()));
        defs = List.of(new X64.VirtualReg.Reg(X64.Register.retReg, new X64.Type.Int()));
        X64.Instr.T instr = new X64.Instr.CallIndirect(
                (uarg, darg) ->
                        STR."call\t*\{uarg.getFirst()}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // generate a direct call
    private void genCallDirect(String fname) {
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
    private void genComment(String comment) {
        if (!Control.X64.embedComment)
            return;
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of();
        defs = List.of();
        X64.Instr.T instr = new X64.Instr.Comment(
                (uarg, darg) ->
                        STR."//\{comment}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    private void genCmpId2Const(Id left, int right) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(new X64.VirtualReg.Vid(left, new X64.Type.Int()));
        defs = List.of();
        X64.Instr.T instr = new X64.Instr.Comment(
                (uarg, darg) ->
                        STR."cmpq\t$\{right}, \{uarg.get(0)}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    private void genCmpId2Id(Id left, Id right) {
        List<X64.VirtualReg.T> uses, defs;
        uses = List.of(
                new X64.VirtualReg.Vid(left, new X64.Type.Int()),
                new X64.VirtualReg.Vid(right, new X64.Type.Int())
        );
        defs = List.of();
        X64.Instr.T instr = new X64.Instr.Comment(
                (uarg, darg) ->
                        STR."cmpq\t\{uarg.get(0)}, \{uarg.get(1)}",
                uses,
                defs);
        this.currentInstrs.add(instr);
    }

    // You should extend this function to add
    // instruction selection code for the remaining features in CFG.
    // TODO: lab 4, exercise 3:
    private void munchStm(Cfg.Stm.T s) {
        List<X64.VirtualReg.T> uses, defs;
        X64.Instr.T instr;

        switch (s) {
            case Cfg.Stm.Assign(Id id, Cfg.Exp.T exp) -> {
                switch (exp) {
                    case Cfg.Exp.Bop(
                            String op,
                            List<Id> operands,
                            Cfg.Type.T type
                    ) -> {
                        X64.Type.T targetType = munchType(type);
                        switch (op) {
                            case "+" -> {
                                genMoveId2Id(id, operands.get(0), targetType);
                                genBop("addq", id, operands.get(1));
                            }
                            case "-" -> {
                                genMoveId2Id(id, operands.get(0), targetType);
                                genBop("subq", id, operands.get(1));
                            }
                            case "<" -> {
                                genCmpId2Id(operands.get(1), operands.get(0));
                                // the first instruction
                                uses = List.of();
                                defs = List.of(
                                        new X64.VirtualReg.Reg(Id.newName("%rax"),
                                                new X64.Type.Int())
                                );
                                instr = new X64.Instr.MoveConst((_, _) ->
                                        STR."setl\t%al",
                                        uses,
                                        defs);
                                this.currentInstrs.add(instr);
                                uses = List.of(new X64.VirtualReg.Reg(Id.newName("%rax"),
                                        new X64.Type.Int()));
                                instr = new X64.Instr.MoveConst((_, _) ->
                                        STR."movzbq\t%al, %rax",
                                        uses,
                                        defs);
                                this.currentInstrs.add(instr);
                                // the second instruction
                                uses = List.of();
                                defs = List.of(new X64.VirtualReg.Vid(id,
                                        new X64.Type.Int()));
                                instr = new X64.Instr.MoveConst((_, darg) ->
                                        STR."movq\t%rax, \{darg.getFirst()}",
                                        uses,
                                        defs);
                                this.currentInstrs.add(instr);
                            }
                            default -> throw new Error(op);
                        }
                    } // end of "bop"
                    case Cfg.Exp.Call(
                            Id func,
                            List<Id> args,
                            Cfg.Type.T retType
                    ) -> {
                        X64.Type.T targetType = this.allVars.get(id);
                        for (int i = 0; i < args.size(); i++) {
                            // we only process no more than 6 arguments
                            if (i > 5) {
                                // TODO: lab 4, exercise 4.
                                throw new Todo("#arguments > 6");
                            }
                            Id value = args.get(i);
                            Id argReg = X64.Register.argPassingRegs.get(i);
                            genMoveId2Reg(argReg, value, new X64.Type.Int());
                        }
                        genCallIndirect(func);
                        genMoveReg2Id(id, X64.Register.retReg, munchType(retType));
                    }
                    case Cfg.Exp.Eid(Id x, Cfg.Type.T type) -> {
                        genMoveId2Id(id, x, munchType(type));
                    }
                    // id = GetMethod(obj, clsName, methodName)
                    case Cfg.Exp.GetMethod(
                            Id objId,
                            Id clsId,
                            Id methodId
                    ) -> {
                        // move the object into the first arg
                        genMoveId2Reg(X64.Register.argPassingRegs.get(0),
                                objId,
                                new X64.Type.ClassType(clsId));
                        // load the virtual method table pointer
                        genMoveConst2Reg(X64.Register.argPassingRegs.get(1),
                                this.layouts.vtablePtrOffsetInObject,
                                new X64.Type.Int());
                        genMoveConst2Reg(X64.Register.argPassingRegs.get(2),
                                this.layouts.methodOffset(clsId, methodId),
                                new X64.Type.Int());
                        genCallDirect("Tiger_getVirtualMethod");
                        genMoveReg2Id(id, X64.Register.retReg, new X64.Type.CodePtr());
                    }
                    case Cfg.Exp.Int(int n) -> {
                        genMoveConst2Id(id,
                                n,
                                new X64.Type.Int());
                    }
                    case Cfg.Exp.New(Id clsId) -> {
                        X64.Type.T type = this.allVars.get(id);
                        // the 1st argument: virtual table pointer
                        Id argReg0 = X64.Register.argPassingRegs.getFirst();
                        int sizeOfClass = this.layouts.classSize(clsId);
                        genMoveConst2Reg(argReg0, sizeOfClass, new X64.Type.Int());
                        // the 2nd argument
                        Id argReg1 = X64.Register.argPassingRegs.get(1);
                        String vtableName = STR.".V_\{clsId.toString()}";
                        genMoveConst2Reg(argReg1, vtableName);
                        // generate the call
                        genCallDirect("Tiger_new");
                        Id retReg = X64.Register.retReg;
                        genMoveReg2Id(id, retReg, new X64.Type.ClassType(clsId));
                    }
                    case Cfg.Exp.Print(Id x) -> {
                        Id argReg = X64.Register.argPassingRegs.getFirst();
                        genMoveId2Reg(argReg, x, new X64.Type.Int());
                        genCallDirect("Tiger_print");
                    }
                    default -> throw new Error(s);
                }
            }
        }
    }

    private void munchStmTraced(Cfg.Stm.T stm) {
        genComment(STR."stm start: \{stm.getClass()}");
        munchStm(stm);
        genComment(STR."stm finished: \{stm.getClass()}");
    }

    private void munchTransfer(Cfg.Transfer.T transfer,
                               List<X64.Transfer.T> newTransfer,
                               X64.Function.T newFunc) {
        switch (transfer) {
            case Cfg.Transfer.Jmp(Cfg.Block.T target) -> {
                Label label = Cfg.Block.getLabel(target);
                X64.Block.T newBlock = X64.Function.getBlock(newFunc, label);
                newTransfer.add(new X64.Transfer.Jmp(newBlock));
            }
            case Cfg.Transfer.If(
                    Id x,
                    Cfg.Block.T trueBlock,
                    Cfg.Block.T falseBlocks
            ) -> {
                Label trueLabel = Cfg.Block.getLabel(trueBlock);
                Label falseLabel = Cfg.Block.getLabel(falseBlocks);
                X64.Block.T newTrueBlock = X64.Function.getBlock(newFunc, trueLabel);
                X64.Block.T newFalseBlock = X64.Function.getBlock(newFunc, falseLabel);
                genCmpId2Const(x, 1);
                newTransfer.add(new X64.Transfer.If("je", newTrueBlock, newFalseBlock));
            }
            case Cfg.Transfer.Ret(Id x) -> {
                Id retReg = X64.Register.retReg;
                genMoveId2Reg(retReg, x, new X64.Type.Int());
                newTransfer.add(new X64.Transfer.Ret());
            }
        }
    }

    // this pass will create empty block first
    private X64.Block.T munchBlock1(Cfg.Block.T block) {
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

    private void munchBlockPass2(List<Cfg.Block.T> cfgBlocks,
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
    private void callingConvention(X64.Function.T f) {
        switch (f) {
            case X64.Function.Singleton(
                    X64.Type.T retType,
                    Id classId,
                    Id methodId,
                    List<X64.Dec.T> formals,
                    List<X64.Dec.T> locals,
                    List<X64.Block.T> blocks
            ) -> {
                X64.Block.Singleton newEntryBlock = new X64.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());
                newEntryBlock.transfer().add(new X64.Transfer.Jmp(blocks.getFirst()));
                blocks.addFirst(newEntryBlock);
                // to move arguments:
                int index = 0;
                for (X64.Dec.T formal : formals) {
                    if (index > 5) {
                        // TODO: lab 4, exercise 4.
                        throw new Todo("arguments > 6");
                    }
                    switch (formal) {
                        case X64.Dec.Singleton(
                                X64.Type.T type,
                                Id id1
                        ) -> {
                            genMoveReg2Id(id1,
                                    X64.Register.argPassingRegs.get(index),
                                    type);
                        }
                    }
                    index++;
                }
            }
        }
    }

    private X64.Function.T munchFunction(Cfg.Function.T f) {
        switch (f) {
            case Cfg.Function.Singleton(
                    Cfg.Type.T retType,
                    Id classId,
                    Id functionId,
                    List<Cfg.Dec.T> formals,
                    List<Cfg.Dec.T> locals,
                    List<Cfg.Block.T> blocks
            ) -> {
                // the first pass will create empty blocks first
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
                // the second pass will translate all blocks
                // record all arguments and locals, which will be used to
                // generate typed variables during code generation.
                this.allVars = new HashMap<>();
                newFormals.forEach((dec) -> {
                    switch (dec) {
                        case X64.Dec.Singleton(X64.Type.T type, Id id1) -> {
                            this.allVars.put(id1, type);
                        }
                    }
                });
                newLocals.forEach((dec) -> {
                    switch (dec) {
                        case X64.Dec.Singleton(X64.Type.T type, Id id1) -> {
                            this.allVars.put(id1, type);
                        }
                    }
                });
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

    private X64.Vtable.T munchVtable(Cfg.Vtable.T f) {
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
        Trace<Cfg.Program.T, X64.Program.T> trace =
                new Trace<>("codegen.Munch.munchProgram",
                        this::munchProgram0,
                        cfg,
                        Cfg.Program::pp,
                        X64.Program::pp);
        return trace.doit();
    }
}
