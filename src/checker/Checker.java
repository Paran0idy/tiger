package checker;

import ast.Ast;
import ast.Ast.Class;
import ast.Ast.*;
import ast.PrettyPrinter;
import control.Control;
import util.Id;
import util.Pair;
import util.Todo;

import java.util.List;
import java.util.Objects;

public class Checker {
    // symbol table for all classes
    public ClassTable classTable;
    // symbol table for each method
    public MethodTable methodTable;
    // the class name being checked
    public Id currentClass;

    public Checker() {
        this.classTable = new ClassTable();
        this.methodTable = new MethodTable();
        this.currentClass = null;
    }

    private void error(String s) {
        System.out.println(STR."Error: type mismatch: \{s}");
        System.exit(1);
    }

    private void error(String s, Type.T expected, Type.T got) {
        System.out.println(STR."Error: type mismatch: \{s}");
        Type.output(expected);
        Type.output(got);
        System.exit(1);
    }

    // /////////////////////////////////////////////////////
    // ast-id
    public Type.T checkAstId(AstId aid) {
        boolean isClassField = false;
        // first search in current method table
        Pair<Ast.Type.T, Id> resultId = this.methodTable.get(aid.id);
        // not a local or formal
        if (resultId == null) {
            isClassField = true;
            resultId = this.classTable.getField(this.currentClass, aid.id);
        }
        if (resultId == null) {
            error("id");
        }
        assert resultId != null;
        // set up the fresh
        aid.freshId = resultId.second();
        aid.isClassField = isClassField;
        return resultId.first();
    }

    // /////////////////////////////////////////////////////
    // expressions
    // type check an expression will return its type, as well
    // as a new expression.
    public Type.T checkExp(Exp.T e) {
        switch (e) {
            case Exp.Call(
                    Exp.T callee,
                    AstId methodId,
                    List<Exp.T> args,
                    Type.T calleeType,
                    List<Type.T> argTypes,
                    Type.T retType
            ) -> {
                var resultCallee = checkExp(callee);
                Id calleeClassId = null;
                if (Objects.requireNonNull(resultCallee) instanceof Type.ClassType(Id calleeClassId_)) {
                    calleeClassId = calleeClassId_;
                }
                var resultMethodId = this.classTable.getMethod(calleeClassId, methodId.id);
                if (resultMethodId == null) {
                    error(STR."method not found: \{calleeClassId} . \{methodId}");
                }
                var resultArgs = args.stream().map(this::checkExp).toList();
                assert resultMethodId != null;
                methodId.freshId = resultMethodId.second();
                return resultMethodId.first().retType();
            }
            case Exp.NewObject(Id classId) -> {
                var classBinding = this.classTable.getClass_(classId);
                return Type.getClassType(classId);
            }
            case Exp.Num(int n) -> {
                return Type.getInt();
            }
            case Exp.Bop(Exp.T left, String bop, Exp.T right) -> {
                var resultLeft = checkExp(left);
                var resultRight = checkExp(right);

                switch (bop) {
                    case "+", "-" -> {
                        if (Type.nonEquals(resultLeft, Type.getInt()) ||
                                Type.nonEquals(resultRight, Type.getInt())) {
                            error(bop);
                        }
                        return Type.getInt();
                    }
                    case "<" -> {
                        if (Type.nonEquals(resultLeft, Type.getInt()) ||
                                Type.nonEquals(resultRight, Type.getInt())) {
                            error("<");
                        }
                        return Type.getBool();
                    }
                    default -> throw new Todo();
                }
            }
            case Exp.ExpId(AstId aid) -> {
                return checkAstId(aid);
            }
            case Exp.This() -> {
                return Type.getClassType(this.currentClass);
            }
            default -> throw new Todo();
        }
    }

    // type check statements
    // produce new statement
    public void checkStm(Stm.T s) {
        switch (s) {
            case Stm.If(Exp.T cond, Stm.T then_, Stm.T else_) -> {
                var resultCond = checkExp(cond);
                if (Type.nonEquals(resultCond, Type.getBool())) {
                    error("if require a boolean type");
                }
                checkStm(then_);
                checkStm(else_);
            }
            case Stm.Print(Exp.T exp) -> {
                var resultExp = checkExp(exp);
                if (Type.nonEquals(resultExp, Type.getInt())) {
                    error("print requires an integer type");
                }
            }
            case Stm.Assign(AstId id, Exp.T exp) -> {
                // first lookup in the method table
                var resultAstId = checkAstId(id);
                var resultExp = checkExp(exp);
                if (Type.nonEquals(resultAstId, resultExp)) {
                    error("=");
                }
            }
            default -> throw new Todo();
        }
    }

