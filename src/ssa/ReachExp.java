package ssa;

import util.Id;
import util.Label;
import util.Todo;
import util.Tuple;
import util.set.Set;

import java.util.HashMap;
import java.util.List;

public class ReachExp {


    // /////////////////////////////////////////////////////////
    // statement
    private void doitStm(Ssa.Stm.T t) {
        throw new Todo();
    }
    // end of statement

    // /////////////////////////////////////////////////////////
    // transfer
    private void doitTransfer(Ssa.Transfer.T t) {
        throw new Todo();
    }

    // /////////////////////////////////////////////////////////
    // block
    private void doitBlock(Ssa.Block.T b) {
        switch (b) {
            case Ssa.Block.Singleton(
                    Label label,
                    List<Ssa.Stm.T> phis,
                    List<Ssa.Stm.T> stms,
                    List<Ssa.Transfer.T> transfer
            ) -> throw new Todo();
        }
    }

    // /////////////////////////////////////////////////////////
    // function
    // TODO: lab3, exercise 9.
    private void doitFunction(Ssa.Function.T func) {
        switch (func) {
            case Ssa.Function.Singleton(
                    Ssa.Type.T retType,
                    Id classId,
                    Id functionId,
                    List<Ssa.Dec.T> formals,
                    List<Ssa.Dec.T> locals,
                    List<Ssa.Block.T> blocks
            ) -> throw new Todo();
        }
    }

    public HashMap<Object, Tuple.Two<Set<Ssa.Exp.T>, Set<Ssa.Exp.T>>>
    doitProgram(Ssa.Program.T prog) {
        switch (prog) {
            case Ssa.Program.Singleton(
                    Id mainClassId,
                    Id mainFuncId,
                    List<Ssa.Vtable.T> vtables,
                    List<Ssa.Struct.T> structs,
                    List<Ssa.Function.T> functions
            ) -> {
                functions.forEach(this::doitFunction);
                return null;
            }
        }
    }
}
