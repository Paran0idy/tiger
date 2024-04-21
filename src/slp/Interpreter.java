package slp;

import slp.Slp.Exp;
import slp.Slp.Stm;
import util.Todo;

import java.util.HashMap;

// an interpreter for the SLP language.
public class Interpreter {
    // an abstract memory mapping each variable to its value
    HashMap<String, Integer> memory = new HashMap<>();

    // ///////////////////////////////////////////
    // interpret an expression
    private int interpExp(Exp.T exp) {
        throw new Todo(exp);
    }

    // ///////////////////////////////////////////
    // interpret a statement
    public void interpStm(Stm.T stm) {
        throw new Todo(stm);
    }
}
