package util;

import java.io.Serializable;
import java.util.HashMap;

public class Plist implements Serializable {
    private final HashMap<Object, Object> map;

    public Plist() {
        this.map = new HashMap<>();
    }

    public void put(Object key, Object value) {
        this.map.put(key, value);
    }

    public Object get(Object key) {
        return this.map.get(key);
    }

    public void clear(Object key) {
        this.map.remove(key);
    }
}