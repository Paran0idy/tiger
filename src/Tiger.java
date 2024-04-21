import ast.Ast;
import cfg.Cfg;
import checker.Checker;
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


    }
}


