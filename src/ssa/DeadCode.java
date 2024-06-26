package ssa;

import util.Id;
import util.Label;
import util.Todo;

import java.util.List;

public class DeadCode {

    public DeadCode() {
    }


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
    private Ssa.Function.T doitFunction(Ssa.Function.T func) {
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

    // TODO: lab3, exercise 10.
    public Ssa.Program.T doitProgram(Ssa.Program.T prog) {
        switch (prog) {
            case Ssa.Program.Singleton(
                    Id mainClassId,
                    Id mainFuncId,
                    List<Ssa.Vtable.T> vtables,
                    List<Ssa.Struct.T> structs,
                    List<Ssa.Function.T> functions
            ) -> {
                var newFunctions =
                        functions.stream().map(this::doitFunction).toList();
                // TODO: your code here:
                //
                return prog;
            }
        }
    }
}
