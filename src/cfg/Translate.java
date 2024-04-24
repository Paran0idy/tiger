package cfg;

import ast.Ast;
import control.Control;
import util.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
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
        throw new Todo();
    }

    /////////////////////////////
    // translate a statement
    // this function does not return its result,
    // but saved the result into "currentBlock"
    private void transStm(Ast.Stm.T stm) {
        throw new Todo();
    }

    private Cfg.Function.T transMethod(Ast.Method.T method) {
        throw new Todo();
    }

    // the prefixing algorithm
    private Tuple.Two<Vector<Cfg.Dec.T>,
            Vector<Cfg.Vtable.Entry>> prefixOneClass(Ast.Class.T cls,
                                                     Tuple.Two<Vector<Cfg.Dec.T>,
                                                             Vector<Cfg.Vtable.Entry>> decsAndFunctions) {
        throw new Todo();
    }

    // build an inherit tree
    private Tree<Ast.Class.T> buildInheritTree0(Ast.Program.T ast) {
        throw new Todo();
    }

    private Tree<Ast.Class.T> buildInheritTree(Ast.Program.T ast) {
        Trace<Ast.Program.T, Tree<Ast.Class.T>> trace =
                new Trace<>("cfg.Translate.buildInheritTree",
                        this::buildInheritTree0,
                        ast,
                        new ast.PrettyPrinter()::ppProgram,
                        (tree) -> {
                            // draw the tree
                            throw new Todo();
                        });
        return trace.doit();
    }

    private Cfg.Program.T translate0(Ast.Program.T ast) {
        // if we are using the builtin AST, then do not generate
        // the CFG, but load the CFG directly from disk
        // and return it.
        if (Control.bultinAst != null) {
            Cfg.Program.T result;
            String serialFileName = "./cfg/SumRec.java.cfg.ser";
            try {
                FileInputStream fileIn = new FileInputStream(serialFileName);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                result = (Cfg.Program.T) in.readObject();
                in.close();
                fileIn.close();
            } catch (Exception e) {
                throw new util.Error(e);
            }
            return result;
        }

        // Step #1: build the inheritance tree
        Tree<Ast.Class.T> tree = buildInheritTree(ast);
        // Step #2: perform prefixing via a level-order traversal
        // we also translate each method during this traversal.
        tree.levelOrder(tree.root,
                this::prefixOneClass,
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
