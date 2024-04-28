package regalloc;

import codegen.X64;
import control.Control;

// Allocator dispatcher.
public class Allocator {

    public X64.Program.T allocProgram(X64.Program.T x64) {
        return switch (Control.Allocator.strategy) {
            case Linear -> new LinearScan().allocProgram(x64);
            case Stack -> new StackAllocator().allocProgram(x64);
        };
    }
}

