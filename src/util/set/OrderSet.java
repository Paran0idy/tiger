package util.set;

import util.Error;

import java.util.function.Function;

public class OrderSet<X> implements Set<X> {
    private final java.util.LinkedList<X> theList;
    private final Function<X, Integer> getOrder;

    public OrderSet(Function<X, Integer> getIndex) {
        this.theList = new java.util.LinkedList<>();
        this.getOrder = getIndex;
    }

    @SuppressWarnings("unchecked")
    private OrderSet(OrderSet<X> theSet) {
        try {
            this.theList = (java.util.LinkedList<X>) theSet.theList.clone();
        } catch (Exception e) {
            throw new Error(e);
        }
        this.getOrder = theSet.getOrder;
    }

    // s \/ {data}
    public void add(X data) {
        int theOrder = getOrder.apply(data);
        for (int i = 0; i < theList.size(); i++) {
            int currentOrder = getOrder.apply(theList.get(i));
            if (currentOrder == theOrder)
                // the target data is already here
                return;
            if (currentOrder < theOrder)
                continue;
            // ">"
            theList.add(i, data);
            return;
        }
        theList.add(data);
    }

    // s - {data}
    public void remove(X data) {
        int theOrder = getOrder.apply(data);
        for (int i = 0; i < theList.size(); i++) {
            int currentOrder = getOrder.apply(theList.get(i));
            if (currentOrder == theOrder) {
                // the target data is already here
                theList.remove(i);
                return;
            }
        }
    }

    // s1 \/ s2
    public void union(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        for (X target : targetSet.theList) {
            this.add(target);
        }
    }

    // s1 - s2
    public void sub(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        for (X target : targetSet.theList) {
            this.theList.remove(target);
        }
    }

    public Set<X> getClone() {
        return new OrderSet<>(this);
    }

    public boolean isSame(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        if (this.theList.size() != targetSet.theList.size())
            return false;
        for (int i = 0; i < this.theList.size(); i++) {
            if (!this.getOrder.apply(this.theList.get(i)).equals(
                    targetSet.getOrder.apply(targetSet.theList.get(i))))
                return false;
        }
        return true;
    }
}
