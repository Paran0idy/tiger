package ast;

import ast.Ast.*;
import util.Id;
import util.Todo;

import java.util.List;

public class PrettyPrinter {
    public boolean afterTypeCheck = false;
    private int indentLevel = 4;

    public PrettyPrinter() {
        this.indentLevel = 0;
    }

    private void indent() {
        this.indentLevel += 4;
    }

    private void unIndent() {
        this.indentLevel -= 4;
    }

    private void printSpaces() {
        int i = this.indentLevel;
        while (i-- != 0)
            this.say(" ");
    }

    private <T> void sayln(T s) {
        System.out.println(s);
    }

    private <T> void say(T s) {
        System.out.print(s);
    }

    // /////////////////////////////////////////////////////
    // ast id
    public void ppAstId(AstId aid) {
        if (afterTypeCheck)
            say(aid.freshId);
        else
            say(aid.id);
    }

    // /////////////////////////////////////////////////////
    // expressions
    public void ppExp(Exp.T e) {
        switch (e) {
            case Exp.ExpId(AstId aid) -> {
                ppAstId(aid);
            }
            case Exp.Call(
                    Exp.T callee,
                    AstId methodId,
                    List<Exp.T> args,
                    Type type,
                    List<Type.T> argTypes,
                    List<Type.T> retType
            ) -> {
                ppExp(callee);
                say(STR.".");
                ppAstId(methodId);
                say("(");
                for (Exp.T arg : args) {
                    ppExp(arg);
                    say(", ");
                }
                say(")");
            }
            case Exp.NewObject(Id id) -> {
                say(STR."new \{id.toString()}()");
            }
            case Exp.Num(int n) -> {
                say(n);
            }
            case Exp.Bop(Exp.T left, String bop, Exp.T right) -> {
                ppExp(left);
                say(STR." \{bop} ");
                ppExp(right);
            }
            case Exp.This() -> {
                say("this");
            }
            default -> {
                throw new Todo();
            }
        }
    }

    // statement
    public void ppStm(Stm.T s) {
        switch (s) {
            case Stm.If(Exp.T cond, Stm.T then_, Stm.T else_) -> {
                printSpaces();
                say("if(");
                ppExp(cond);
                sayln("){");
                indent();
                ppStm(then_);
                unIndent();
                printSpaces();
                sayln("}else{");
                indent();
                ppStm(else_);
                unIndent();
                printSpaces();
                sayln("}");
            }
            case Stm.Print(Exp.T exp) -> {
                printSpaces();
                say("System.out.println(");
                ppExp(exp);
                sayln(");");
            }
            case Stm.Assign(AstId aid, Exp.T exp) -> {
                printSpaces();
                ppAstId(aid);
                say(STR." = ");
                ppExp(exp);
                sayln(";");
            }
            default -> throw new Todo();
        }
    }

    // type
    public void ppType(Type.T t) {
        // we have made the constructors for type private,
        // hence, we cannot pattern matching it.
//        switch (t) {
//            case Type.Int() -> {
//                say("int");
//            }
//            default -> {
//                throw new Todo();
//            }
//        }
        // instead, we convert it explicitly.
        String s = Type.convertString(t);
        say(s);
    }

    // dec
    public void ppDec(Dec.T dec) {
        Dec.Singleton d = (Dec.Singleton) dec;
        ppType(d.type());
        say(" ");
        ppAstId(d.aid());
    }

    // method
    public void ppMethod(Method.T mtd) {
        Method.Singleton m = (Method.Singleton) mtd;
        printSpaces();
        this.say("public ");
        ppType(m.retType());
        this.say(" ");
        ppAstId(m.methodId());
        this.say(STR."(");
        for (Dec.T d : m.formals()) {
            ppDec(d);
            say(", ");
        }
        this.sayln("){");
        indent();
        for (Dec.T d : m.locals()) {
            printSpaces();
            ppDec(d);
            this.sayln(";");
        }
        this.sayln("");
        for (Stm.T s : m.stms())
            ppStm(s);
        printSpaces();
        this.say("return ");
        ppExp(m.retExp());
        this.sayln(";");
        unIndent();
        printSpaces();
        this.sayln("}");
    }

    // class
    public void ppOneClass(Ast.Class.T cls) {
        Ast.Class.Singleton c = (Ast.Class.Singleton) cls;
        this.say(STR."class \{c.classId()}");
        if (c.extends_() != null)
            this.say(STR." extends \{c.extends_()}");
        else
            this.say("");
        this.sayln("{");
        indent();
        for (Dec.T d : c.decs()) {
            ppDec(d);
        }
        for (Method.T mthd : c.methods())
            ppMethod(mthd);
        this.sayln("}");
        unIndent();
    }

    // main class
    public void ppMainClass(MainClass.T m) {
        MainClass.Singleton mc = (MainClass.Singleton) m;
        this.sayln(STR."class \{mc.classId()}{");
        this.say(STR."\tpublic static void main(String[] ");
        ppAstId(mc.arg());
        sayln("){");
        indent();
        indent();
        ppStm(mc.stm());
        unIndent();
        unIndent();
        this.sayln("\t}");
        this.sayln("}");
        return;
    }

    // program
    public void ppProgram(Program.T prog) {
        Program.Singleton p = (Program.Singleton) prog;
        ppMainClass(p.mainClass());
        this.sayln("");
        for (Ast.Class.T cls : p.classes()) {
            ppOneClass(cls);
        }
        System.out.println("\n\n");
    }
}

