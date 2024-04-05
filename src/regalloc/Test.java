package regalloc;

import ast.SamplePrograms;
import cfg.Cfg;
import cfg.Translate;
import checker.Checker;
import codegen.Munch;
import codegen.X64;

public class Test {
    public static void main(String[] args) throws Exception {
        //
        Checker checker = new Checker();
        checker.checkProgram(SamplePrograms.progSumRec);
        Translate trans = new Translate();
        Cfg.Program.T cfg = trans.translate(SamplePrograms.progSumRec);
        Cfg.Program.pp(cfg);
        // instruction selection
        X64.Program.T x64 = new Munch().munchProgram(cfg);
        X64.Program.pp(x64);

        // register allocation:
        X64.Program.T x64Allocated = new RegAllocStack().allocProgram(x64);
        new PpAssem().ppProgram(x64Allocated);
    }
}
