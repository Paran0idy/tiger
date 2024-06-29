package slp;

import slp.Slp.Exp;
import slp.Slp.Stm;
import util.Todo;

import java.util.HashMap;
import java.util.List;

// an interpreter for the SLP language.
public class Interpreter {
    // an abstract memory mapping each variable to its value
    HashMap<String, Integer> memory = new HashMap<>();

    // ///////////////////////////////////////////
    // interpret an expression
    private int interpExp(Exp.T exp) {
        switch (exp) {
            case Exp.Num(int n) -> {
                return n;
            }
            case Exp.Id(String x) -> {
                return memory.get(x);
            }
            case Exp.Op(
                    Exp.T left,
                    String bop,
                    Exp.T right
            ) -> {
                switch (bop) {
                    case "+": {
                        return interpExp(left) + interpExp(right);
                    }
                    case "-": {
                        return interpExp(left) - interpExp(right);
                    }
                    case "*": {
                        return interpExp(left) * interpExp(right);
                    }
                    case "/": {
                        return interpExp(left) / interpExp(right);
                    }
                }
            }
            case Exp.Eseq(Stm.T stm, Exp.T e) -> {
                interpStm(stm);
                return interpExp(e);
            }
        }
        return 0;
    }

    // ///////////////////////////////////////////
    // interpret a statement
    public void interpStm(Stm.T stm) {
        switch (stm) {
            case Stm.Compound(
                    Stm.T s1,
                    Stm.T s2
            ) -> {
                interpStm(s1);
                interpStm(s2);
            }
            case Stm.Assign(
                    String x,
                    Exp.T e
            ) -> {
                memory.put(x, interpExp(e));
            }
            case Stm.Print(List<Exp.T> exps) -> {
                for (Exp.T exp : exps)
                    System.out.print(STR."\{interpExp(exp)} ");
                System.out.println();
            }
        }
    }
}
