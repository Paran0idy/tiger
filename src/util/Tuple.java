package util;

// tuple of diverse arity.
public class Tuple {

    // one
    public static class One<X> {
        private X data;

        public One() {
            this.data = null;
        }

        public One(X data) {
            this.data = data;
        }

        public X get() {
            return data;
        }

        public void set(X data) {
            this.data = data;
        }
    }

    // two
    public record Two<X, Y>(X first, Y second) {
    }

    // three
    public record Three<X, Y, Z>(X first, Y second, Z third) {
    }
}
