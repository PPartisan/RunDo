package com.werdpressed.partisan.undoredo;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;

public class SubtractStrings {

    Context context;
    char[] oldText, newText;
    int firstDeviation, lastDeviation, tempReverseDeviation;

    public enum AlterationType {
        ADDITION, REPLACEMENT, DELETION
    }

    public SubtractStrings(Context context) {
        this.context = context;
    }

    public SubtractStrings(Context context, String oldText, String newText) {
        this.context = context;
        this.oldText = oldText.toCharArray();
        this.newText = newText.toCharArray();
    }

    public void findFirstDeviation(char[] oldText, char[] newText) {

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

    public void findLastDeviation(char[] oldText, char[] newText) {

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

    public void findDeviations(char[] oldText, char[] newText) {

        findFirstDeviation(oldText, newText);
        findLastDeviation(oldText, newText);
    }

    public String findAlteredText(char[] oldText, char[] newText){

        if (Arrays.equals(oldText, newText)) {
            return null;
        }

        String oldString = new String(oldText), newString = new String(newText);

        findDeviations(oldText, newText);

        if (newText.length >= oldText.length) {
            return newString.substring(firstDeviation, lastDeviation);
        } else {
            return oldString.substring(firstDeviation, lastDeviation);
        }
    }

    public String findAlteredText(String oldText, String newText){

        if (oldText.equals(newText)) {
            return null;
        }

        char[] oldCharArr = oldText.toCharArray(), newCharArr = newText.toCharArray();

        findDeviations(oldCharArr, newCharArr);

        offsetCheckResult(oldCharArr, newCharArr);

        if (newCharArr.length >= oldCharArr.length) {
            return newText.substring(firstDeviation, lastDeviation);
        } else {
            return oldText.substring(firstDeviation, lastDeviation);
        }
    }

    private boolean offsetCheckResult(char[] oldText, char[] newText) {

    int offsetValue;

        if (oldText.length > newText.length) {
            offsetValue = findOffsetSize(oldText, newText);
            lastDeviation = offsetValue;
        } else if (newText.length > oldText.length) {
            offsetValue = findOffsetSize(newText, oldText);
            lastDeviation = offsetValue;
        } else if (newText.length == oldText.length){
            return false;
        }
        return false;
    }

    private int findOffsetSize(char[] largeText, char[] smallText) {

        int potentialOffsetSize = largeText.length - smallText.length;
        int maxValue;
        boolean condition;

        condition = ((lastDeviation + potentialOffsetSize) < largeText.length);
        maxValue = (condition) ? (lastDeviation + potentialOffsetSize) : (largeText.length - 1);

        largeText = reverseText(largeText);

        for (int i = lastDeviation; i < maxValue; i++){
            if (largeText[i] == largeText[i - potentialOffsetSize]) {
                return (largeText.length + (i - lastDeviation));
            }
        }
        return lastDeviation;
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

    public void setOldText(char[] oldText) {
        this.oldText = oldText;
    }

    public void setNewText(char[] newText) {
        this.newText = newText;
    }

    public void setOldText(String oldText) {
        this.oldText = oldText.toCharArray();
    }

    public void setNewText(String newText) {
        this.newText = newText.toCharArray();
    }

    public int getFirstDeviation() {
        return firstDeviation;
    }

    public int getLastDeviation() {
        return lastDeviation;
    }

    public int[] getDeviations() {
        return new int[]{firstDeviation, lastDeviation};
    }
}
