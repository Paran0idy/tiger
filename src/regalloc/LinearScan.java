package regalloc;

import codegen.X64;
import control.Control;
import util.Todo;
import util.Trace;

// A linear scan register allocator.
public class LinearScan {


    private X64.Program.T doitProgram0(X64.Program.T x64) {
        throw new Todo();
    }

    public X64.Program.T doitProgram(X64.Program.T x64) {
        Trace<X64.Program.T, X64.Program.T> trace =
                new Trace<>("regalloc.LinearScan.doitProgram",
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