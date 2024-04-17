import control.CommandLine;
import parser.Parser;

// the Tiger compiler main class.
public class Tiger {
    public static void main(String[] args) throws Exception {
        Parser parser;

        // ///////////////////////////////////////////////////////
        // process command line arguments
        CommandLine cmd = new CommandLine();
        // get the file to be compiled
        String fileName = cmd.scan(args);
        if (fileName == null) {
            // no input file is given, then exit silently.
            return;
        }

        // /////////////////////////////////////////////////////////
        // otherwise, we continue the normal compilation pipeline.
        // first, create a parser:
        parser = new Parser(fileName);
        // then use it to parse the input file:
        parser.parse();

    }
}


