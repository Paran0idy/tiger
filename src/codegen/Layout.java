package codegen;

import cfg.Cfg;
import util.Id;

import java.util.HashMap;
import java.util.List;

public class Layout {

    public static class ClassLayoutBinding {
        // the size of a class, in #bytes
        int numBytes;
        // the offsets of all methods in the given class
        HashMap<Id, Integer> methodOffsets;

        ClassLayoutBinding(int numBytes) {
            this.numBytes = numBytes;
            this.methodOffsets = new HashMap<>();
        }
    }

    // map each class id to its layout
    HashMap<Id, ClassLayoutBinding> map;
    // where to put the "vptr" in an object
    public int vtablePtrOffsetInObject;

    Layout() {
        this.map = new HashMap<>();
        this.vtablePtrOffsetInObject = 0;
    }

    public int classSize(Id clazz) {
        return this.map.get(clazz).numBytes;
    }

    public int methodOffset(Id clazz, Id method) {
        ClassLayoutBinding binding = this.map.get(clazz);
        return binding.methodOffsets.get(method);
    }

    public void layoutVtableEntry(Cfg.Vtable.Entry entry, int index) {
        ClassLayoutBinding binding = this.map.get(entry.classId());
        int offset = index * X64.WordSize.bytesOfWord;
        binding.methodOffsets.put(entry.functionId(), offset);
    }

    public void layoutVtable(Cfg.Vtable.T vtable) {
        switch (vtable) {
            case Cfg.Vtable.Singleton(
                    Id name,
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
                    Id clsName,
                    List<Cfg.Dec.T> fields
            ) -> {
                int bytes = fields.size();
                bytes += 1; // the virtual function table pointer
                bytes *= X64.WordSize.bytesOfWord;
                this.map.put(clsName, new ClassLayoutBinding(bytes));
            }
        }
    }

    public void layoutProgram(Cfg.Program.T cfg) {
        switch (cfg) {
            case Cfg.Program.Singleton(
                    Id entryClassName,
                    Id entryFuncName,
                    List<Cfg.Vtable.T> vtables,
                    List<Cfg.Struct.T> structs,
                    List<Cfg.Function.T> functions
            ) -> {
                structs.forEach(this::layoutStruct);
                vtables.forEach(this::layoutVtable);
            }
        }
    }
}