    // check type
    public void checkType(Type.T t) {
        throw new Todo();
    }

    // dec
    public void checkDec(Dec.T d) {
        throw new Todo();
    }

    // method type
    private List<Type.T> genMethodArgType(List<Dec.T> decs) {
        return decs.stream().map(Dec::getType).toList();
    }

    // method
    public void checkMethod(Method.T mtd) {
        Method.Singleton m = (Method.Singleton) mtd;
        // construct the method table
        this.methodTable = new MethodTable();
        this.methodTable.putFormalLocal(m.formals(), m.locals());
        for (Stm.T stm : m.stms()) {
            checkStm(stm);
        }
        var resultExp = checkExp(m.retExp());
        if (Type.nonEquals(resultExp, m.retType())) {
            error("ret type mismatch", m.retType(), resultExp);
        }
    }

    // class
    public void checkClass(Class.T c) {
        Class.Singleton cls = (Class.Singleton) c;
        this.currentClass = cls.classId();
        for (Method.T mtd : cls.methods()) {
            checkMethod(mtd);
        }
    }

    // main class
    public void checkMainClass(MainClass.T c) {
        MainClass.Singleton mainClass = (MainClass.Singleton) c;
        this.currentClass = mainClass.classId();
        // "main" method has an argument "arg" of type "String[]", but
        // MiniJava programs do not use it.
        // So we can safely create a fake one with integer type.
        this.methodTable = new MethodTable();
        this.methodTable.putFormalLocal(List.of(new Dec.Singleton(Type.getInt(), mainClass.arg())),
                List.of()); // no local variables
        checkStm(mainClass.stm());
    }

    // ////////////////////////////////////////////////////////
    // step 1: create class table for Main class
    private void buildMainClass(MainClass.T main) {
        MainClass.Singleton mc = (MainClass.Singleton) main;
        this.classTable.putClass(mc.classId(), null);
    }

    // create class table for each normal class
    private void buildClass(Class.T cls) {
        Class.Singleton c = (Class.Singleton) cls;
        this.classTable.putClass(c.classId(), c.extends_());

        // add all instance variables into the class table
        for (Dec.T dec : c.decs()) {
            Dec.Singleton d = (Dec.Singleton) dec;
            this.classTable.putField(c.classId(),
                    d.aid(),
                    d.type());
        }
        // add all methods into the class table
        for (Method.T method : c.methods()) {
            Method.Singleton m = (Method.Singleton) method;
            this.classTable.putMethod(c.classId(),
                    m.methodId(),
                    // for now, do not worry to check
                    // method formals, as we will check
                    // this during method table construction.
                    new ClassTable.MethodType(m.retType(),
                            genMethodArgType(m.formals())));
        }
    }


    // to check a program
    public Ast.Program.T checkProgram0(Program.T p) {
        // "p" is singleton
        Program.Singleton prog = (Program.Singleton) p;
        // ////////////////////////////////////////////////
        // step 1: build the class table
        // a class table maps a class name to a class binding:
        // classTable: className -> ClassBinding{extends_, fields, methods}
        buildMainClass(prog.mainClass());
        for (Class.T c : prog.classes()) {
            buildClass(c);
        }

        // we can double-check that the class table is OK!
        //        if (control.Control.ConAst.elabClassTable) {
        //            this.classTable.dump();
        //        }

        // ////////////////////////////////////////////////
        // step 2: elaborate each class in turn, under the class table
        // built above.
        checkMainClass(prog.mainClass());
        for (Class.T c : prog.classes()) {
            checkClass(c);
        }

        return p;
    }

    public Ast.Program.T checkProgram(Program.T ast) {
        PrettyPrinter pp = new PrettyPrinter();

        if (Control.Type.dump) {
            pp.afterTypeCheck = false;
            pp.ppProgram(ast);
        }

        checkProgram0(ast);

        if (Control.Type.dump) {
            pp.afterTypeCheck = true;
            pp.ppProgram(ast);
        }
        return ast;
    }
}


