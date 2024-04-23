package cfg;

import ast.Ast;
import ast.Ast.AstId;
import util.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class Translate {
    // the generated results:
    private final Vector<Cfg.Vtable.T> vtables;
    private final Vector<Cfg.Struct.T> structs;
    private final Vector<Cfg.Function.T> functions;
    // for bookkeeping purpose:
    private Id currentClassId = null;
    private Id currentThis = null;
    private Cfg.Function.T currentFunction = null;
    private Cfg.Block.T currentBlock = null;
    private LinkedList<Cfg.Dec.T> newDecs = new LinkedList<>();
    // for main function
    private Id mainClassId = null;
    private Id mainFunctionId = null;

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
                        new Cfg.Type.ClassType(this.currentClassId));
            }
            case Ast.Exp.NewObject(Id id) -> {
                Id newVar = Id.newNoname();
                Cfg.Type.T newType = new Cfg.Type.ClassType(id);
                emitDec(new Cfg.Dec.Singleton(newType, newVar));
                emit(new Cfg.Stm.AssignNew(newVar, id));
                return new Cfg.Value.Vid(newVar, newType);
            }
            default -> throw new Todo();
        }
    }

    /////////////////////////////
    // translate a statement
    // this function does not return its result,
    // but saved the result into "currentBlock"
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
                this.currentThis = Id.newName("this");
                // clear the caches:
                Cfg.Function.T newFunc = new Cfg.Function.Singleton(transType(retType),
                        this.currentClassId,
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
                if (!methodId.freshId.equals(this.mainFunctionId)) {
                    Cfg.Dec.T newFormal = new Cfg.Dec.Singleton(new Cfg.Type.ClassType(this.currentClassId),
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
    @SuppressWarnings("unchecked")
    private Tuple.Two<Vector<Cfg.Dec.T>,
            Vector<Cfg.Vtable.Entry>> prefixOneNode(Ast.Class.T cls,
                                                    Tuple.Two<Vector<Cfg.Dec.T>,
                                                            Vector<Cfg.Vtable.Entry>> decsAndFunctions) {
        var decs = decsAndFunctions.first();
        var functions = decsAndFunctions.second();
        this.currentClassId = null;
        List<Ast.Dec.T> localDecs;
        List<Ast.Method.T> localMethods;

        switch (cls) {
            case Ast.Class.Singleton(
                    Id classId,
                    Id _,
                    List<Ast.Dec.T> decs1,
                    List<Ast.Method.T> lms,
                    util.Tuple.One<Ast.Class.T> _
            ) -> {
                this.currentClassId = classId;
                localDecs = decs1;
                localMethods = lms;
            }
        }
        // instance variables
        Vector<Cfg.Dec.T> newDecs = (Vector<Cfg.Dec.T>) decs.clone();
        assert localDecs != null;
        for (Ast.Dec.T localDec : localDecs) {
            Cfg.Dec.Singleton newLocalDec = (Cfg.Dec.Singleton) transDec(localDec);
            int index = decs.indexOf(newLocalDec);
            var _ = -1 == index ?
                    newDecs.add(newLocalDec) :
                    newDecs.set(index, newLocalDec);
        }
        Cfg.Struct.T struct = new Cfg.Struct.Singleton(this.currentClassId,
                newDecs);
        this.structs.add(struct);

        // methods
        Vector<Cfg.Vtable.Entry> newFunctions = (Vector<Cfg.Vtable.Entry>) functions.clone();
        for (Ast.Method.T localMethod : localMethods) {
            Ast.Method.Singleton lm = (Ast.Method.Singleton) localMethod;
            // translate each method
            Cfg.Function.T newFunc = translateMethod(localMethod);
            this.functions.add(newFunc);
            // generate the "vtable", but do not process the special "main" method:
            if (lm.methodId().freshId.equals(this.mainFunctionId))
                continue;
            Cfg.Vtable.Entry newEntry = new Cfg.Vtable.Entry(transType(lm.retType()),
                    this.currentClassId,
                    lm.methodId().freshId,
                    transDecList(lm.formals()));
            int i = 0;
            for (; i < functions.size(); i++) {
                Cfg.Vtable.Entry current = functions.get(i);
                // method overriding
                if (lm.methodId().freshId.equals(current.functionId())) {
                    newFunctions.set(i, newEntry);
                    break;
                }
            }
            if (i >= functions.size())
                newFunctions.add(newEntry);

        }
        Cfg.Vtable.T vtable = new Cfg.Vtable.Singleton(
                this.currentClassId,
                newFunctions);
        this.vtables.add(vtable);

        return new Tuple.Two<>(newDecs, newFunctions);
    }

    // build an inherit tree
    private Tree<Ast.Class.T> buildInheritTree0(Ast.Program.T ast) {
        // we create an empty "Object" class with no methods.
        // This class servers as the root node in the inheritance tree.
        // But real "Object" class in Java contains 11 methods, see:
        //   https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/Object.html
        Ast.Class.T objCls = new Ast.Class.Singleton(Id.newName("Object"),
                null, // null for non-existing "extends"
                new LinkedList<>(),
                new LinkedList<>(),
                new Tuple.One<>());// parent

        Tree<Ast.Class.T> tree = new Tree<>("inheritTree");
        // make "Object" the root
        tree.addRoot(objCls);

        switch (ast) {
            case Ast.Program.Singleton(
                    Ast.MainClass.T mainClass,
                    List<Ast.Class.T> allClasses
            ) -> {
                // step #1: create a new class for the "Main" class:
                // allClasses is unmodifiable in the test cases, so we make it modifiable
                LinkedList<Ast.Class.T> newAllClasses = new LinkedList<>(allClasses);
                switch (mainClass) {
                    case Ast.MainClass.Singleton(
                            Id classId,
                            AstId arg,
                            Ast.Stm.T stm
                    ) -> {
                        AstId newMainMethodId = new AstId(Id.newName("main"));
                        LinkedList<Ast.Stm.T> stms = new LinkedList<>();
                        stms.add(stm);
                        Ast.Class.Singleton newMainClass = new Ast.Class.Singleton(
                                classId,
                                null,
                                new LinkedList<>(),
                                List.of(new Ast.Method.Singleton(Ast.Type.getInt(),
                                        newMainMethodId,
                                        new LinkedList<>(),
                                        new LinkedList<>(),
                                        stms,
                                        new Ast.Exp.Num(0))),
                                new Tuple.One<>());
                        this.mainClassId = classId;
                        this.mainFunctionId = newMainMethodId.genFreshId();
                        newAllClasses.add(newMainClass);
                    }
                }

                // step #2: add all classes (including "Main" class
                // we have just create) into the tree,
                newAllClasses.forEach(tree::addNode);
                // to establish the parent-child relationship
                newAllClasses.forEach((c) -> {
                    Ast.Class.Singleton cls = (Ast.Class.Singleton) c;
                    var parentNode = (cls.extends_() == null) ?
                            tree.root :
                            tree.lookupNode(cls.parent().get());
                    var childNode = tree.lookupNode(c);
                    // add the child into parent
                    tree.addEdge(parentNode, childNode);
                });
            }
        }
        return tree;
    }

    private Tree<Ast.Class.T> buildInheritTree(Ast.Program.T ast) {
        Trace<Ast.Program.T, Tree<Ast.Class.T>> trace =
                new Trace<>("cfg.Translate.buildInheritTree",
                        this::buildInheritTree0,
                        ast,
                        (a) -> {
                            new ast.PrettyPrinter().ppProgram(ast);
                        },
                        (tree) -> {
                            throw new Todo();
                        });
        return trace.doit();
    }

    private Cfg.Program.T translate0(Ast.Program.T ast) {
        // Step #1: build the inheritance tree
        Tree<Ast.Class.T> tree = buildInheritTree(ast);
        // Step #2: perform prefixing via a level-order traversal
        // we also translate each method during this traversal.
        tree.levelOrder(tree.root,
                this::prefixOneNode,
                new Tuple.Two<>(new Vector<>(),
                        new Vector<>()));

        return new Cfg.Program.Singleton(this.mainClassId,
                this.mainFunctionId,
                this.vtables,
                this.structs,
                this.functions);
    }

    // given an abstract syntax tree, lower it down
    // to a corresponding control-flow graph.
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
