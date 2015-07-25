package com.werdpressed.partisan.rundo;

import android.util.Log;

import java.util.Arrays;

/**
 * Compares two {@link String} objects and returns the first and last points they deviate from one another, <code>subString</code>s between the two, and information on whether or not text has been added, replaced or deleted.
 *
 * @author Tom Calver
 */

public class SubtractStrings {

    char[] oldText, newText;

    private int firstDeviation, lastDeviation, tempReverseDeviation;
    private int lastDeviationOldText, lastDeviationNewText;

    private AlterationType alterationType;

    enum AlterationType {
        ADDITION, REPLACEMENT, DELETION, UNCHANGED
    }

    /**
     * Default constructor
     */
    public SubtractStrings() {
    }

    /**
     * Contains the two <code>String</code>s to be compared.
     *
     * <br><br>The order of the arguments determines the results of
     * certain methods (i.e. if <code>oldText</code> is empty,
     * but <code>newText</code> populated, the change will be regarded as an <code>ADDITION</code>
     * rather than <code>DELETION</code>)
     *
     * @param oldText Old Text to compare.
     * @param newText New Text to compare.
     */
    public SubtractStrings(String oldText, String newText) {
        this.oldText = oldText.toCharArray();
        this.newText = newText.toCharArray();
    }

    /**
     * Calculates the first point of deviation between two arguments, and stores it to <code>firstDeviation</code>
     *
     * @param oldText Old Text to compare.
     * @param newText New Text to compare.
     */
    private void findFirstDeviation(char[] oldText, char[] newText) {

        if (Arrays.equals(oldText, newText)) {
            return;
        }

        int shortestLength = findShortestLength(oldText, newText);

        for (int i = 0; i < shortestLength; i++) {
            if (oldText[i] != newText[i]) {
                firstDeviation = i;
                return;
            }
        }
        firstDeviation = shortestLength;
    }

    /**
     * Calculates the last point of deviation between the two arguments, in respect to the value with the longer length.
     *
     * Unlike {@link #findLastDeviationInContext(char[], char[])}, the values assigned by this method
     * will always be in respect to the longer of the two arguments, regardless of whether this is <code>oldText.length</code>
     * or <code>newText.length</code>.
     *
     * @param oldText Old Text to compare.
     * @param newText New Text to comapre.
     *
     * @see #findLastDeviationInContext(char[], char[])
     */
    private void findLastDeviation(char[] oldText, char[] newText) {

        char[] oldTextReversed = reverseText(oldText);
        char[] newTextReversed = reverseText(newText);

        if (Arrays.equals(oldText, newText)) {
            return;
        }

        int shortestLength = findShortestLength(oldTextReversed, newTextReversed);
        int longestLength = findLongestLength(oldTextReversed, newTextReversed);

        for (int i = 0; i < shortestLength; i++) {
            if (oldTextReversed[i] != newTextReversed[i]) {
                tempReverseDeviation = i;
                lastDeviation = (longestLength - i);
                return;
            }
        }
        tempReverseDeviation = shortestLength;
        lastDeviation = longestLength - shortestLength;
    }

