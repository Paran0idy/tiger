package checker;

import ast.Ast;
import ast.Ast.Type;
import util.Todo;

import java.util.List;
import java.util.Objects;

public class ClassTable {

    // a special type for method: argument and return types
    public record MethodType(Type.T retType,
                             List<Ast.Dec.T> argsType) {
        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            for (Ast.Dec.T dec : this.argsType) {
                switch (dec) {
                    case Ast.Dec.Singleton(Type.T type, String id) ->
                            stringBuilder.append(STR."\{type.toString()} \{id}, ");
                }
            }
            stringBuilder.append(STR.") -> \{this.retType.toString()}");
            return stringBuilder.toString();
        }
    }

    // the binding for a class
    public record Binding(
            // null for empty extends
            String extends_,
            // the fields in a class
            java.util.HashMap<String, Type.T> fields,
            // the methods in a class
            java.util.HashMap<String, MethodType> methods) {

        public void put(String xid, Type.T type) {
            if (this.fields.get(xid) != null) {
                System.out.println(STR."duplicated class field: \{xid}");
                System.exit(1);
            }
            this.fields.put(xid, type);
        }

        public void put(String mid, MethodType mt) {
            if (this.methods.get(mid) != null) {
                System.out.println(STR."duplicated class method: \{mid}");
                System.exit(1);
            }
            this.methods.put(mid, mt);
        }

        @Override
        public String toString() {
            System.out.print("extends: ");
            System.out.println(Objects.requireNonNullElse(this.extends_, "<>"));
            System.out.println("\nfields:\n  ");
            System.out.println(fields.toString());
            System.out.println("\nmethods:\n  ");
            System.out.println(methods.toString());
            return "";
        }
    }

    // map a class name (a string), to its corresponding class binding.
    private final java.util.HashMap<String, Binding> table;

    public ClassTable() {
        this.table = new java.util.HashMap<>();
    }

    // Duplication is not allowed
    public void put(String c, Binding cb) {
        if (this.table.get(c) != null) {
            System.out.println(STR."duplicated class: \{c}");
            System.exit(1);
        }
        this.table.put(c, cb);
    }

    // put a field into this table
    // Duplication is not allowed
    public void put(String c, String id, Type.T type) {
        Binding cb = this.table.get(c);
        cb.put(id, type);
        return;
    }

    // put a method into this table
    // Duplication is not allowed.
    // Also note that MiniJava does NOT allow overloading.
    public void put(String c, String id, MethodType type) {
        Binding cb = this.table.get(c);
        cb.put(id, type);
        return;
    }

    // return null for non-existing class
    public Binding get(String className) {
        return this.table.get(className);
    }

    // get type of some field
    // return null for non-existing field.
    public Type.T get(String className, String fieldId) {
        Binding cb = this.table.get(className);
        Type.T type = cb.fields.get(fieldId);
        while (type == null) { // search all parent classes until found or fail
            if (cb.extends_ == null)
                return type;
            cb = this.table.get(cb.extends_);
            type = cb.fields.get(fieldId);
        }
        return type;
    }

    // get type of some method
    // return null for non-existing method
    public MethodType getm(String className, String mid) {
        Binding cb = this.table.get(className);
        MethodType type = cb.methods.get(mid);
        while (type == null) { // search all parent classes until found or fail
            if (cb.extends_ == null)
                return type;

            cb = this.table.get(cb.extends_);
            type = cb.methods.get(mid);
        }
        return type;
    }

    // lab 2, exercise 7:
    public void dump() {
        new Todo();
    }

    @Override
    public String toString() {
        return this.table.toString();
    }
}
