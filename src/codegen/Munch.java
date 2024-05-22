package codegen;

import cfg.Cfg;
import util.Id;
import util.Todo;
import util.Trace;

import java.util.HashMap;
import java.util.List;

public class Munch {
    // all parameters and locals in a function
    private HashMap<Id, X64.Type.T> allVars;
    // current locals, we will append freshly generated locals into it
    private List<X64.Dec.T> currentLocals;
    // points to the current instruction list in a x64 block
    private List<X64.Instr.T> currentInstrs;

    private X64.Program.T doitProgram0(Cfg.Program.T cfg) {
        // TODO: lab 4.
        throw new Todo();
    }

    public X64.Program.T doitProgram(Cfg.Program.T cfg) {
        Trace<Cfg.Program.T, X64.Program.T> trace =
                new Trace<>("codegen.Munch.munchProgram",
                        this::doitProgram0,
                        cfg,
                        Cfg.Program::pp,
                        X64.Program::pp);
        return trace.doit();
    }
}
