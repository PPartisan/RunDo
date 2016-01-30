package com.werdpressed.partisan.rundo;

import android.os.Parcel;
import android.os.Parcelable;

import com.werdpressed.partisan.rundo.utils.SubtractStringUtils;

import java.util.Arrays;

final class SubtractStrings {

    @SuppressWarnings("unused")
    private static final String TAG = "SubtractStrings";

    static final int ADDITION = 55023;
    static final int REPLACEMENT = ADDITION + 1;
    static final int DELETION = ADDITION + 2;
    static final int UNCHANGED = ADDITION + 3;

    private final char[] mOldText, mNewText;

    private int firstDeviation = -1;
    private int lastDeviationOldText = -1;
    private int lastDeviationNewText = -1;
    private int deviationType = -1;

    private Item mItem = null;

    public SubtractStrings(String oldString, String newString) {

        mOldText = oldString.toCharArray();
        mNewText = newString.toCharArray();

    }

    /**
     * Calculates the first point of deviation between old and new text, by comparing each individual
     * char from beginning to end.
     *
     * @see #getFirstDeviation()
     */
    private int findFirstDeviation() {

        if (isOldTextEqualToNewText()) return 0;

        int shortestLength = findShortestLength();

        for (int i = 0; i < shortestLength; i++) {
            if (mOldText[i] != mNewText[i]) {
                shortestLength = i;
                break;
            }
        }

        return shortestLength;
    }

    /**
     * Calculates the last point of deviation by reversing old and new text, and repeating the same
     * process as {@link #findFirstDeviation()}. Different values are assigned for old and new text
     * in order to effectively calculate what has changed between the two. This is especially
     * relevant when text is replaced.
     *
     * Under certain circumstances, running both this and {@link #findFirstDeviation()} alone will
     * produce incorrect results, especially when words are duplicated. For example:
     *
     * <pre>
     *     {@code
     *     int firstDeviation, lastDeviation;
     *
     *     char[] mOldText = new String("one").toCharArray();
     *     char[] mNewText = new String("one one").toCharArray();
     *
     *     findFirstDeviation();
     *     findLastDeviation();
     *
     *     String output = new String(mNewText).subString(firstDeviation, lastDeviation);
     *
     *     //firstDeviation will equal 3, last deviation 4, and "output" will be " ".
     *     }
     * </pre>
     *
     * This is because the first deviation comes after the first "e", at index 3, yet when the
     * arrays are reversed, the "e" at the end of "one" in mOldText shifts to index 0. It is
     * effectively counted twice:
     *
     * <pre>
     *     {@code
     *     char[] mOldText = new char[]{ 'o', 'n', 'e' }
     *     char[] mNewText = new char[]{ 'o', 'n', 'e', ' ', 'o', 'n', 'e' };
     *
     *     mOldTextReversed = new char[]{ 'e', 'n', 'o' };
     *     mNewTextReversed = new char[]{ 'e', 'n', 'o', ' ', 'e', 'n', 'o'};
     *     }
     * </pre>
     *
     * lastDeviation values are adjusted in {@link #findLastDeviationOffsetSize(char[], char[], int)}
     * to account for such situations.
     *
     * @return Last point of deviation between old and new text, in relation to old text.
     *
     * @see #findLastDeviationNewText()
     * @see #getLastDeviationOldText()
     * @see Item#getLastDeviationOldText()
     */
    private int findLastDeviationOldText() {

        if (isOldTextEqualToNewText()) return 0;

        final int shortestLength = findShortestLength();

        final char[] oldTextReversed = SubtractStringUtils.reverseCharArray(mOldText);
        final char[] newTextReversed = SubtractStringUtils.reverseCharArray(mNewText);

        int tempLastDeviation = shortestLength;
        int difference = Math.abs((mNewText.length - mOldText.length));

        for (int i = 0; i < shortestLength; i++) {
            if (oldTextReversed[i] != newTextReversed[i]) {
                tempLastDeviation = i;
                break;
            }
        }

        int lastDeviation =
                findLastDeviationOffsetSize(oldTextReversed, newTextReversed, tempLastDeviation);

        return (isNewTextLonger() ? (lastDeviation - difference) : lastDeviation);

    }

