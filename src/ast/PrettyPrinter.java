package ast;

import ast.Ast.*;
import util.Id;
import util.Todo;
import util.Tuple1;

import java.util.List;

public class PrettyPrinter {
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
                    Tuple1<Id> theObjectType,
                    Tuple1<Type.T> retType
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
        switch (t) {
            case Type.Int() -> {
                say("int");
            }
            default -> {
                throw new Todo();
            }
        }
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
        m.formals().forEach(x -> {
            ppDec(x);
            say(", ");
        });
        this.sayln("){");
        indent();
        m.locals().forEach(x -> {
            printSpaces();
            ppDec(x);
            this.sayln(";");
        });
        this.sayln("");
        m.stms().forEach(this::ppStm);
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
        printSpaces();
        this.say(STR."class \{c.classId()}");
        if (c.extends_() != null) {
            this.say(STR." extends \{c.extends_()}");
        } else {
            this.say("");
        }
        this.sayln("{");
        indent();
        c.decs().forEach(this::ppDec);
        c.methods().forEach(this::ppMethod);
        unIndent();
        printSpaces();
        this.sayln("}");
    }

    // main class
    public void ppMainClass(MainClass.T m) {
        MainClass.Singleton mc = (MainClass.Singleton) m;
        this.sayln(STR."class \{mc.classId()}{");
        indent();
        printSpaces();
        this.say(STR."public static void main(String[] ");
        ppAstId(mc.arg());
        sayln("){");
        indent();
        ppStm(mc.stm());
        unIndent();
        printSpaces();
        this.sayln("}");
        unIndent();
        printSpaces();
        this.sayln("}");
        return;
    }

    // program
    public void ppProgram(Program.T prog) {
        Program.Singleton p = (Program.Singleton) prog;
        ppMainClass(p.mainClass());
        this.sayln("");
        p.classes().forEach(this::ppOneClass);
        System.out.println("\n\n");
    }
}