    /**
     * Calculates the last point of deviation between the two arguments, in respect to <code>newText</code>.
     *
     * Unlike {@link #findLastDeviation(char[], char[])}, the values assigned by this method will be
     * in respect to the <code>newText</code> parameter, regardless of whether <code>newText.length</code>
     * is longer or shorter than <code>oldText.length</code>
     *
     * @param oldText Old Text
     * @param newText New Text
     *
     * @see #findLastDeviation(char[], char[])
     */
    private void findLastDeviationInContext(char[] oldText, char[] newText) {

        char[] oldTextReversed = reverseText(oldText);
        char[] newTextReversed = reverseText(newText);

        boolean condition = (newText.length > oldText.length);

        if (Arrays.equals(oldText, newText)) {
            return;
        }

        int shortestLength = findShortestLength(oldTextReversed, newTextReversed);
        int longestLength = findLongestLength(oldTextReversed, newTextReversed);

        for (int i = 0; i < shortestLength; i++) {
            if (oldTextReversed[i] != newTextReversed[i]) {
                tempReverseDeviation = i;
                lastDeviationNewText = (condition) ? (longestLength - i) : (shortestLength - i);
                lastDeviationOldText = (condition) ? (shortestLength - i) : (longestLength - i);
                lastDeviation = (condition) ? lastDeviationNewText : lastDeviationOldText;
                return;
            }
        }
        tempReverseDeviation = shortestLength;
        lastDeviation = longestLength - shortestLength;
        lastDeviationNewText = (condition) ? (longestLength - shortestLength) : shortestLength;
        lastDeviationOldText = (condition) ? shortestLength : (longestLength - shortestLength);
    }

    /**
     * Convenience method for calculating {@link #findFirstDeviation(char[], char[])} and
     * {@link #findLastDeviationInContext(char[], char[])} simultaneously.
     *
     * @param oldText Old Text
     * @param newText New Text
     *
     * @see #findDeviations(char[], char[])
     */
    private void findDeviationsInContext(char[] oldText, char[] newText) {

        findFirstDeviation(oldText, newText);
        findLastDeviationInContext(oldText, newText);
    }

    /**
     * Calculates the difference between the two arguments, and returns a <code>subString</code>
     * based on whether or not text has been added, deleted or replaced.
     *
     * If text has been replaced, this method will always return a <code>subString</code> from the
     * <code>newText</code> parameter.
     *
     * @param oldText Old Text
     * @param newText New Text
     * @return <code>subString</code> of <code>newText</code> if text has been replaced, or a
     * <code>subString</code> of either <code>oldText</code> or <code>newText</code> if an addition
     * or deletion has occured.
     *
     * @see #findAlteredText(char[], char[])
     * @see #findAlteredText(String, String)
     */
    public String findAlteredTextInContext(char[] oldText, char[] newText) {

        if (Arrays.equals(oldText, newText)) {
            return null;
        }

        String oldString = new String(oldText);
        String newString = new String(newText);
        findDeviationsInContext(oldText, newText);
        offsetCheckResultInContext(oldText, newText);
        alterationType = findAlterationType(oldText, newText);

        if (alterationType == AlterationType.REPLACEMENT) {
            lastDeviation = lastDeviationNewText;
            return oldString.substring(firstDeviation, lastDeviationOldText);
        } else {
            if (newText.length >= oldText.length) {
                return newString.substring(firstDeviation, lastDeviation);
            } else {
                return oldString.substring(firstDeviation, lastDeviation);
            }
        }

    }

    /**
     * Calculates offset and, if necessary, adjusts <code>lastDeviation</code> values for
     * {@link #lastDeviationOldText} and {@link #lastDeviationNewText} accordingly.
     *
     * This method checks for situations where {@link #findLastDeviation(char[], char[])} or
     * {@link #findLastDeviationInContext(char[], char[])} could return false values, and makes
     * relevant adjustments.
     *
     * Unlike {@link #offsetCheckResult(char[], char[])}, this method will assign values to
     * {@link #lastDeviationOldText} and {@link #lastDeviationNewText}
     *
     * @param oldText Old Text
     * @param newText New Text
     *
     * @see #offsetCheckResult(char[], char[])
     */
    private void offsetCheckResultInContext(char[] oldText, char[] newText) {

        if (oldText.length == newText.length) {
            return;
        }

        int deviationOffset;
        int offsetValue = subtractLongestFromShortest(oldText, newText);

        if (oldText.length > newText.length) {
            deviationOffset = findOffsetSizeInContext(true, oldText, newText);
            lastDeviationOldText = deviationOffset;
            lastDeviationNewText = deviationOffset - offsetValue;
        } else if (newText.length > oldText.length) {
            deviationOffset = findOffsetSizeInContext(false, newText, oldText);
            lastDeviationNewText = deviationOffset;
            lastDeviationOldText = deviationOffset - offsetValue;
        }
    }

