package util;

import java.util.HashMap;

public class Id {
    private static int gCounter = 0;
    // this uniquely identifies an Id
    private final int counter;
    // null, if no source names
    private String origName;
    private static final String prefix = "%x_";
    // map original names to its Id
    private static final HashMap<String, Id> allIds = new HashMap<>();
    // a per-id map storing attributes of an id
    // this implements the famous "plist", see:
    // https://www.cs.cmu.edu/Groups/AI/html/cltl/clm/node108.html
    private final Plist plist;

    // the singleton design pattern
    private Id() {
        this.counter = gCounter++;
        this.origName = null;
        this.plist = new Plist();
    }

    private Id(String srcName) {
        this.counter = gCounter++;
        this.origName = srcName;
        this.plist = new Plist();
        allIds.put(this.origName, this);
    }

    // "id" without original names
    public static Id newNoname() {
        Id id = new Id();
        allIds.put(id.toString(), id);
        return id;
    }

    public static Id newName(String origName) {
        Id id = allIds.get(origName);
        if (id == null) {
            id = new Id(origName);
            allIds.put(origName, id);
        }
        return id;
    }

    // create an id with the same original name, but different counter
    // that is, the id is considered be a fresh one.
    public Id newSameOrigName() {
        if (this.origName == null) {
            throw new Error("no original name");
        }
        Id fresh = newNoname();
        fresh.origName = this.origName;
        return fresh;
    }

    public Plist getPlist() {
        return this.plist;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Id))
            return false;
        return this.counter == ((Id) o).counter;
    }

    @Override
    public int hashCode() {
        return this.counter;
    }

    private boolean dumpId = true;

    @Override
    public String toString() {
        if (this.origName == null)
            return STR."\{prefix}\{this.counter}";
        if (dumpId) {
            return STR."\{this.origName}(\{prefix}\{this.counter})";
        }
        return STR."\{this.origName}";
    }

}
