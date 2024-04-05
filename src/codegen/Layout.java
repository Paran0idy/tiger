package codegen;

import cfg.Cfg;

import java.util.HashMap;
import java.util.List;

public class Layout {

    public static class ClassLayoutBinding {
        // the size of a class, in bytes
        int numBytes;
        // the offsets of all methods in the given class
        HashMap<String, Integer> methodOffsets;

        ClassLayoutBinding(int numBytes) {
            this.numBytes = numBytes;
            this.methodOffsets = new HashMap<>();
        }
    }

    //
    HashMap<String, ClassLayoutBinding> map;
    public int vtablePtrOffsetInObject;

    Layout() {
        this.map = new HashMap<String, ClassLayoutBinding>();
        this.vtablePtrOffsetInObject = 0;
    }

    public int classSize(String clazz) {
        return this.map.get(clazz).numBytes;
    }

    public int methodOffset(String clazz, String method) {
        ClassLayoutBinding binding = this.map.get(clazz);
        return binding.methodOffsets.get(method);
    }

    public void layoutVtableEntry(Cfg.Vtable.Entry entry, int index) {
        ClassLayoutBinding binding = this.map.get(entry.clsName());
        int offset = index * X64.WordSize.bytesOfWord;
        binding.methodOffsets.put(entry.funcName(), offset);
    }

    public void layoutVtable(Cfg.Vtable.T vtable) {
        switch (vtable) {
            case Cfg.Vtable.Singleton(
                    String name,
                    List<Cfg.Vtable.Entry> funcTypes
            ) -> {
                int i = 0;
                for (var entry : funcTypes) {
                    layoutVtableEntry(entry, i++);
                }
            }
        }
    }

    public void layoutStruct(Cfg.Struct.T struct) {
        switch (struct) {
            case Cfg.Struct.Singleton(
                    String clsName,
                    List<Cfg.Dec.T> fields
            ) -> {
                int bytes = fields.size();
                bytes += 1; // the virtual function table pointer
                bytes *= X64.WordSize.bytesOfWord;
                this.map.put(clsName, new ClassLayoutBinding(bytes));
            }
        }
    }

    public HashMap<String, ClassLayoutBinding> layoutProgram(Cfg.Program.T cfg) {
        switch (cfg) {
            case Cfg.Program.Singleton(
                    String entryFuncName,
                    List<Cfg.Vtable.T> vtables,
                    List<Cfg.Struct.T> structs,
                    List<Cfg.Function.T> functions
            ) -> {
                for (var struct : structs) {
                    layoutStruct(struct);
                }
                for (var vtable : vtables) {
                    layoutVtable(vtable);
                }
            }
        }

        return map;
    }
}
