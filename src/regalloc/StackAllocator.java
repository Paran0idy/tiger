package regalloc;

import codegen.X64;
import control.Control;
import util.Todo;
import util.Trace;

// A register allocator to allocate each variable to a physical register,
// using a stack-based allocation approach.
public class StackAllocator {

    private X64.Program.T doitProgram0(X64.Program.T x64) {
        throw new Todo();
    }

    public X64.Program.T doitProgram(X64.Program.T x64) {
        Trace<X64.Program.T, X64.Program.T> trace =
                new Trace<>("regalloc.StackAllocator.allocProgram",
                        this::doitProgram0,
                        x64,
                        X64.Program::pp,
                        X64.Program::pp);
        X64.Program.T result = trace.doit();
        // this should not be controlled by trace
        if (Control.X64.assemFile != null) {
            new PpAssem().ppProgram(result);
        }
        return result;
    }
}