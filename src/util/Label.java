package util;

public class Label {
    private final int i;
    private static int count = 0;

    public Label() {
        this.i = count++;
    }

    @Override
    public String toString() {
        return STR."L_\{this.i}";
    }
}

