package codegen;

import cfg.Cfg;
import util.Id;
import util.Property;
import util.Trace;

import java.util.List;

public class Layout {
    public final int vtablePtrOffsetInObject;

    private final Property<Id, Integer> sizeOfClassProp;
    private final Property<Id, Integer> fieldOffsetProp;
    private final Property<Id, Integer> methodOffsetProp;

    Layout() {
        this.vtablePtrOffsetInObject = 0;
        this.sizeOfClassProp = new Property<>(Id::getPlist);
        this.fieldOffsetProp = new Property<>(Id::getPlist);
        this.methodOffsetProp = new Property<>(Id::getPlist);
    }

    // retrieval
    public int classSize(Id clazz) {
        return sizeOfClassProp.get(clazz);
    }

    public int methodOffset(Id clazz, Id method) {
        return methodOffsetProp.get(method);
    }

    // layout facility
    private void layoutVtableEntry(Cfg.Vtable.Entry entry, int offset) {
        methodOffsetProp.put(entry.functionId(), offset);
    }

    private void layoutVtable(Cfg.Vtable.T vtable) {
        switch (vtable) {
            case Cfg.Vtable.Singleton(
                    Id classId,
                    List<Cfg.Vtable.Entry> funcAndTypes
            ) -> {
                int offset = 0;
                for (Cfg.Vtable.Entry entry : funcAndTypes) {
                    layoutVtableEntry(entry, offset++);
                }
            }
        }
    }

    public void layoutStruct(Cfg.Struct.T struct) {
        switch (struct) {
            case Cfg.Struct.Singleton(
                    Id clsId,
                    List<Cfg.Dec.T> fields
            ) -> {
                int offset = 0;
                // the virtual function table pointer
                offset += X64.WordSize.bytesOfWord;
                for (var entry : fields) {
                    switch (entry) {
                        case Cfg.Dec.Singleton(
                                Cfg.Type.T type,
                                Id id
                        ) -> fieldOffsetProp.put(id, offset);
                    }
                    offset += X64.WordSize.bytesOfWord;
                }
                sizeOfClassProp.put(clsId, offset);
            }
        }
    }

    private Object layoutProgram0(Cfg.Program.T cfg) {
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
        return null;
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
                Trace<Cfg.Program.T, Object> trace =
                        new Trace<>("codegen.Layout.layoutProgram",
                                this::layoutProgram0,
                                cfg,
                                Cfg.Program::pp,
                                (_) -> {
                                    //
                                    structs.forEach((s) -> {
                                        switch (s) {
                                            case Cfg.Struct.Singleton(
                                                    Id clsId,
                                                    List<Cfg.Dec.T> fields
                                            ) -> {
                                                System.out.println(STR."class \{clsId.toString()} size = \{sizeOfClassProp.get(clsId).toString()}");
                                                for (var entry : fields) {
                                                    switch (entry) {
                                                        case Cfg.Dec.Singleton(
                                                                Cfg.Type.T type,
                                                                Id id
                                                        ) -> {
                                                            System.out.println(STR."\{id.toString()} at: \{fieldOffsetProp.get(id).toString()}");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    });
                                    //
                                    vtables.forEach((s) -> {
                                        System.out.println("vtable...");
                                    });
                                });
                trace.doit();
            }
        }
    }
}
