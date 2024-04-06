package regalloc;

import codegen.X64;
import util.Todo;

import java.util.List;

// a linear scan register allocator.
class LinearScan {


    public X64.Program.T allocProgram(X64.Program.T x64) {
        switch (x64) {
            case X64.Program.Singleton(
                    String entryFuncName,
                    List<X64.Vtable.T> vtables,
                    List<X64.Struct.T> structs,
                    List<X64.Function.T> functions
            ) -> {
                throw new Todo();
            }
        }
    }
}