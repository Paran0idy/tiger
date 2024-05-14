package cfg;

import util.*;
import util.set.HashSet;

import java.util.HashMap;
import java.util.List;

public class CopyProp {

    public CopyProp() {
    }


    // /////////////////////////////////////////////////////////
    // statement
    private void doitStm(Cfg.Stm.T t) {
        throw new Todo();
    }
    // end of statement

    // /////////////////////////////////////////////////////////
    // transfer
    private void doitTransfer(Cfg.Transfer.T t) {
        throw new Todo();
    }

    // /////////////////////////////////////////////////////////
    // block
    private void doitBlock(Cfg.Block.T b) {
        switch (b) {
            case Cfg.Block.Singleton(
                    Label label,
                    List<Cfg.Stm.T> stms,
                    List<Cfg.Transfer.T> transfer
            ) -> throw new Todo();
        }
    }

    // /////////////////////////////////////////////////////////
    // function
    // TODO: lab3, exercise 10.
    private Cfg.Function.T doitFunction(Cfg.Function.T func) {
        switch (func) {
            case Cfg.Function.Singleton(
                    Cfg.Type.T retType,
                    Id classId,
                    Id functionId,
                    List<Cfg.Dec.T> formals,
                    List<Cfg.Dec.T> locals,
                    List<Cfg.Block.T> blocks
            ) -> throw new Todo();
        }
    }

    // TODO: lab3, exercise 13.
    public Cfg.Program.T doitProgram(Cfg.Program.T prog) {
        switch (prog) {
            case Cfg.Program.Singleton(
                    Id mainClassId,
                    Id mainFuncId,
                    List<Cfg.Vtable.T> vtables,
                    List<Cfg.Struct.T> structs,
                    List<Cfg.Function.T> functions
            ) -> {
                HashMap<Object, Tuple.Two<HashSet<Cfg.Stm.T>, HashSet<Cfg.Stm.T>>>
                        liveInOutMap = new ReachDef().doitProgram(prog);
                var newFunctions =
                        functions.stream().map(this::doitFunction).toList();
                // TODO: your code here:
                //
                return prog;
            }
        }
    }
}