    /**
     * Identical process to {@link #findLastDeviationOldText()}, except value returned is in
     * relation to new text.
     *
     * @return Last point of deviation between old and new text, in relation to new text.
     *
     * @see #findLastDeviationOldText()
     * @see #getLastDeviationNewText()
     * @see Item#getLastDeviationNewText()
     */
    private int findLastDeviationNewText() {

        if (isOldTextEqualToNewText()) return 0;

        final int shortestLength = findShortestLength();

        final char[] oldTextReversed = SubtractStringUtils.reverseCharArray(mOldText);
        final char[] newTextReversed = SubtractStringUtils.reverseCharArray(mNewText);

        int tempLastDeviation = shortestLength;
        int difference = Math.abs((mNewText.length - mOldText.length));

        for (int i = 0; i < shortestLength; i++) {
            if (oldTextReversed[i] != newTextReversed[i]) {
                tempLastDeviation = i;
                break;
            }
        }

        int lastDeviation =
                findLastDeviationOffsetSize(oldTextReversed, newTextReversed, tempLastDeviation);

        return (isNewTextLonger() ? lastDeviation : (lastDeviation - difference));

    }

    /**
     * Adjusts the last point at which the two {@code char[]} diverge, due to the reasons outlined in
     * {@link #findLastDeviationOldText()}. This is achieved by calculating the difference in length between
     * the old and new text, and comparing each {@code char} from this final end point to the char at the
     * same position less the offset difference. If the same value is found, then the current
     * index is used to determine the true last deviation value. For example:
     *
     * <pre>
     *     {@code
     *     mOldTextReversed = new char[]{ 'e', 'n', 'o' };
     *     mNewTextReversed = new char[]{ 'e', 'n', 'o', ' ', 'e', 'n', 'o'};
     *     }
     * </pre>
     *
     * In this case, the potential offset size (the length of the longest array subtracted from the
     * shortest) is {@code(7 - 3) = 4}. Thus, the char at index [4] of the longest array, which is
     * 'e', is compared to the char at index[0] (4 - (potentialOffsetSize of 4) = 0), which is also
     * 'e'. As the two values match, the final value returned is
     * (length of the longest array - (current index - potential offset size)), which translates
     * to (7 - (4 - 0)) = 3.
     *
     * @param oldText Reversed old text
     * @param newText Reversed new text
     * @param tempLastDeviation The current last deviation figure. This is the value that will be
     *                          checked and possibly altered before being returned.
     * @return The adjusted last deviation value.
     */
    private int findLastDeviationOffsetSize(char[] oldText, char[] newText, int tempLastDeviation) {

        final char[] longestArray = (isNewTextLonger()) ? newText : oldText;
        final int potentialOffsetSize = (newText.length - oldText.length);

        boolean isOffsetWithinArrayBounds =
                ((tempLastDeviation + potentialOffsetSize) < longestArray.length);

        final int maxValue = (isOffsetWithinArrayBounds)
                ? (tempLastDeviation + potentialOffsetSize)
                : longestArray.length;

        final int reverseDeviation = (tempLastDeviation < potentialOffsetSize)
                ? potentialOffsetSize
                : tempLastDeviation;

        for (int i = reverseDeviation; i < maxValue; i++) {

            if (longestArray[i] == longestArray[i - reverseDeviation]) {
                return (longestArray.length - (i - reverseDeviation));
            }
        }

        return findLongestLength();

    }

    private int findLastDeviationOldTextFromNew() {
        final int difference = findLengthDifference();
        return (isNewTextLonger())
                ? (lastDeviationNewText - difference)
                : (lastDeviationNewText + difference);
    }

