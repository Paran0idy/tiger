package util.set;

import util.Error;

public class HashSet<X> implements Set<X> {
    // the straightforward set representation
    // do not confuse with the standard "java.util.set.HashSet"
    private final java.util.HashSet<X> set;

    public HashSet() {
        this.set = new java.util.HashSet<>();
    }

    @SuppressWarnings("unchecked")
    private HashSet(HashSet<X> theSet) {
        try {
            this.set = (java.util.HashSet<X>) theSet.set.clone();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // s \/ {data}
    public void add(X data) {
        this.set.add(data);
    }

    // s1 \/ s2
    public void union(Set<X> theSet) {
        var targetSet = (HashSet<X>) theSet;
        this.set.addAll(targetSet.set);
    }

    // s1 - s2
    public void sub(Set<X> theSet) {
        var targetSet = (HashSet<X>) theSet;
        this.set.removeAll(targetSet.set);
    }

    public Set<X> getClone() {
        return new HashSet<>(this);
    }

    public boolean isSame(Set<X> theSet) {
        var targetSet = (HashSet<X>) theSet;
        if (this.set.size() != targetSet.set.size())
            return false;
        for (X data : this.set) {
            if (!targetSet.set.contains(data))
                return false;
        }
        return true;
    }
}
