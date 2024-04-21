import ast.Ast;
import cfg.Cfg;
import checker.Checker;
import codegen.X64;
import control.CommandLine;
import control.CompilerPass;
import control.Control;
import parser.Parser;

// the Tiger compiler main class.
public class Tiger {
    public static void main(String[] args) throws Exception {
        // ///////////////////////////////////////////////////////
        // process command line arguments
        CommandLine cmd = new CommandLine();
        // get the file to be compiled
        String fileName = cmd.scan(args);
        if (fileName == null) {
            // no input file is given, then exit silently.
            if (Control.bultinAst == null)
                return;
        }

        // /////////////////////////////////////////////////////////
        // otherwise, we continue the normal compilation pipeline.
        CompilerPass<String, Ast.Program.T> parserPass =
                new CompilerPass<>("parsing",
                        // a special hack to allow us to use the builtin ast,
                        // in case that your parser does not work properly.
                        (f) -> ((Control.bultinAst == null) ?
                                new Parser(f).parse() :
                                Control.bultinAst),
                        fileName);
        Ast.Program.T ast = parserPass.apply();

        CompilerPass<Ast.Program.T, Ast.Program.T> checkerPass =
                new CompilerPass<>("type checking",
                        (f) -> new Checker().check(f),
                        ast);
        Ast.Program.T newAst = checkerPass.apply();

        CompilerPass<Ast.Program.T, Cfg.Program.T> transPass =
                new CompilerPass<>("translating to CFG",
                        (f) -> new cfg.Translate().translate(f),
                        newAst);
        Cfg.Program.T cfg = transPass.apply();

        CompilerPass<Cfg.Program.T, X64.Program.T> codeGenPass =
                new CompilerPass<>("code generation",
                        (f) -> new codegen.Munch().munchProgram(f),
                        cfg);
        X64.Program.T x64 = codeGenPass.apply();

        CompilerPass<X64.Program.T, X64.Program.T> regAllocPass =
                new CompilerPass<>("register allocation",
                        (f) -> new regalloc.RegAllocStack().allocProgram(f),
                        x64);
        X64.Program.T newX64 = regAllocPass.apply();


    }
}


