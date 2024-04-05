package codegen;

import ast.SamplePrograms;
import cfg.Cfg;
import cfg.Translate;
import checker.Checker;

public class Test {
    public static void main(String[] args) throws Exception {
        //
        Checker checker = new Checker();
        checker.checkProgram(SamplePrograms.progSumRec);
        Translate trans = new Translate();
        Cfg.Program.T cfg = trans.translate(SamplePrograms.progSumRec);
        Cfg.Program.pp(cfg);
        //
        X64.Program.T x64 = new Munch().munchProgram(cfg);
        X64.Program.pp(x64);
    }
}
