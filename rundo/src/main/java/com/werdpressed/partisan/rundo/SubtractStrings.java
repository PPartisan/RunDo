package com.werdpressed.partisan.rundo;

import java.util.Arrays;

//Immutable
final class SubtractStrings {

    private static final String TAG = "SubtractStrings";

    static final int ADDITION = 55023;
    static final int REPLACEMENT = ADDITION + 1;
    static final int DELETION = REPLACEMENT + 1;
    static final int UNCHANGED = DELETION + 1;

    private final char[] mOldText, mNewText;
    private final char[] mOldTextReversed, mNewTextReversed;

    private int firstDeviation, lastDeviation, tempReverseDeviation;
    private int lastDeviationOldText, lastDeviationNewText;
    private int deviationType;
    private int shortestLength, longestLength;

    private final boolean isNewTextLonger, isTextLengthEqual;

    public SubtractStrings(String oldString, String newString) {

        mOldText = oldString.toCharArray();
        mNewText = newString.toCharArray();

        mOldTextReversed = reverseCharArray(mOldText);
        mNewTextReversed = reverseCharArray(mNewText);

        shortestLength = findShortestLength(mOldText, mNewText);
        longestLength = findLongestLength(mOldText, mNewText);

        isNewTextLonger = (mNewText.length > mOldText.length);
        isTextLengthEqual = (mNewText.length == mOldText.length);

        findDeviations();
    }

    private void findDeviations() {
        //check equality. If equal, exit early
        if (checkCharArraysEqual()) {
            return;
        }
        //populate first deviation
        findFirstDeviation();
        //find last deviation
        findLastDeviation();
        //Calculate offset that may occur on last deviation
        findLastDeviationOffset();
        //Find deviation type (Add, Delete, Replace)
        findDeviationType();
    }

    /**
     * Checks whether old and new text are equal. If they are equal, then no deviations are recorded.
     * @return True if new and old text are equal.
     */
    private boolean checkCharArraysEqual() {
        if (Arrays.equals(mOldText, mNewText)) {
            firstDeviation = lastDeviation = 0;
            deviationType = UNCHANGED;
            return true;
        }
        return false;
    }