    private int findLastDeviationNewTextFromOld() {
        final int difference = findLengthDifference();
        return (isNewTextLonger())
                ? (lastDeviationOldText + difference)
                : (lastDeviationOldText - difference);
    }


    /**
     * Populates the {@link #deviationType} field with one of three constant values,
     * representing an ADDITION of text, from old to new, with no text from old replaced. DELETION,
     * showing a removal of text from old to new, in which no text was replaced, and a REPLACEMENT,
     * in which text has been either added or removed from old to new, and overwritten the old text
     * in part or in its entirety. For example:
     *
     * <b>ADDITION:</b> The difference between "one" and "one two".
     *
     * <b>DELETION:</b> The difference between "one two" and "one".
     *
     * <b>REPLACEMENT:</b> The difference between "one" and "two".
     *
     * @see #getDeviationType()
     */
    private int findDeviationType() {

        if (firstDeviation == -1) getFirstDeviation();

        int deviationType;

        if (isNewTextLonger()) {

            if (lastDeviationNewText == -1) getLastDeviationNewText();

            deviationType = (SubtractStringUtils.isArrayEqualWithOmission(
                    mNewText, mOldText, firstDeviation, lastDeviationNewText)
            )
                    ? ADDITION
                    : REPLACEMENT;
        } else if(isTextLengthEqual()) {
            deviationType = (isOldTextEqualToNewText())
                    ? UNCHANGED
                    : REPLACEMENT;
        } else {

            if (lastDeviationOldText == -1) getLastDeviationOldText();

            deviationType = (SubtractStringUtils.isArrayEqualWithOmission(
                    mNewText, mOldText, firstDeviation, lastDeviationOldText)
            )
                    ? DELETION
                    : REPLACEMENT;
        }

        return deviationType;

    }

    private boolean isOldTextEqualToNewText() {
        return Arrays.equals(mOldText, mNewText);
    }

    private boolean isNewTextLonger() {
        return (mNewText.length > mOldText.length);
    }

    private boolean isTextLengthEqual() {
        return (mNewText.length == mOldText.length);
    }

    private int findLongestLength() {
        return Math.max(mNewText.length, mOldText.length);
    }

    private int findShortestLength() {
        return Math.min(mNewText.length, mOldText.length);
    }

    private int findLengthDifference() {
        return findLongestLength() - findShortestLength();
    }

    /**
     *
     * @return First deviation
     */
    int getFirstDeviation() {

        if (firstDeviation == -1) {
            firstDeviation = findFirstDeviation();
        }

        return  firstDeviation;
    }

    int getLastDeviationOldText() {

        if (lastDeviationOldText == -1) {
            lastDeviationOldText = (lastDeviationNewText == -1 )
            ? findLastDeviationOldText()
            : findLastDeviationOldTextFromNew();
        }

        return lastDeviationOldText;
    }

    int getLastDeviationNewText() {

        if (lastDeviationNewText == -1) {
            lastDeviationNewText = (lastDeviationOldText == -1)
                    ? findLastDeviationNewText()
                    : findLastDeviationNewTextFromOld();
        }

        return lastDeviationNewText;
    }

    /**
     *
     * @return Deviation type, in the form of an int value. For a String representation, use
     * {@link #valueOfDeviation(int)} ()}
     */
    int getDeviationType() {
        if (deviationType == -1) {
            deviationType = findDeviationType();
        }
        return deviationType;
    }

    /**
     * Converts {@code int} value returned by {@link #getDeviationType()} to {@link String}
     * representation.
     * @param deviationType Value return from {@link #getDeviationType()}
     * @return String representation of argument if argument is valid. Otherwise, {@code null}
     */
    @SuppressWarnings("unused")
    public static String valueOfDeviation(int deviationType) {

        switch (deviationType) {
            case ADDITION:
                return "Addition";
            case DELETION:
                return "Deletion";
            case REPLACEMENT:
                return "Replacement";
            case UNCHANGED:
                return "Unchanged";
            default:
                return null;
        }

    }

