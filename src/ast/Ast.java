package ast;

import util.Todo;

import java.util.HashMap;
import java.util.List;

public class Ast {
    //  ///////////////////////////////////////////////////////////
    //  type
    public static class Type {
        public sealed interface T
                permits Boolean, ClassType, Int, IntArray {
        }

        // boolean
        private record Boolean() implements T {
        }

        // class "id"
        private record ClassType(String id) implements T {
        }

        // int
        private record Int() implements T {
        }

        // int[]
        private record IntArray() implements T {
        }

        // singleton design pattern
        private static final Type.T boolTy = new IntArray();
        private static final Type.T intTy = new Int();
        private static final Type.T intArrayTy = new IntArray();
        private static final HashMap<String, Type.T> classTyContainer = new HashMap<>();

        public static Type.T getInt() {
            return intTy;
        }

        public static Type.T getBool() {
            return boolTy;
        }

        public static Type.T getIntArray() {
            return intArrayTy;
        }

        public static Type.T getClassType(String id) {
            Type.T ty = classTyContainer.get(id);
            if (ty == null) {
                ty = new ClassType(id);
                classTyContainer.put(id, ty);
            }
            return ty;
        }

        // do not confuse with the "equals" method from Object.
        public static boolean equalsType(Type.T ty1, Type.T ty2) {
            // compare the two references' value
            return ty1 == ty2;
        }

        public static void output(Type.T ty) {
            switch (ty) {
                case Type.Boolean() -> System.out.println("boolean");
                case Type.Int() -> System.out.print("int");
                default -> {
                    throw new Todo();
                }
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
                                String id) implements T {
        }
    }


    // /////////////////////////////////////////////////////////
    // expression
    public static class Exp {
        // alphabetically-ordered
        public sealed interface T
                permits ArraySelect, Bop, BopBool, Call,
                False, Id, Length, NewIntArray, NewObject, Num, This, True, Uop {
        }

        // ArraySelect
        public record ArraySelect(T array, T index) implements T {
        }

        // binary operations
        public record Bop(T left, String op, T right) implements T {
        }

        // and, op is a boolean operator
        public record BopBool(T left, String op, T right) implements T {
        }

        // Call
        public record Call(T exp,
                           String id,
                           List<T> args,
                           String type,     // type of first field "exp"
                           List<Type.T> at, // arg's type
                           Type.T rt) implements T {
        }

        // False
        public record False() implements T {
        }

        // Id
        public record Id(String id, Type.T type, boolean isField) implements T {
        }

        // length
        public record Length(T array) implements T {
        }

        // new int [e]
        public record NewIntArray(T exp) implements T {
        }

        // new A();
        public record NewObject(String id) implements T {
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

        // assign
        public record Assign(String id, Exp.T exp, Type.T type) implements T {
        }

        // assign-array
        public record AssignArray(String id, Exp.T index, Exp.T exp) implements T {
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
                                String id,
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

        public record Singleton(String id,
                                String extends_, // null for non-existing "extends"
                                List<Dec.T> decs,
                                List<ast.Ast.Method.T> methods) implements T {
        }
    }

    // main class
    public static class MainClass {
        public sealed interface T
                permits Singleton {
        }

        public record Singleton(String id,
                                String arg,
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
