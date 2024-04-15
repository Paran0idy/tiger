package util;

public class Temp {
    private final int i;
    private static int count = 0;

    public Temp() {
        i = count++;
    }

    @Override
    public String toString() {
        return STR."x_\{this.i}";
    }
}