    /**
     *
     * @return If text has been added or replaced, returns a substring of the new text that has
     * been altered in respect to old text.
     */
    public String getAlteredText() {

        if (firstDeviation == -1) getFirstDeviation();
        if (lastDeviationNewText == -1) getLastDeviationNewText();

        switch (deviationType) {
            case ADDITION:
            case REPLACEMENT:
                return new String(mNewText).substring(firstDeviation, lastDeviationNewText);
        }

        return "";
    }

    /**
     *
     * @return If text has been deleted or replaced, returns a substring of the old text that has
     * been removed or overwritten.
     */
    public String getReplacedText() {

        if (firstDeviation == -1) getFirstDeviation();
        if (lastDeviationOldText == -1) getLastDeviationOldText();

        switch (deviationType) {
            case DELETION:
            case REPLACEMENT:
                return new String(mOldText).substring(firstDeviation, lastDeviationOldText);
        }

        return "";

    }

    /**
     *
     * @return Encapsulates all important information relating to the differences between old and
     * new text.
     * @see com.werdpressed.partisan.rundo.SubtractStrings.Item
     */
    public Item getItem() {

        if (mItem == null) {
            mItem = new Item(
                    getFirstDeviation(),
                    getLastDeviationOldText(),
                    getLastDeviationNewText(),
                    getDeviationType(),
                    getReplacedText(),
                    getAlteredText()
            );
        }

        return mItem;
    }

    /**
     * Model class which encapsulates all important pieces of information relating to the differences
     * between two {@link String}s, calculated by {@link SubtractStrings}
     */
    static final class Item implements Parcelable {

        private final int firstDeviation, lastDeviationOldText, lastDeviationNewText, deviationType;
        private final String replacedText, alteredText;

        Item(
                int firstDeviation,
                int lastDeviationOldText,
                int lastDeviationNewText,
                int deviationType,
                String replacedText,
                String alteredText
        ) {
            this.firstDeviation = firstDeviation;
            this.lastDeviationOldText = lastDeviationOldText;
            this.lastDeviationNewText = lastDeviationNewText;
            this.deviationType = deviationType;
            this.replacedText = replacedText;
            this.alteredText = alteredText;
        }

        protected Item(Parcel in) {
            firstDeviation = in.readInt();
            lastDeviationOldText = in.readInt();
            lastDeviationNewText = in.readInt();
            deviationType = in.readInt();
            replacedText = in.readString();
            alteredText = in.readString();
        }

        /**
         *
         * @return First point of deviation between old and new text.
         */
        public int getFirstDeviation() {
            return firstDeviation;
        }

        /**
         *
         * @return Last point of deviation between old and new text, in relation to old text.
         */
        public int getLastDeviationOldText() {
            return lastDeviationOldText;
        }

        /**
         *
         * @return Last point of deviation between old and new text, in relation to new text.
         */
        public int getLastDeviationNewText() {
            return lastDeviationNewText;
        }

        /**
         *
         * @return Deviation type in the form of an {@code int}. Value will correlate to
         * {@link #ADDITION}. {@link #REPLACEMENT}, {@link #DELETION} or {@link #UNCHANGED}. For
         * a {@link String} representation, use {@link #valueOfDeviation(int)}
         *
         * @see #valueOfDeviation(int)
         */
        public int getDeviationType() {
            return deviationType;
        }

        /**
         *
         * @return Section of old text replaced by new text, if applicable.
         */
        public String getReplacedText() {
            return replacedText;
        }

        /**
         *
         * @return Section of new text replaced by old text, if applicable.
         */
        public String getAlteredText() {
            return alteredText;
        }

        public static final Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(firstDeviation);
            dest.writeInt(lastDeviationOldText);
            dest.writeInt(lastDeviationNewText);
            dest.writeInt(deviationType);
            dest.writeString(replacedText);
            dest.writeString(alteredText);
        }
    }
}
