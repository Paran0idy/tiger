import ast.Ast;
import cfg.Cfg;
import checker.Checker;
import control.CommandLine;
import control.Control;
import parser.Parser;
import util.Pass;

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
        Pass<String, Ast.Program.T> parserPass =
                new Pass<>("parsing",
                        // a special hack to allow us to use the builtin ast,
                        // in case that your parser does not work properly.
                        (f) -> ((Control.bultinAst == null) ?
                                new Parser(f).parse() :
                                Control.bultinAst),
                        fileName,
                        Control.Verbose.L0);
        Ast.Program.T ast = parserPass.apply();

        Pass<Ast.Program.T, Ast.Program.T> checkerPass =
                new Pass<>("type checking",
                        (f) -> new Checker().check(f),
                        ast,
                        Control.Verbose.L0);
        Ast.Program.T newAst = checkerPass.apply();

        Pass<Ast.Program.T, Cfg.Program.T> transPass =
                new Pass<>("translating to CFG",
                        (f) -> new cfg.Translate().translate(f),
                        newAst,
                        Control.Verbose.L0);
        Cfg.Program.T cfg = transPass.apply();


    }
}


