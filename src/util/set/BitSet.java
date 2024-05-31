package util.set;

import util.Error;

import java.util.function.Function;

public class BitSet<X> implements Set<X> {
    // do not confuse with the standard "java.util.BitSet"
    private final java.util.BitSet set;
    private final Function<X, Integer> getIndex;

    public BitSet(Function<X, Integer> getIndex) {
        this.getIndex = getIndex;
        this.set = new java.util.BitSet();
    }

    private BitSet(BitSet<X> theSet) {
        try {
            this.set = (java.util.BitSet) theSet.set.clone();
        } catch (Exception e) {
            throw new Error(e);
        }
        this.getIndex = theSet.getIndex;
    }

    // s \/ {data}
    public void add(X data) {
        this.set.set(this.getIndex.apply(data));
    }

    // s - {data}
    public void remove(X data) {
        this.set.clear(this.getIndex.apply(data));
    }

    // s1 \/ s2
    public void union(Set<X> theSet) {
        var targetSet = (BitSet<X>) theSet;
        this.set.or(targetSet.set);
    }

    // s1 - s2
    public void sub(Set<X> theSet) {
        var targetSet = (BitSet<X>) theSet;
        this.set.andNot(targetSet.set);
    }

    public Set<X> getClone() {
        return new BitSet<>(this);
    }

    public boolean isSame(Set<X> theSet) {
        var targetSet = (BitSet<X>) theSet;
        if (this.set.size() != targetSet.set.size())
            return false;
        for (int i = 0; i < this.set.size(); i++) {
            if (this.set.get(i) != targetSet.set.get(i))
                return false;
        }
        return true;
    }
}
