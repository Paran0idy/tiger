package util;

import control.Control;

import java.io.*;
import java.util.LinkedList;

public class Dot {
    public record Element<X, Y, Z>(X x, Y y, Z z) {
//        Entry<X, Y, Z> e;

        @Override
        public String toString() {
            String s = "";
            if (z != null)
                s = z.toString();

            return (STR."""
"\{x.toString()}"->"\{y.toString()}"\{s};
""");
        }
    }

    LinkedList<Element<String, String, String>> list;

    public Dot() {
        this.list = new LinkedList<>();
    }

    public void insert(String from, String to) {
        this.list.addFirst(new Element<>(from, to, null));
    }

    public void insert(String from, String to, String info) {
        String s = STR."[label=\"\{info}\"]";
        // System.out.println(s);
        this.list.addFirst(new Element<>(from, to, s));
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Element<String, String, String> e : this.list) {
            stringBuilder.append(e.toString());
        }
        return stringBuilder.toString();
    }

    public void toDot(String fname) {
        String fn = STR."\{fname}.dot";
        try {
            File f = new File(fn);
            FileWriter fw = new FileWriter(f);
            BufferedWriter w = new BufferedWriter(fw);

            String sb = STR."""
digraph g{
\tsize = "10, 10";
\tnode [color=lightblue2, style=filled];
\{this.toString()}}
""";

            w.write(sb);
            w.close();
            fw.close();
        } catch (Throwable o) {
            throw new util.Error();
        }
    }

    void visualize(String name) {
        toDot(name);
        String format = Control.Cfg.dotOutputFormat;
        String[] args = {"dot", "-T", format, "-O", STR."\{name}.dot"};
        try {
            // Refer to this article:
            // http://walsh.iteye.com/blog/449051
            final class StreamDrainer implements Runnable {
                private final InputStream ins;

                public StreamDrainer(InputStream ins) {
                    this.ins = ins;
                }

                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (Exception e) {
                        throw new util.Error(e);
                    }
                }

            }
            Process process = Runtime.getRuntime().exec(args);
            new Thread(new StreamDrainer(process.getInputStream())).start();
            new Thread(new StreamDrainer(process.getErrorStream())).start();
            process.getOutputStream().close();
            int exitValue = process.waitFor();
            if (false) {
                if (!new File(STR."\{name}.dot").delete())
                    throw new util.Error("Can't delete dot");
            }
        } catch (Throwable o) {
            throw new util.Error(o);
        }
    }
}
