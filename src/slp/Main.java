package slp;

public class Main {

    public static void main(String[] args) throws Exception {
        Main obj = new Main();
        obj.doit(SamplePrograms.sample1);
        obj.doit(SamplePrograms.sample2);
    }

    public void doit(Slp.Stm.T prog) throws Exception {
        PrettyPrint pp = new PrettyPrint();
        pp.ppStm(prog);

        // maximum argument:
        MaxArgument max = new MaxArgument();
        max.maxStm(prog);

        // interpreter:
        Interpreter interp = new Interpreter();
        interp.interpStm(prog);

        // compiler to x64:
        Compiler compiler = new Compiler();
        compiler.compileStm(prog);
    }
}
