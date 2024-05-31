package util.set;

// an abstract imperative set interface.
// with several concrete implementations:
//   1. HashSet (in HashSet.java): a hash-based set;
//   2. BitSet (in BitSet.java): a bit-vector-based set; and
//   3. OrderSet (in OrderSet.java): an ordered set.
public interface Set<X> {
    // s \/ {data}
    void add(X data);

    // s - {data}
    void remove(X data);

    // s1 \/ s2
    void union(Set<X> theSet);

    // s1 - s2
    void sub(Set<X> theSet);

    // We use copy constructor instead of clone, see:
    //   "Copy Constructor versus Cloning"
    // in:
    // https://www.artima.com/articles/josh-bloch-on-design#part13
    Set<X> getClone();

    boolean isSame(Set<X> theSet);
}
