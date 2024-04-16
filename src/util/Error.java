package util;

public class Error extends AssertionError {
    public Error() {
        super();
        System.out.println("Compiler error");
    }

    public Error(Object obj) {
        super(obj);
        System.out.println("Compiler error");
    }
}
