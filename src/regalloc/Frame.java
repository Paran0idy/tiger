package regalloc;

import codegen.X64;
import util.Id;

// to allocate a variable in a frame
public class Frame {
    // whose frame, for debugging purpose
    Id funcId;
    int currentPos;

    Frame(Id funcId) {
        this.funcId = funcId;
        this.currentPos = 0;
    }

    public int alloc() {
        this.currentPos += X64.WordSize.bytesOfWord;
        return this.currentPos;
    }

    public int size() {
        return currentPos;
    }
}
