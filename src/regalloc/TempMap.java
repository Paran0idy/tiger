package regalloc;

import java.util.HashMap;

public class TempMap {

    public static class Position {
        public sealed interface T
                permits InReg,
                InStack {
        }

        public record InReg(String reg) implements T {
        }

        public record InStack(int offset) implements T {
        }
    }

    // a data structure
    public HashMap<String, Position.T> map;

    //
    TempMap() {
        this.map = new HashMap<>();
    }

    Position.T get(String id) {
        return map.get(id);
    }

    void put(String id, Position.T pos) {
        this.map.put(id, pos);
    }
}
