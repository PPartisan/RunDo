package com.werdpressed.partisan.rundo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayDeque;

/**
 * Implementation of <code>ArrayDeque</code> class that adds a fixed, maximum capacity.
 *
 * Thanks to contributors on StackOverflow at <a href="http://stackoverflow.com/questions/30324358/which-collection-type-is-most-appropriate-for-a-constantly-updated-fixed-size-a/30324618#30324618">this</a> post.
 *
 * @author Tom Calver
 */
public final class CustomArrayDeque<T> extends ArrayDeque<T> implements Parcelable {

    /**
     * Max capacity for <code>ArrayDeque</code>.
     */
    private final int maxSize;

    /**
     * Creates <code>ArrayDeque</code> with fixed, maximum capacity of <code>maxSize</code>.
     *
     * @param maxSize Max capacity for <code>ArrayDeque</code>.
     */
    public CustomArrayDeque(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    protected CustomArrayDeque(Parcel in) {
        maxSize = in.readInt();
    }

    public static final Creator<CustomArrayDeque> CREATOR = new Creator<CustomArrayDeque>() {
        @Override
        public CustomArrayDeque createFromParcel(Parcel in) {
            return new CustomArrayDeque(in);
        }

        @Override
        public CustomArrayDeque[] newArray(int size) {
            return new CustomArrayDeque[size];
        }
    };

    /**
     * Inserts the specified element at the front of the deque
     * @param t The element to add
     */
    @Override
    public void addFirst(T t) {
        if (maxSize == size()) {
            removeLast();
        }
        super.addFirst(t);
    }

    /**
     * Inserts the specified element at the end of the deque
     * @param t The element to add
     */
    @Override
    public void addLast(T t) {
        if (maxSize == size()) {
            removeFirst();
        }
        super.addLast(t);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(maxSize);
    }
}