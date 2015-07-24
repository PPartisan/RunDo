package com.werdpressed.partisan.undoredo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayDeque;

/**
 * Custom class for use with the Undo/Redo function needed in EditorActivity. Thanks to contributors
 * on StackOverflow at <a href="http://stackoverflow.com/questions/30324358/which-collection-type-is-most-appropriate-for-a-constantly-updated-fixed-size-a/30324618#30324618">this</a>
 * post for the help.
 */
public class CustomArrayDeque<T> extends ArrayDeque<T> {

    private final int maxSize;

    public CustomArrayDeque(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public void addFirst(T t) {
        if (maxSize == size()) {
            removeLast();
        }
        super.addFirst(t);
    }

    @Override
    public void addLast(T t) {
        if (maxSize == size()) {
            removeFirst();
        }
        super.addLast(t);
    }
}