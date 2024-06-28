package ssa;

import cfg.Cfg;
import control.Control;
import util.Todo;
import util.Trace;

public class Translate {

    private Cfg.Program.T outSsa0(Cfg.Program.T ssa) {
        throw new Todo();
    }

    private Cfg.Program.T outSsa(Cfg.Program.T ssa) {
        Trace<Cfg.Program.T, Cfg.Program.T> trace =
                new Trace<>("ssa.Translate.outSsa",
                        this::outSsa0,
                        ssa,
                        Cfg.Program::pp,
                        Cfg.Program::pp);
        return trace.doit();
    }

    private Cfg.Program.T optimizeSsa0(Cfg.Program.T ssa) {
        throw new Todo();
    }

    private Cfg.Program.T optimizeSsa(Cfg.Program.T ssa) {
        Trace<Cfg.Program.T, Cfg.Program.T> trace =
                new Trace<>("ssa.Translate.optimizeSsa",
                        this::optimizeSsa0,
                        ssa,
                        Cfg.Program::pp,
                        Cfg.Program::pp);
        return trace.doit();
    }

    private Cfg.Program.T buildSsa0(Cfg.Program.T cfg) {
        throw new Todo();
    }

    private Cfg.Program.T buildSsa(Cfg.Program.T cfg) {
        Trace<Cfg.Program.T, Cfg.Program.T> trace =
                new Trace<>("ssa.Translate.buildSsa",
                        this::buildSsa0,
                        cfg,
                        Cfg.Program::pp,
                        Cfg.Program::pp);
        return trace.doit();
    }

    private Cfg.Program.T doitProgram0(Cfg.Program.T cfg) {
        // Step #1: build the SSA from the CFG
        Cfg.Program.T ssa = buildSsa(cfg);


        // Step #2: analyze and optimize the SSA.
        ssa = optimizeSsa(ssa);

        // Step #3: translate the SSA back to CFG.
        Cfg.Program.T newCfg = outSsa(ssa);
        return newCfg;
    }

    // given a control-flow graph, translate it to a corresponding
    // static single-assignment form (SSA), perform optimizations on
    // the resulting SSA, then
    // translate the optimized SSA back to CFG.
    public Cfg.Program.T doitProgram(Cfg.Program.T cfg) {
        Trace<Cfg.Program.T, Cfg.Program.T> trace =
                new Trace<>("ssa.Translate.doitProgram",
                        this::doitProgram0,
                        cfg,
                        Cfg.Program::pp,
                        (x) -> {
                            Cfg.Program.pp(x);
                            if (Control.Dot.beingDotted("cfg2")) {
                                Cfg.Program.dot(x);
                            }
                        });
        return trace.doit();
    }
}
