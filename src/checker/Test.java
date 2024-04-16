package checker;

public class Test {
    public static void main(String[] args) {
        // to test the pretty printer
        Checker checker = new Checker();
        checker.checkProgram(ast.SamplePrograms.progSumRec);
    }
}
