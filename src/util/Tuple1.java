package util;

// this is a one-element tuple to contain a singleton data.
public class Tuple1<X> {
    X data;

    public Tuple1() {
        this.data = null;
    }

    public Tuple1(X data) {
        this.data = data;
    }

    public X getData() {
        return data;
    }

    public void setData(X data) {
        this.data = data;
    }
}
