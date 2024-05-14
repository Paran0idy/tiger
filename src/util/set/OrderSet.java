package util.set;

import util.Error;

import java.util.function.Function;

public class OrderSet<X> implements Set<X> {
    private final java.util.LinkedList<X> set;
    private final Function<X, Integer> getOrder;

    public OrderSet(Function<X, Integer> getIndex) {
        this.set = new java.util.LinkedList<>();
        this.getOrder = getIndex;
    }

    @SuppressWarnings("unchecked")
    private OrderSet(OrderSet<X> theSet) {
        try {
            this.set = (java.util.LinkedList<X>) theSet.set.clone();
        } catch (Exception e) {
            throw new Error(e);
        }
        this.getOrder = theSet.getOrder;
    }

    // s \/ {data}
    public void add(X data) {
        int theOrder = getOrder.apply(data);
        for (int i = 0; i < set.size(); i++) {
            int currentOrder = getOrder.apply(set.get(i));
            if (currentOrder == theOrder)
                // the target data is already here
                return;
            if (currentOrder < theOrder)
                continue;
            // ">"
            set.add(i, data);
        }
    }

    // s1 \/ s2
    public void union(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        for (X target : targetSet.set) {
            this.add(target);
        }
    }

    // s1 - s2
    public void sub(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        for (X target : targetSet.set) {
            this.set.remove(target);
        }
    }

    public Set<X> getClone() {
        return new OrderSet<>(this);
    }

    public boolean isSame(Set<X> theSet) {
        var targetSet = (OrderSet<X>) theSet;
        if (this.set.size() != targetSet.set.size())
            return false;
        for (int i = 0; i < this.set.size(); i++) {
            if (!this.getOrder.apply(this.set.get(i)).equals(
                    targetSet.getOrder.apply(targetSet.set.get(i))))
                return false;
        }
        return true;
    }
}
