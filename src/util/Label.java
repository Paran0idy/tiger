package util;

import java.io.Serializable;

public class Label implements Serializable {
    private final int i;
    private static int gCounter = 0;
    private final Plist plist;


    public Label() {
        this.i = gCounter++;
        this.plist = new Plist();
    }

    public Plist getPlist() {
        return plist;
    }

    @Override
    public String toString() {
        return STR."L_\{this.i}";
    }
}

