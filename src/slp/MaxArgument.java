package slp;

import slp.Slp.Exp;
import slp.Slp.Stm;
import util.Todo;

import java.util.List;

import static java.lang.Integer.max;

public class MaxArgument {
    // ///////////////////////////////////////////
    // expression
    private int maxExp(Exp.T exp) {
        switch (exp) {
            case Exp.Num(int n) -> {
                return 0;
            }
            case Exp.Id(String x) -> {
                return 0;
            }
            case Exp.Op(
                    Exp.T left,
                    String bop,
                    Exp.T right
            ) -> {
                return max(maxExp(left), maxExp(right));
            }
            case Exp.Eseq(Stm.T stm, Exp.T e) -> {
                return max(maxStm(stm), maxExp(e));
            }
        }
    }

    // ///////////////////////////////////////////
    // statement
    public int maxStm(Stm.T stm) {
        switch (stm) {
            case Stm.Print(List<Exp.T> exps) -> {
                return exps.size();
            }
            case Stm.Compound(
                    Stm.T s1,
                    Stm.T s2
            ) -> {
                return max(maxStm(s1), maxStm(s2));
            }
            case Stm.Assign(
                    String x,
                    Exp.T e
            ) -> {
                return maxExp(e);
            }
        }
    }
}
