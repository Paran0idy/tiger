package util;

import java.util.HashMap;

public class Plist<X, Y> {
    private final HashMap<X, Y> map;

    public Plist() {
        this.map = new HashMap<>();
    }

    public void put(X x, Y y) {
        this.map.put(x, y);
    }

    public Y get(X x) {
        return this.map.get(x);
    }

}