    private int findOffsetSizeInContext(boolean oldTextLarger, char[] largeText, char[] smallText) {

        int potentialOffsetSize = largeText.length - smallText.length;
        int maxCalculatedValue;
        int adjustedReverseDeviation;
        boolean condition;

        condition = ((tempReverseDeviation + potentialOffsetSize) < largeText.length);
        maxCalculatedValue = (condition) ? (tempReverseDeviation + potentialOffsetSize) : (largeText.length);

        adjustedReverseDeviation = (tempReverseDeviation < potentialOffsetSize) ? (potentialOffsetSize) : tempReverseDeviation;

        for (int i = (adjustedReverseDeviation); i < maxCalculatedValue; i++) {
            if (largeText[i] == largeText[i - potentialOffsetSize]) {
                int returnValue = largeText.length - (i - potentialOffsetSize);
                condition = ((returnValue - firstDeviation) < potentialOffsetSize);
                return (condition) ? (firstDeviation + potentialOffsetSize) : returnValue;
            }
        }

        if (oldTextLarger) {
            condition = (lastDeviationOldText < firstDeviation) ||
                    ((largeText.length == smallText.length) && (lastDeviationOldText <= firstDeviation));
        } else {
            condition = (lastDeviationNewText < firstDeviation) ||
                    ((largeText.length == smallText.length) && (lastDeviationNewText <= firstDeviation));
        }
        return (condition) ? (lastDeviation + potentialOffsetSize) : lastDeviation;
    }

    private void findDeviations(char[] oldText, char[] newText) {

        findFirstDeviation(oldText, newText);
        findLastDeviation(oldText, newText);
    }

    /**
     * Convenience method. Converts parameters to <code>String</code>s.
     * @param oldText Old Text to compare.
     * @param newText New Text to compare.
     * @return <code>subString</code> of the larger of the two arguments, between {@link #firstDeviation} and {@link #lastDeviation}
     *
     * @see #findAlteredText(char[], char[])
     */
    public String findAlteredText(char[] oldText, char[] newText){
        return findAlteredText(new String(oldText), new String(newText));
    }

    /**
     * Calculates first and last deviation points between <code>oldText</code> and <code>newText</code>,
     * accounts for offsets, assigns an {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType}
     * to {@link #alterationType} and returns a <code>subString</code> between the two deviation points.
     *
     * <br><br>Unlike {@link #findAlteredTextInContext(char[], char[])}, this method will always
     * return a <code>subString</code> of the larger of the two argumens.
     *
     * @param oldText Old Text to compare.
     * @param newText New Text to compare.
     * @return <code>subString</code> of the larger of the two arguments, between {@link #firstDeviation} and {@link #lastDeviation}
     */
    public String findAlteredText(String oldText, String newText){

        if (oldText.equals(newText)) {
            return null;
        }

        char[] oldCharArr = oldText.toCharArray();
        char[] newCharArr = newText.toCharArray();

        findDeviations(oldCharArr, newCharArr);

        offsetCheckResult(oldCharArr, newCharArr);

        alterationType = findAlterationType(oldText, newText);

        if (newCharArr.length >= oldCharArr.length) {
            return newText.substring(firstDeviation, lastDeviation);
        } else {
            return oldText.substring(firstDeviation, lastDeviation);
        }
    }

    private void offsetCheckResult(char[] oldText, char[] newText) {

        if (oldText.length == newText.length) {
            return;
        }

        if (oldText.length > newText.length) {
            lastDeviation = findOffsetSize(oldText, newText);
        } else if (newText.length > oldText.length) {
            lastDeviation = findOffsetSize(newText, oldText);
        }

    }

