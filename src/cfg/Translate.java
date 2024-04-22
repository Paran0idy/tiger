package cfg;

import ast.Ast;
import ast.Ast.AstId;
import util.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class Translate {
    private final Vector<Cfg.Vtable.T> vtables;
    private final Vector<Cfg.Struct.T> structs;
    private final Vector<Cfg.Function.T> functions;
    // for code generation purpose
    private Id currentClassName = null;
    private Id currentThis = null;
    private Cfg.Function.T currentFunction = null;
    private Cfg.Block.T currentBlock = null;
    private LinkedList<Cfg.Dec.T> newDecs = new LinkedList<>();
    private boolean shouldCloseMethod = true;

    public Translate() {
        this.vtables = new Vector<>();
        this.structs = new Vector<>();
        this.functions = new Vector<>();
    }

    /////////////////////////////
    // translate a type
    private Cfg.Type.T transType(Ast.Type.T ty) {
        switch (ty) {
            case Ast.Type.ClassType(Id id) -> {
                return new Cfg.Type.ClassType(id);
            }
            case Ast.Type.Boolean() -> {
                return new Cfg.Type.Int();
            }
            case Ast.Type.IntArray() -> {
                return new Cfg.Type.IntArray();
            }
            case Ast.Type.Int() -> {
                return new Cfg.Type.Int();
            }
        }
    }

    private Cfg.Dec.T transDec(Ast.Dec.T dec) {
        switch (dec) {
            case Ast.Dec.Singleton(Ast.Type.T type, Ast.AstId aid) -> {
                return new Cfg.Dec.Singleton(transType(type), aid.freshId);
            }
        }
    }

    private List<Cfg.Dec.T> transDecList(List<Ast.Dec.T> decs) {
        return decs.stream().map(this::transDec).collect(Collectors.toList());
    }

    private void emit(Cfg.Stm.T s) {
        Cfg.Block.add(this.currentBlock, s);
    }

    private void emitTransfer(Cfg.Transfer.T s) {
        Cfg.Block.addTransfer(this.currentBlock, s);
    }

    private void emitDec(Cfg.Dec.T dec) {
        this.newDecs.add(dec);
    }

    /////////////////////////////
    // translate an expression
    private Cfg.Value.T transExp(Ast.Exp.T exp) {
        switch (exp) {
            case Ast.Exp.ExpId(AstId aid) -> {
                // for now, this is a fake type
                Cfg.Type.T newType = new Cfg.Type.Int();
                return new Cfg.Value.Vid(aid.freshId, newType);
            }
            case Ast.Exp.Num(int num) -> {
                return new Cfg.Value.Int(num);
            }
            case Ast.Exp.Call(
                    Ast.Exp.T theObject,
                    Ast.AstId methodId,
                    List<Ast.Exp.T> args,
                    Tuple.One<Id> theObjectType,
                    Tuple.One<Ast.Type.T> _
            ) -> {
                // the object
                Cfg.Value.T theObjectValue = transExp(theObject);

                Id funcCodeId = Id.newNoname();
                Cfg.Type.T newType = new Cfg.Type.CodePtr();
                emitDec(new Cfg.Dec.Singleton(newType, funcCodeId));
                emit(new Cfg.Stm.GetMethod(funcCodeId, theObjectValue,
                        theObjectType.get(), methodId.freshId));

                // function arguments
                LinkedList<Cfg.Value.T> newArgs = new LinkedList<>();
                newArgs.add(theObjectValue);
                for (Ast.Exp.T arg : args) {
                    newArgs.add(transExp(arg));
                }
                // the assignment
                Id newLeftId = Id.newNoname();
                // a fake return type
                Cfg.Type.T newRetType = new Cfg.Type.Int();
                emitDec(new Cfg.Dec.Singleton(newRetType, newLeftId));

                emit(new Cfg.Stm.AssignCall(newLeftId, funcCodeId, newArgs, newRetType));
                return new Cfg.Value.Vid(newLeftId, newRetType);
            }
            case Ast.Exp.Bop(Ast.Exp.T left, String op, Ast.Exp.T right) -> {
                Cfg.Value.T lvalue = transExp(left);
                Cfg.Value.T rvalue = transExp(right);
                Id newVar = Id.newNoname();
                switch (op) {
                    case "+" -> {
                        Cfg.Type.T newType = new Cfg.Type.Int();
                        emitDec(new Cfg.Dec.Singleton(newType, newVar));
                        emit(new Cfg.Stm.AssignBop(newVar, lvalue, "+", rvalue, newType));
                        return new Cfg.Value.Vid(newVar, newType);
                    }
                    case "-" -> {
                        Cfg.Type.T newType = new Cfg.Type.Int();
                        emitDec(new Cfg.Dec.Singleton(newType, newVar));
                        emit(new Cfg.Stm.AssignBop(newVar, lvalue, "-", rvalue, newType));
                        return new Cfg.Value.Vid(newVar, newType);
                    }
                    case "<" -> {
                        Cfg.Type.T newType = new Cfg.Type.Int();
                        emitDec(new Cfg.Dec.Singleton(newType, newVar));
                        emit(new Cfg.Stm.AssignBop(newVar, lvalue, "<", rvalue, newType));
                        return new Cfg.Value.Vid(newVar, newType);
                    }
                    default -> {
                        throw new Todo();
                    }
                }
            }
            case Ast.Exp.This() -> {
                return new Cfg.Value.Vid(this.currentThis,
                        new Cfg.Type.ClassType(this.currentClassName));
            }
            case Ast.Exp.NewObject(Id id) -> {
                Id newVar = Id.newNoname();
                Cfg.Type.T newType = new Cfg.Type.ClassType(id);
                emitDec(new Cfg.Dec.Singleton(newType, newVar));
                emit(new Cfg.Stm.AssignNew(newVar, id));
                return new Cfg.Value.Vid(newVar, newType);
            }
            default -> {
                throw new Todo();
            }
        }
    }

    /////////////////////////////
    // translate a statement
    // this function does not return its result,
    // as the result has been saved into currentBlock
    private void transStm(Ast.Stm.T stm) {
        switch (stm) {
            case Ast.Stm.Assign(AstId aid, Ast.Exp.T exp) -> {
                Cfg.Value.T value = transExp(exp);
                // a fake type, to be corrected
                Cfg.Type.T newType = new Cfg.Type.Int();
                emit(new Cfg.Stm.Assign(aid.freshId, value, newType));
            }
            case Ast.Stm.If(Ast.Exp.T cond, Ast.Stm.T thenn, Ast.Stm.T elsee) -> {
                Cfg.Value.T value = transExp(cond);
                Cfg.Block.T trueBlock = new Cfg.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());
                Cfg.Block.T falseBlock = new Cfg.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());
                Cfg.Block.T mergeBlock = new Cfg.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());

                // a branching point
                emitTransfer(new Cfg.Transfer.If(value, trueBlock, falseBlock));
                // all jump to the merge block
                Cfg.Block.addTransfer(trueBlock, new Cfg.Transfer.Jmp(mergeBlock));
                Cfg.Block.addTransfer(falseBlock, new Cfg.Transfer.Jmp(mergeBlock));
                this.currentBlock = trueBlock;
                transStm(thenn);
                Cfg.Function.addBlock(currentFunction, trueBlock);
                this.currentBlock = falseBlock;
                transStm(elsee);
                Cfg.Function.addBlock(currentFunction, falseBlock);
                Cfg.Function.addBlock(currentFunction, mergeBlock);
                this.currentBlock = mergeBlock;
            }
            case Ast.Stm.Print(Ast.Exp.T exp) -> {
                Cfg.Value.T value = transExp(exp);
                emit(new Cfg.Stm.Print(value));
            }
            default -> throw new Todo();
        }
    }

    private Cfg.Function.T translateMethod(Ast.Method.T method) {
        switch (method) {
            case Ast.Method.Singleton(
                    Ast.Type.T retType,
                    Ast.AstId methodId,
                    List<Ast.Dec.T> formals,
                    List<Ast.Dec.T> locals,
                    List<Ast.Stm.T> stms,
                    Ast.Exp.T retExp
            ) -> {
                this.currentThis = Id.newName("this").newSameOrigName();
                // clear the caches:
                Cfg.Function.T newFunc = new Cfg.Function.Singleton(transType(retType),
                        this.currentClassName,
                        methodId.freshId,
                        transDecList(formals),
                        transDecList(locals),
                        new LinkedList<Cfg.Block.T>());

                Cfg.Block.T newBlock = new Cfg.Block.Singleton(new Label(),
                        new LinkedList<>(),
                        new LinkedList<>());
                // add the new block into the function
                Cfg.Function.addBlock(newFunc, newBlock);
                // clear the caches:
                this.currentFunction = newFunc;
                this.currentBlock = newBlock;
                this.newDecs = new LinkedList<>();
                stms.forEach(this::transStm);

                // translate the "retExp"
                Cfg.Value.T retValue = transExp(retExp);
                emitTransfer(new Cfg.Transfer.Ret(retValue));

                // close the method, if it is non-static:
                if (this.shouldCloseMethod) {
                    Cfg.Dec.T newFormal = new Cfg.Dec.Singleton(new Cfg.Type.ClassType(this.currentClassName),
                            this.currentThis);
                    Cfg.Function.addFirstFormal(newFunc, newFormal);
                }
                // add newly generated locals
                Cfg.Function.addDecs(newFunc, this.newDecs);
                return newFunc;
            }
        }
    }

    // the prefixing algorithm
    private void prefixing(Tree<Ast.Class.T>.Node currentRoot,
                           Vector<Cfg.Dec.T> decs,
                           Vector<Cfg.Vtable.Entry> functions) {
        Ast.Class.T cls = currentRoot.data;
        this.currentClassName = null;
        List<Ast.Dec.T> localDecs = List.of();
        if (cls instanceof Ast.Class.Singleton(
                Id classId,
                Id _,
                List<Ast.Dec.T> decs1,
                List<Ast.Method.T> _,
                util.Tuple.One<Ast.Class.T> _
        )) {
            this.currentClassName = classId;
            localDecs = decs1;
        }
        // instance variables
        Vector<Cfg.Dec.T> newDecs = (Vector<Cfg.Dec.T>) decs.clone();
        for (Ast.Dec.T localDec : localDecs) {
            Cfg.Dec.T newLocalDec = transDec(localDec);
            int index = decs.indexOf(newLocalDec);
            if (index == -1) {
                newDecs.add(newLocalDec);
            } else {
                newDecs.set(index, newLocalDec);
            }
        }
        Cfg.Struct.T struct = new Cfg.Struct.Singleton(Ast.Class.getClassId(currentRoot.data),
                newDecs);
        this.structs.add(struct);

        // methods
        assert cls instanceof Ast.Class.Singleton;
        List<Ast.Method.T> localMethods = ((Ast.Class.Singleton) cls).methods();
        Vector<Cfg.Vtable.Entry> newEntries = (Vector<Cfg.Vtable.Entry>) functions.clone();
        for (Ast.Method.T localMethod : localMethods) {
            Ast.Method.Singleton lm = (Ast.Method.Singleton) localMethod;
            Cfg.Vtable.Entry newEntry = new Cfg.Vtable.Entry(transType(lm.retType()),
                    this.currentClassName,
                    lm.methodId().freshId,
                    transDecList(lm.formals()));
            for (int i = 0; i < functions.size(); i++) {
                Cfg.Vtable.Entry ve = functions.get(i);
                // method overriding
                if (lm.methodId().freshId.equals(ve.funcName())) {
                    newEntries.set(i, newEntry);
                }
            }
            newEntries.add(newEntry);
            // translate the method
            Cfg.Function.T newFunc = translateMethod(localMethod);
            this.functions.add(newFunc);
        }
        Cfg.Vtable.T vtable = new Cfg.Vtable.Singleton(
                Ast.Class.getClassId(currentRoot.data),
                newEntries);
        this.vtables.add(vtable);

        // process childrens, recursively
        for (var child : currentRoot.children) {
            prefixing(child, newDecs, newEntries);
        }
    }


    // given an abstract syntax tree, lower it down
    // to a corresponding control-flow graph.
    private Cfg.Program.T translate0(Ast.Program.T ast) {
        // build the inheritance tree
        Tree<Ast.Class.T> tree = new InheritTree().buildTree(ast);

        // start from the tree root, perform prefixing
        prefixing(tree.root,
                new Vector<>(),
                new Vector<>());

        // "Main" class is special, it has neither vtable nor struct.
        // hence we create a temporary method, and translate it.
        Ast.MainClass.Singleton mainCls = null;
        if (ast instanceof Ast.Program.Singleton(Ast.MainClass.T mainClass, List<Ast.Class.T> _)) {
            mainCls = (Ast.MainClass.Singleton) mainClass;
        }
        assert mainCls != null;
        this.currentClassName = mainCls.classId();
        this.shouldCloseMethod = false;
        AstId mainMethodId = new Ast.AstId(Id.newName("main"));
        Id freshMainMethodId = mainMethodId.genFreshId();
        Ast.Method.T mainMethod = new Ast.Method.Singleton(new Ast.Type.Int(),
                mainMethodId,
                new LinkedList<>(),
                new LinkedList<>(),
                List.of(mainCls.stm()),
                new Ast.Exp.Num(0));
        this.functions.add(translateMethod(mainMethod));

        return new Cfg.Program.Singleton(mainCls.classId(),
                freshMainMethodId,
                this.vtables,
                this.structs,
                this.functions);
    }

    public Cfg.Program.T translate(Ast.Program.T ast) {
        Trace<Ast.Program.T, Cfg.Program.T> trace =
                new Trace<>("cfg.Translate.translate",
                        this::translate0,
                        ast,
                        new ast.PrettyPrinter()::ppProgram,
                        Cfg.Program::pp);
        return trace.doit();
    }
}
