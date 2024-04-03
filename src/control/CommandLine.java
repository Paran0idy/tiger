package control;

import lexer.Token;
import util.Bug;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CommandLine {
    enum Kind {
        Empty,
        Bool,
        Int,
        String,
        StringList,
    }

    record Arg(
            String name,
            String option,
            String description,
            Kind kind,
            Consumer<Object> action) {
    }

    private final List<Arg> args;

    public void error(String message) {
        System.err.println(STR."Error: \{message}");
        usage();
        System.exit(1);
    }

    public CommandLine() {
        this.args = List.of(

                new Arg("dump",
                        "{token}",
                        "dump tokens from lexical analysis",
                        Kind.String,
                        (Object x) -> {
                            String s = (String) x;
                            switch (s) {
                                case "token" -> {
                                    Control.Lexer.dumpToken = true;
                                }
                                default -> {
                                    error(STR."unknown argument: \{s}");
                                }
                            }
                        }),
                new Arg(
                        "help",
                        null,
                        "show this help information",
                        Kind.Empty,
                        (_) -> {
                            usage();
                            System.exit(1);
                        })
        );
    }

    // scan the command line arguments, return the file name
    // in it; return null if there is no file name.
    // The file name should be unique.
    public String scan(String[] cmdLineArgs) {
        String filename = null;

        for (int i = 0; i < cmdLineArgs.length; i++) {
            String cmdArg = cmdLineArgs[i];
            if (!cmdArg.startsWith("-")) {
                if (null != filename) {
                    error("compile only one Java file one time");
                } else {
                    filename = cmdArg;
                    continue;
                }
            }

            // to crawl through arguments:
            cmdArg = cmdArg.substring(1);
            boolean foundArg = false;
            for (Arg arg : this.args) {
                if (!arg.name.equals(cmdArg))
                    continue;

                foundArg = true;
                String param = "";
                switch (arg.kind) {
                    case Kind.Empty -> {
                        arg.action.accept(null);
                    }
                    default -> {
                        if (i >= cmdLineArgs.length)
                            error("wants more arguments");
                        else {
                            param = cmdLineArgs[i++];
                        }
                    }
                }
                switch (arg.kind) {
                    case Kind.Empty -> {
                        arg.action.accept(null);
                    }
                    case Kind.Bool -> {
                        switch (param) {
                            case "true" -> {
                                arg.action.accept(true);
                            }
                            case "false" -> {
                                arg.action.accept(false);
                            }
                            default -> {
                                error(STR."\{arg.name} requires a boolean");
                            }
                        }
                    }
                    case Int -> {
                        int num = 0;
                        try {
                            num = Integer.parseInt(param);
                        } catch (java.lang.NumberFormatException e) {
                            error(STR."\{arg.name} requires an integer");
                        }
                        arg.action.accept(num);
                    }
                    case String -> {
                        arg.action.accept(param);
                    }
                    case StringList -> {
                        String[] strArray = param.split(",");
                        arg.action.accept(strArray);
                    }
                    default -> {
                        error("");
                    }
                }
            }
            if (!foundArg) {
                error(STR."invalid option: \{cmdLineArgs[i]}");
            }
        }
        return filename;
    }

    private void outputSpace(int n) throws Exception {
        if (n < 0)
            throw new Bug();

        while (n-- != 0)
            System.out.print(" ");
    }

    public void output() {
        int max = 0;
        for (Arg a : this.args) {
            int current = a.name.length();
            if (a.option != null)
                current += a.option.length();
            if (current > max)
                max = current;
        }
        System.out.println("Available options:");
        for (Arg a : this.args) {
            int current = a.name.length();
            System.out.print(STR."   -\{a.name} ");
            if (a.option != null) {
                current += a.option.length();
                System.out.print(a.option);
            }
            try {
                outputSpace(max - current + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(a.description);
        }
    }

    public void usage() {
        int startYear = 2013;
        System.out.println(STR."""
                The Tiger compiler. Copyright (C) \{startYear}-, SSE of USTC.
                Usage: java Tiger [options] <filename>
                """);
        output();
    }
}

