package regalloc;

import codegen.X64;

// to allocate a new variable in a frame
public class Frame {
    // whose frame, for debugging purpose
    String funcName;
    int offset;

    Frame(String funcName) {
        this.funcName = funcName;
        this.offset = 0;
    }

    public int alloc() {
        this.offset -= X64.WordSize.bytesOfWord;
        return this.offset;
    }

    
    public int size() {
        return -offset;
    }
}
