package util;

public class Label {
    private final int i;
    private static int count = 0;

    public Label() {
        i = count++;
    }

    @Override
    public boolean equals(Object o) {
        return this.toString().equals(o.toString());
    }

    @Override
    public String toString() {
        return "L_" + (this.i);
    }
}

