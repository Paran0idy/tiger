package regalloc;

import codegen.X64;
import control.Control;

// Allocator dispatcher.
public class Allocator {

    public X64.Program.T doitProgram(X64.Program.T x64) {
        return switch (Control.Allocator.strategy) {
            case Linear -> new LinearScan().doitProgram(x64);
            case Stack -> new StackAllocator().doitProgram(x64);
        };
    }
}

