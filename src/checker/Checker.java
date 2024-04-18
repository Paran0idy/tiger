package checker;

import ast.Ast;
import ast.Ast.Class;
import ast.Ast.*;
import ast.PrettyPrinter;
import control.Control;
import util.Id;
import util.Pair;
import util.Todo;
import util.Tuple1;

import java.util.List;
import java.util.Objects;

public class Checker {
    // symbol table for all classes
    private final ClassTable classTable;
    // symbol table for each method
    private MethodTable methodTable;
    // the class name being checked
    private Id currentClass;

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
    private Type.T checkAstId(AstId aid) {
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
    private Type.T checkExp(Exp.T e) {
        switch (e) {
            case Exp.Call(
                    Exp.T theObject,
                    AstId methodId,
                    List<Exp.T> args,
                    Tuple1<Id> calleeTy,
                    Tuple1<Type.T> retTy
            ) -> {
                var typeOfTheObject = checkExp(theObject);
                Id calleeClassId = null;
                if (Objects.requireNonNull(typeOfTheObject) instanceof Type.ClassType(Id calleeClassId_)) {
                    calleeClassId = calleeClassId_;
                    // put the return type onto the AST
                    calleeTy.setData(calleeClassId);
                }
                var resultMethodId = this.classTable.getMethod(calleeClassId, methodId.id);
                if (resultMethodId == null) {
                    error(STR."method not found: \{calleeClassId} . \{methodId}");
                }
                var resultArgs = args.stream().map(this::checkExp).toList();
                assert resultMethodId != null;
                methodId.freshId = resultMethodId.second();
                Ast.Type.T retType = resultMethodId.first().retType();
                // put the return type onto the AST
                retTy.setData(retType);
                return retType;
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
    private void checkStm(Stm.T s) {
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
    private void checkMethod(Method.T mtd) {
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
    private void checkClass(Class.T c) {
        Class.Singleton cls = (Class.Singleton) c;
        this.currentClass = cls.classId();
        Id extends_ = cls.extends_();
        if (extends_ != null) {
            ClassTable.Binding binding = this.classTable.getClass_(extends_);
            cls.parent().setData(binding.self());
        }
        for (Method.T mtd : cls.methods()) {
            checkMethod(mtd);
        }
    }

    // main class
    private void checkMainClass(MainClass.T c) {
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
        // we do not put Main class into the class table.
        // so that no other class can inherit from it.
        //MainClass.Singleton mc = (MainClass.Singleton) main;
        //this.classTable.putClass(mc.classId(), null);
    }

    // create class table for each normal class
    private void buildClass(Class.T cls) {
        Class.Singleton c = (Class.Singleton) cls;
        this.classTable.putClass(c.classId(), c.extends_(), cls);

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
    private void checkProgram0(Program.T p) {
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

        // ////////////////////////////////////////////////
        // step 2: elaborate each class in turn, under the class table
        // built above.
        checkMainClass(prog.mainClass());
        for (Class.T c : prog.classes()) {
            checkClass(c);
        }

    }

    public Ast.Program.T checkProgram(Program.T ast) {
        PrettyPrinter pp = new PrettyPrinter();

        if (Control.Type.dump) {
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


