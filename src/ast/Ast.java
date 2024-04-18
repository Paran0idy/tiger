package ast;

import util.Id;
import util.Todo;

import java.util.HashMap;
import java.util.List;


public class Ast {
    // /////////////////////////////////////////////////////////
    // ast-id
    // we use class instead of record, as we need to change its
    // fields
    public static class AstId {
        public Id id;
        public Id freshId;
        public Type.T type;
        public boolean isClassField;

        public AstId(Id id) {
            this.id = id;
            // following fields have default values
            this.freshId = null;
            this.type = null;
            this.isClassField = false;
        }

        public Id genFreshId() {
            this.freshId = this.id.newSameOrigName();
            return this.freshId;
        }
    }

    //  ///////////////////////////////////////////////////////////
    //  type
    public static class Type {
        public sealed interface T
                permits Boolean, ClassType, Int, IntArray {
        }

        // boolean
        public record Boolean() implements T {
        }

        // class "id"
        public record ClassType(Id id) implements T {
        }

        // int
        public record Int() implements T {
        }

        // int[]
        public record IntArray() implements T {
        }

        // singleton design pattern
        private static final Type.T boolTy = new IntArray();
        private static final Type.T intTy = new Int();
        private static final Type.T intArrayTy = new IntArray();
        private static final HashMap<Id, Type.T> classTyContainer = new HashMap<>();

        public static Type.T getInt() {
            return intTy;
        }

        public static Type.T getBool() {
            return boolTy;
        }

        public static Type.T getIntArray() {
            return intArrayTy;
        }

        public static Type.T getClassType(Id id) {
            Type.T ty = classTyContainer.get(id);
            if (ty == null) {
                ty = new ClassType(id);
                classTyContainer.put(id, ty);
            }
            return ty;
        }

        // do not confuse with the "equals" method from Object.
        public static boolean nonEquals(Type.T ty1, Type.T ty2) {
            // compare the two references' value
            return ty1 != ty2;
        }

        public static void output(Type.T ty) {
            switch (ty) {
                case Type.Boolean() -> System.out.println("boolean");
                case Type.Int() -> System.out.print("int");
                default -> throw new Todo();
            }
        }

        public static String convertString(Type.T ty) {
            switch (ty) {
                case Type.Boolean() -> {
                    return "boolean";
                }
                case Type.Int() -> {
                    return "int";
                }
                default -> throw new Todo();
            }
        }
    }

    // ///////////////////////////////////////////////////
    // declaration
    public static class Dec {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(Type.T type,
                                AstId aid) implements T {
        }

        public static Type.T getType(T dec) {
            switch (dec) {
                case Singleton(Type.T type, _) -> {
                    return type;
                }
            }
        }
    }


    // /////////////////////////////////////////////////////////
    // expression
    public static class Exp {
        // alphabetically-ordered
        public sealed interface T
                permits ArraySelect, Bop, BopBool, Call, ExpId,
                False, Length, NewIntArray, NewObject, Num, This, True, Uop {
        }

        // ArraySelect
        public record ArraySelect(T array, T index) implements T {
        }

        // binary operations
        public record Bop(T left, String op, T right) implements T {
        }

        // op is a boolean operator
        public record BopBool(T left, String op, T right) implements T {
        }

        // Call
        public record Call(T exp,
                           AstId methodId,
                           List<T> args,
                           Type type,     // type of object "exp"
                           List<Type.T> at, // arg's type
                           Type.T rt) implements T {
        }

        // ExpId
        public record ExpId(AstId id) implements T {
        }

        // False
        public record False() implements T {
        }

        // length
        public record Length(T array) implements T {
        }

        // new int [e]
        public record NewIntArray(T exp) implements T {
        }

        // new A();
        public record NewObject(Id id) implements T {
        }

        // number
        public record Num(int num) implements T {
        }

        // this
        public record This() implements T {
        }

        // True
        public record True() implements T {
        }

        // !
        public record Uop(String op, T exp) implements T {
        }
    }
    // end of expression

    // /////////////////////////////////////////////////////////
    // statement
    public static class Stm {
        // alphabetically-ordered
        public sealed interface T
                permits Assign, AssignArray, Block, If, Print, While {
        }

        // assign: id = exp;
        public record Assign(AstId aid, Exp.T exp) implements T {
        }

        // assign-array: id[exp] = exp
        public record AssignArray(AstId id, Exp.T index, Exp.T exp) implements T {
        }

        // block
        public record Block(List<T> stms) implements T {
        }

        // if
        public record If(Exp.T cond, T thenn, T elsee) implements T {
        }

        // System.out.println
        public record Print(Exp.T exp) implements T {
        }

        // while
        public record While(Exp.T cond, T body) implements T {
        }
    }
    // end of statement

    // /////////////////////////////////////////////////////////
    // method
    public static class Method {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(Type.T retType,
                                AstId methodId,
                                List<Dec.T> formals,
                                List<Dec.T> locals,
                                List<Stm.T> stms,
                                Exp.T retExp) implements T {
        }
    }

    // class
    public static class Class {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(Id classId,
                                Id extends_, // null for non-existing "extends"
                                List<Dec.T> decs,
                                List<ast.Ast.Method.T> methods) implements T {
        }
    }

    // main class
    public static class MainClass {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(Id classId,
                                AstId arg,
                                Stm.T stm) implements T {
        }
    }

    // whole program
    public static class Program {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(MainClass.T mainClass,
                                List<Class.T> classes) implements T {
        }
    }
}
