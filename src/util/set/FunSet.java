package util.set;

import util.Error;

// a functional set.
public class FunSet<X> {
    private final java.util.HashSet<X> set;

    public FunSet() {
        this.set = new java.util.HashSet<>();
    }

    @SuppressWarnings("unchecked")
    private FunSet(FunSet<X> theSet) {
        try {
            this.set = (java.util.HashSet<X>) theSet.set.clone();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // s \/ {data}
    public FunSet<X> add(X data) {
        var targetSet = new FunSet<>(this);
        targetSet.add(data);
        return targetSet;
    }

    // s1 \/ s2
    public FunSet<X> union(FunSet<X> theSet) {
        var targetSet = new FunSet<>(this);
        targetSet.set.addAll(theSet.set);
        return targetSet;
    }

    // s1 - s2
    public FunSet<X> sub(FunSet<X> theSet) {
        var targetSet = new FunSet<>(this);
        targetSet.set.removeAll(theSet.set);
        return targetSet;
    }

    public FunSet<X> getClone() {
        return new FunSet<>(this);
    }

    public boolean isSame(FunSet<X> theSet) {
        if (this.set.size() != theSet.set.size())
            return false;
        for (X data : this.set) {
            if (!theSet.set.contains(data))
                return false;
        }
        return true;
    }
}
