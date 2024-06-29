package slp;

public class Main {

    public static void main(String[] args) {
        Main obj = new Main();
        obj.doit(SamplePrograms.sample1);
//        obj.doit(SamplePrograms.sample2);
    }

    public void doit(Slp.Stm.T prog) {
        PrettyPrint pp = new PrettyPrint();
        pp.ppStm(prog);

        // maximum argument:
        MaxArgument max = new MaxArgument();
        System.out.println(max.maxStm(prog));

        // interpreter:
        Interpreter interp = new Interpreter();
        interp.interpStm(prog);

        // compiler to x64:
        Compiler compiler = new Compiler();
        try {
            compiler.compileStm(prog);
        } catch (Exception e) {
            throw new util.Error();
        }
    }
}
