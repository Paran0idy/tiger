package ssa;

import util.Id;
import util.Label;
import util.Todo;
import util.Tuple;
import util.set.HashSet;

import java.util.HashMap;
import java.util.List;

public class ReachDef {


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

    // TODO: lab3, exercise 11.
    public HashMap<Object, Tuple.Two<HashSet<Ssa.Stm.T>, HashSet<Ssa.Stm.T>>>
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
