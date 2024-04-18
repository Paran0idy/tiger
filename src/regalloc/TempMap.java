package regalloc;

import util.Id;

import java.util.HashMap;

public class TempMap {

    public static class Position {
        public sealed interface T
                permits InReg, InStack {
        }

        public record InReg(String reg) implements T {
        }

        public record InStack(int offset) implements T {
        }
    }

    // the data structure
    public HashMap<Id, Position.T> map;

    // constructors and methods
    TempMap() {
        this.map = new HashMap<>();
    }

    Position.T get(Id id) {
        return map.get(id);
    }

    void put(Id id, Position.T pos) {
        this.map.put(id, pos);
    }
}
