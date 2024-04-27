package slp;

import java.util.List;

// the abstract syntax trees for the SLP language.
public class Slp {
    // ////////////////////////////////////////////////
    // expression
    public static class Exp {
        // the type
        public sealed interface T
                permits Eseq, Id, Op, Num {
        }

        // s, e
        public record Eseq(Stm.T stm,
                           T exp) implements T {
        }

        // x
        public record Id(String id) implements T {
        }

        // e bop e
        public record Op(T left,
                         String op,
                         T right) implements T {
        }

        // n
        public record Num(int num) implements T {
        }
    }
    // end of expression

    // ///////////////////////////////////////////////
    // statement
    public static class Stm {
        // the type
        public sealed interface T
                permits Assign, Compound, Print {
        }

        // x := e
        public record Assign(String id,
                             Exp.T exp) implements T {
        }

        // s1; s2
        public record Compound(T s1,
                               T s2) implements T {
        }

        // print(explist)
        public record Print(List<Exp.T> exps) implements T {
        }
    }
    // end of statement
}
