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
        public record Boolean() implements T {
        }

        // class "id"
        public record ClassType(String id) implements T {
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
        private static final HashMap<String, Type.T> classTyTable = new HashMap<>();

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
            Type.T ty = classTyTable.get(id);
            if (ty == null) {
                ty = new ClassType(id);
                classTyTable.put(id, ty);
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

        // Call: exp.id(args)
        public record Call(T exp,
                           String id,
                           List<T> args,
                           List<String> typeExp_0, // type of first field "exp"
                           List<Type.T> typeArgs, // arg's type
                           List<Type.T> typeRet) implements T {
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

        public static String getName(T c) {
            switch (c) {
                case Singleton(String id, String extends_, List<Dec.T> decs, List<Method.T> methods) -> {
                    return id;
                }
            }
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

        public static List<Class.T> getClasses(T p) {
            switch (p) {
                case Singleton(MainClass.T mainClass, List<Class.T> classes) -> {
                    return classes;
                }
            }
        }

        public static MainClass.T getMainClass(T p) {
            switch (p) {
                case Singleton(MainClass.T mainClass, List<Class.T> classes) -> {
                    return mainClass;
                }
            }
        }

        public static Class.T searchClass(T p, String clsName) {
            switch (p) {
                case Singleton(MainClass.T mainClass, List<Class.T> classes) -> {
                    for (Class.T t : classes) {
                        switch (t) {
                            case Class.Singleton(
                                    String id, String extends_, List<Dec.T> decs, List<Method.T> methods
                            ) -> {
                                if (id.equals(clsName))
                                    return t;
                            }
                        }
                    }
                    return null;
                }
            }
        }
    }
}
