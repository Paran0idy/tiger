package util;

public class Error extends AssertionError {
    public Error() {
        super();
        System.out.println("Compiler bug!\n");
    }

    public Error(Object o) {
        super(o);
        System.out.println("Compiler bug!\n");
    }
}

