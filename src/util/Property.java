package util;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

// X: the target element
// Y: the value associated with the element
public class Property<X, Y> {
    private final Function<X, Plist> getPlist;
    private final List<Plist> allPlists;

    public Property(Function<X, Plist> getPlist) {
        this.getPlist = getPlist;
        this.allPlists = new LinkedList<>();
    }

    private void rememberPlist(Plist plist) {
        // we may remember a plist multiple times
        this.allPlists.add(plist);
    }

    // if the property is not present, issue errors
    public Y get(X element) {
        Plist plist = getPlist.apply(element);
        Object value = plist.get(this);
        if (value == null)
            throw new NoSuchElementException();
        value.getClass().cast(value);
        return (Y) value;
    }

    // if the property is not present, then put the "constantValue"
    // onto this element before returning it.
    public Y getInitConst(X element, Y constantValue) {
        Plist plist = getPlist.apply(element);
        Object value = plist.get(this);
        if (value != null)
            return (Y) value;
        plist.put(this, constantValue);
        this.allPlists.add(plist);
        return constantValue;
    }

    // if the value is not present, then put the function result
    // of "fun" onto this element before returning it.
    public Y getInitFun(X element, Function<X, Y> fun) {
        Plist plist = getPlist.apply(element);
        Object value = plist.get(this);
        if (value == null) {
            value = fun.apply(element);
            plist.put(this, value);
            this.allPlists.add(plist);
        }
        return (Y) value;
    }

    // it does not matter whether the property is on the element
    public void put(X element, Y value) {
        Plist plist = getPlist.apply(element);
        plist.put(this, value);
        this.allPlists.add(plist);
    }

    public void clear() {
        for (Plist plist : allPlists) {
            plist.clear(this);
        }
        this.allPlists.clear();
        ;
    }
}