    /**
     * Calculates the first point of deviation between old and new text, by comparing each individual
     * char from beginning to end.
     *
     * @see #getFirstDeviation()
     */
    private void findFirstDeviation() {

        for (int i = 0; i < shortestLength; i++) {
            if (mOldText[i] != mNewText[i]) {
                firstDeviation = i;
                return;
            }
        }

        firstDeviation = shortestLength;

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
     * lastDeviation values are adjusted in {@link #findLastDeviationOffset()} to account for such
     * situations.
     *
     * @see #getLastDeviation()
     *
     */
    private void findLastDeviation() {

        for (int i = 0; i < shortestLength; i++) {
            if (mOldTextReversed[i] != mNewTextReversed[i]) {
                tempReverseDeviation = i;
                lastDeviationNewText = (isNewTextLonger) ? (longestLength - i) : (shortestLength - i);
                lastDeviationOldText = (isNewTextLonger) ? (shortestLength - i) : (longestLength - i);
                lastDeviation = (isNewTextLonger) ? lastDeviationNewText : lastDeviationOldText;
                return;
            }
        }

        tempReverseDeviation = shortestLength;
        lastDeviation = (longestLength - shortestLength);
        lastDeviationNewText = (isNewTextLonger) ? (longestLength - shortestLength) : shortestLength;
        lastDeviationOldText = (isNewTextLonger) ? shortestLength : (longestLength - shortestLength);

    }

    /**
     * Takes the {@link #lastDeviation} value adjusted in {@link #findLastDeviationOffsetSize()}, and
     * applies to {@link #lastDeviationNewText} and {@link #lastDeviationOldText}
     */
    private void findLastDeviationOffset() {

        int deviationOffset = findLastDeviationOffsetSize();
        int offsetValue = longestLength - shortestLength;

        lastDeviationNewText = (isNewTextLonger) ? deviationOffset : deviationOffset - offsetValue;
        lastDeviationOldText = (isNewTextLonger) ? deviationOffset - offsetValue : deviationOffset;

    }

    /**
     * Adjusts the last point at which the two char[] diverge, due to the reasons outlined in
     * {@link #findLastDeviation()}. This is achieved by calculating the difference in length between
     * the old and new text, and comparing each char from this final end point to the char at the
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
     * 'e'. As the two values match, the final value assigned to {@link #lastDeviation} is
     * (length of the longest array - (current index - potential offset size)), which translates
     * to (7 - (4 - 0)) = 3.
     *
     * @return The adjusted last deviation value.
     */
    private int findLastDeviationOffsetSize() {

        final char[] longestArray = (isNewTextLonger) ? mNewTextReversed : mOldTextReversed;

        final int potentialOffsetSize = longestLength - shortestLength;

        boolean isOffsetWithinArrayBounds =
                ((tempReverseDeviation + potentialOffsetSize) < longestLength);

        final int maxValue = (isOffsetWithinArrayBounds)
                ? (tempReverseDeviation + potentialOffsetSize)
                : longestLength;

        final int reverseDeviation = (tempReverseDeviation < potentialOffsetSize)
                ? potentialOffsetSize
                : tempReverseDeviation;

        for (int i = reverseDeviation; i < maxValue; i++) {

            if (longestArray[i] == longestArray[i - reverseDeviation]) {
                return (longestLength - (i - potentialOffsetSize));
            }

        }

        if (longestLength == mNewText.length) {
            isOffsetWithinArrayBounds = ((lastDeviationNewText < firstDeviation));
            lastDeviation = (isOffsetWithinArrayBounds)
                    ? (lastDeviationNewText + potentialOffsetSize)
                    : lastDeviationNewText;
            return lastDeviation;
        } else {
            isOffsetWithinArrayBounds = ((lastDeviationOldText < firstDeviation));
            lastDeviationOldText = (isOffsetWithinArrayBounds)
                    ? (lastDeviationNewText + potentialOffsetSize)
                    : lastDeviationNewText;
            return lastDeviation;
        }

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
    private void findDeviationType() {

        if (isNewTextLonger) {
            deviationType = (isArrayEqualWithOmission(mNewText, mOldText, firstDeviation, lastDeviationNewText))
                    ? ADDITION
                    : REPLACEMENT;
        } else if(isTextLengthEqual) {
            deviationType = REPLACEMENT;
        } else {
            deviationType = (isArrayEqualWithOmission(mNewText, mOldText, firstDeviation, lastDeviationOldText))
                    ? DELETION
                    : REPLACEMENT;
        }

    }

    private static int findShortestLength(char[] arrOne, char[] arrTwo) {

        return Math.min(arrOne.length, arrTwo.length);

    }

    private static int findLongestLength(char[] arrOne, char[] arrTwo) {

        return Math.max(arrOne.length, arrTwo.length);

    }

    private static char[] reverseCharArray(char[] input){

        char[] output = input.clone();
        char temp;
        int index = 0;

        for (int i = (output.length - 1); i >= (output.length/2); i--) {
            temp = input[i];
            output[i] = input[index];
            output[index] = temp;
            index++;
        }

        return output;
    }

    /**
     * Determines whether the contents of two arrays are equal, after a section from one array is
     * removed.
     *
     * Used to determine whether text has been replaced, or added to/deleted from.
     *
     * @param arrOne First array
     * @param arrTwo Second array
     * @param omissionStart Start index of section to remove from the longer or arrOne and arrTwo.
     * @param omissionEnd End index of section to remove.
     * @return True if both arrOne and arrTwo are equal after section specified in omissionStart
     * and omissionEnd is removed from the longer of the two.
     *
     * @see #findDeviationType()
     */
    private static boolean isArrayEqualWithOmission(char[] arrOne, char[] arrTwo, int omissionStart, int omissionEnd) {

        final boolean isArrOneLonger = (arrOne.length > arrTwo.length);

        final char[] arrOneCopy = (isArrOneLonger)
                ? omitCharArrayEntriesAtIndexes(arrOne.clone(), omissionStart, omissionEnd)
                : arrOne.clone();

        final char[] arrTwoCopy = (!isArrOneLonger)
                ? omitCharArrayEntriesAtIndexes(arrTwo.clone(), omissionStart, omissionEnd)
                : arrTwo.clone();

        return Arrays.equals(arrOneCopy, arrTwoCopy);

    }

    /**
     * Removes the section between the index positions at omissionStart and omissionEnd from arr.
     * @param arr Input array
     * @param omissionStart Start index of section to remove.
     * @param omissionEnd End index of section to remove.
     * @return The array arr, less the section between omissionStart and omissionEnd.
     */
    private static char[] omitCharArrayEntriesAtIndexes(char[] arr, int omissionStart, int omissionEnd) {

        final int omissionLength = omissionEnd - omissionStart;
        char[] output = new char[arr.length - omissionLength];

        for (int i = 0; i < arr.length; i++) {
            if (i < omissionStart) {
                output[i] = arr[i];
            } else if (i >= omissionEnd) {
                output[i - omissionLength] = arr[i];
            }

        }

        return output;

    }

    /**
     *
     * @return int[] containing first and last deviation points
     */
    public int[] getDeviations() {
        return new int[] { firstDeviation, lastDeviation };
    }

    public int[] getDeviationsNewText() {
        return new int[] { firstDeviation, lastDeviationNewText };
    }

    public int[] getDeviationsOldText() {
        return new int[] { firstDeviation, lastDeviationOldText };
    }

    /**
     *
     * @return First deviation
     */
    public int getFirstDeviation() {
        return  firstDeviation;
    }

    /**
     *
     * @return Last deviation, after adjustments with {@link #findLastDeviationOffset()}
     */
    public int getLastDeviation() {
        return  lastDeviation;
    }

    public int getLastDeviationOldText() {
        return lastDeviationOldText;
    }

    public int getLastDeviationNewText() {
        return lastDeviationNewText;
    }

    /**
     *
     * @return Deviation type, in the form of an int value. For a String representation, use
     * {@link #getDeviationTypeAsString()}
     */
    public int getDeviationType() {
        return deviationType;
    }

    /**
     *
     * @return Deviation type as String representing whether text has been added, deleted, replaced
     * or unchanged between old and new.
     */
    public String getDeviationTypeAsString() {

        switch (deviationType) {
            case ADDITION:
                return "Addition";
            case DELETION:
                return "Deletion";
            case REPLACEMENT:
                return "Replacement";
            case UNCHANGED:
                return "Unchanged";
        }

        throw new RuntimeException("Incorrect deviationType");

    }

    /**
     * Converts {@code int} value returned by {@link #getDeviationType()} to {@link String}
     * representation.
     * @param deviationType
     * @return String representation of argument if argument is valid. Otherwise, {@code null}
     */
    public static final String valueOfDeviation(int deviationType) {

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

        switch (deviationType) {
            case DELETION:
            case REPLACEMENT:
                return new String(mOldText).substring(firstDeviation, lastDeviationOldText);
        }

        return "";

    }
}
