package cfg;

import java.util.HashSet;

public class Set<X> {
    // the straightforward set representation
    private HashSet<X> set;

    public Set() {
        this.set = new HashSet<>();
    }

    public void add(X data) {
        this.set.add(data);
    }

    public void union(Set<X> theSet) {
        this.set.addAll(theSet.set);
    }

    public void sub(Set<X> theSet) {
        this.set.removeAll(theSet.set);
    }

    @SuppressWarnings("unchecked")
    public Set<X> clone() {
        Set<X> result = new Set<>();
        result.set = (HashSet<X>) this.set.clone();
        return result;
    }

    public boolean isSame(Set<X> theSet) {
        if (this.set.size() != theSet.set.size())
            return false;
        for (X data : this.set) {
            if (!theSet.set.contains(data))
                return false;
        }
        return true;
    }
}