    private int findOffsetSize(char[] largeText, char[] smallText) {

        int potentialOffsetSize = largeText.length - smallText.length;
        int maxCalculatedValue;
        int adjustedReverseDeviation;
        boolean condition;

        condition = ((tempReverseDeviation + potentialOffsetSize) < largeText.length);
        maxCalculatedValue = (condition) ? (tempReverseDeviation + potentialOffsetSize) : (largeText.length);

        adjustedReverseDeviation = (tempReverseDeviation < potentialOffsetSize) ? (potentialOffsetSize) : tempReverseDeviation;

        for (int i = (adjustedReverseDeviation); i < maxCalculatedValue; i++) {
            if (largeText[i] == largeText[i - potentialOffsetSize]) {
                int returnValue = largeText.length - (i - potentialOffsetSize);
                if ((returnValue - firstDeviation) < potentialOffsetSize) {
                    return firstDeviation + potentialOffsetSize;
                } else {
                    return returnValue;
                }

            }
        }

        condition = (lastDeviation < firstDeviation) ||
                ((largeText.length == smallText.length) && (lastDeviation <= firstDeviation));

        return (condition) ? (lastDeviation + potentialOffsetSize) : lastDeviation;
    }


    private int findLongestLength(char[] oldText, char[] newText) {

        int oldLength, newLength;

        oldLength = oldText.length;
        newLength = newText.length;

        return (oldLength <= newLength) ? newLength : oldLength;
    }

    private int subtractLongestFromShortest(char[] oldText, char[] newText) {
        if (oldText.length > newText.length) {
            return oldText.length - newText.length;
        } else if (newText.length > oldText.length) {
            return newText.length - oldText.length;
        } else {
            return 0;
        }
    }

    private AlterationType findAlterationType(char[] oldText, char[] newText){

        int offsetValue = subtractLongestFromShortest(oldText, newText);
        boolean replacementCheck = ((lastDeviation - offsetValue) - firstDeviation != 0);

        if (oldText.length > newText.length) {
            return replacementCheck ? AlterationType.REPLACEMENT : AlterationType.DELETION;
        } else if (newText.length > oldText.length) {
            return replacementCheck ? AlterationType.REPLACEMENT : AlterationType.ADDITION;
        } else if ((newText.length == oldText.length) && !Arrays.equals(oldText, newText)) {
            return AlterationType.REPLACEMENT;
        } else {
            return AlterationType.UNCHANGED;
        }
    }

    private AlterationType findAlterationType(String oldString, String newString){
        return findAlterationType(oldString.toCharArray(), newString.toCharArray());
    }

    /**
     * Complements {@link #findAlteredText(String, String)} by calculating the <code>subString</code>
     * for the smaller of <code>oldText.length</code> and <code>newText.length</code>.
     *
     * <br><br>Requires {@link #findAlteredText(String, String)} to be called first.
     *
     * <br><br>Requires an {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} argument,
     * else method will return <code>null</code>
     *
     * @param altType Must be of {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} <code>REPLACEMENT</code>
     * @param oldText Old Text to compare.
     * @param newText New Text to compare.
     *
     * @return <code>subString</code> of the smaller of <code>oldText</code> or <code>newText</code>
     * between {@link #firstDeviation} and {@link #lastDeviation} if
     * {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} is <code>REPLACEMENT</code>.
     * If {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} is not
     * <code>REPLACEMENT</code>, this method will return <code>null</code>
     */
    public String findReplacedText(AlterationType altType, char[] oldText, char[] newText) {

        int offsetValue = subtractLongestFromShortest(oldText, newText);
        String returnText = (oldText.length > newText.length) ? new String(newText) : new String(oldText);

        if (altType == AlterationType.REPLACEMENT) {
            return returnText.substring(firstDeviation, (lastDeviation - offsetValue));
        }
        return null;
    }

