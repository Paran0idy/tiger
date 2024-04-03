package util;

public class Todo extends AssertionError {
    public Todo() {
        super();
        System.out.println("TODO: please add your code here:\n");
    }

    public Todo(Object o) {
        super(o);
        System.out.println("TODO: please add your code here:\n");
    }
}