    /**
     * Convenience method.Converts parameters to <code>char[]</code>
     * @param altType Must be of {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} <code>REPLACEMENT</code>
     * @param oldTextString Old Text to compare.
     * @param newTextString New Text to compare.
     * @return <code>subString</code> of the smaller of <code>oldText</code> or <code>newText</code>
     * between {@link #firstDeviation} and {@link #lastDeviation} if
     * {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} is <code>REPLACEMENT</code>.
     * If {@link com.werdpressed.partisan.rundo.SubtractStrings.AlterationType} is not
     * <code>REPLACEMENT</code>, this method will return <code>null</code>
     */
    public String findReplacedText(AlterationType altType, String oldTextString, String newTextString) {
        return findReplacedText(altType, oldTextString.toCharArray(), newTextString.toCharArray());
    }

    private int findShortestLength(char[] oldText, char[] newText) {

        int oldLength, newLength;

        oldLength = oldText.length;
        newLength = newText.length;

        return (oldLength <= newLength) ? oldLength : newLength;
    }

    private char[] reverseText(char[] inputText) {

        int index = 0;

        for (int i = inputText.length - 1; i >= inputText.length / 2; i--) {
            char temp = inputText[i];
            inputText[i] = inputText[index];
            inputText[index] = temp;
            index++;
        }
        return inputText;
    }

    private void sendLogInfo(String message) {
        Log.e(getClass().getSimpleName(), message);
    }

    /**
     * Returns {@link #firstDeviation} value
     * @return {@link #firstDeviation}
     */
    public int getFirstDeviation() {
        return firstDeviation;
    }

    /**
     * Returns {@link #lastDeviation} value
     * @return {@link #lastDeviation} This marks the last point of deviation
     *                      at the end of the longer of the two compared <code>String</code>s
     */
    public int getLastDeviation() {
        return lastDeviation;
    }

    /**
     * Set {@link #firstDeviation} value.
     * @param firstDeviation New <code>firstDeviation</code> value.
     */
    public void setFirstDeviation(int firstDeviation) {
        this.firstDeviation = firstDeviation;
    }
    /**
     * Set {@link #lastDeviation} value.
     * @param lastDeviation New <code>lastDeviation</code> value. This marks the last point of deviation
     *                      at the end of the longer of the two compared <code>String</code>s
     */
    public void setLastDeviation(int lastDeviation) {
        this.lastDeviation = lastDeviation;
    }
    /**
     * Set {@link #lastDeviationOldText} value.
     * @param lastDeviationOldText New <code>lastDeviationOldText</code> value. This marks the last
     *                             point of deviation at the end of <code>oldText</code>
     */
    public void setLastDeviationOldText(int lastDeviationOldText) {
        this.lastDeviationOldText = lastDeviationOldText;
    }
    /**
     * Set {@link #lastDeviationNewText} value.
     * @param lastDeviationNewText New <code>lastDeviationNewText</code> value.This marks the last
     *                             point of deviation at the end of <code>newText</code>
     */
    public void setLastDeviationNewText(int lastDeviationNewText) {
        this.lastDeviationNewText = lastDeviationNewText;
    }
    /**
     * Get {@link #lastDeviationNewText} value.
     * @return <code>lastDeviationNewText</code> value. This marks the last
     *                             point of deviation at the end of <code>newText</code>
     */
    public int getLastDeviationNewText() {
        return lastDeviationNewText;
    }

    /**
     * Get {@link #lastDeviationNewText} value.
     * @return <code>lastDeviationOldText</code> value. This marks the last
     *                             point of deviation at the end of <code>oldText</code>
     */
    public int getLastDeviationOldText() {
        return lastDeviationOldText;
    }

    /**
     * Get {@link #alterationType} value.
     * @return <code>lastAlterationType</code> value. Specifies whether the new text replaced,
     * added to or deleted from the old text during the last comparison.
     */
    public AlterationType getAlterationType() {
        return alterationType;
    }
}
