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

        offsetCheckResult(oldText, newText);

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

    private void offsetCheckResult(char[] oldText, char[] newText) {

        if (oldText.length > newText.length) {
            sendLogInfo("oldText > newText");
            lastDeviation = findOffsetSize(oldText, newText);
        } else if (newText.length > oldText.length) {
            sendLogInfo("newText > oldText");
            lastDeviation = findOffsetSize(newText, oldText);
        }

    }

    private int findOffsetSize(char[] largeText, char[] smallText) {

        int potentialOffsetSize = largeText.length - smallText.length;
        int maxCalculatedValue, absoluteMaxValue, cycleLimit;
        boolean condition;

        sendLogInfo("lastDeviation is " + lastDeviation);
        //condition = ((lastDeviation + potentialOffsetSize) < largeText.length);
        condition = ((tempReverseDeviation + potentialOffsetSize) < largeText.length);

        sendLogInfo("condition is " + condition);

        maxCalculatedValue = (condition) ? (tempReverseDeviation + potentialOffsetSize) : (largeText.length);

        absoluteMaxValue = tempReverseDeviation + 1 + potentialOffsetSize;
        //if (absoluteMaxValue < maxCalculatedValue) maxCalculatedValue = absoluteMaxValue;

        sendLogInfo("maxCalculatedValue is " + maxCalculatedValue + " and potentialOffsetSet is " + potentialOffsetSize);
        sendLogInfo("tempReverseDeviation is " + tempReverseDeviation);

        sendLogInfo("largeTextLength is " + largeText.length);

        //cycleLimit = (potentialOffsetSize > smallText.length) ? smallText.length : potentialOffsetSize;
        cycleLimit = (potentialOffsetSize + tempReverseDeviation > largeText.length - tempReverseDeviation) ? (largeText.length - tempReverseDeviation) : potentialOffsetSize + tempReverseDeviation;

        sendLogInfo("cycleLimit is " + cycleLimit);

        /*
        for (int i = 0; i < cycleLimit; i++) {
            sendLogInfo("i is " + i);
            sendLogInfo("largeText[i] is " + largeText[i] + " and largeText[i + offset] is " + largeText[i + potentialOffsetSize]);
            if ((largeText[i] == largeText[i + potentialOffsetSize]) &&
                    (largeText[i + 1] == largeText[i + 1 + potentialOffsetSize])) {
                return (largeText.length - i);
            }
        }
        */
        if (absoluteMaxValue < maxCalculatedValue) {
            for (int i = (tempReverseDeviation + 1); i < maxCalculatedValue; i++) {
                sendLogInfo("i is " + i);
                sendLogInfo("largeText[i] is " + largeText[i] + " and largeText[i + offset] is " + largeText[i + potentialOffsetSize]);
                if (largeText[i] == largeText[i - potentialOffsetSize]) {
                    sendLogInfo("returnVal is " + (largeText.length - (i - potentialOffsetSize)));
                    return (largeText.length - (i - potentialOffsetSize));
                }
            }
        }
        sendLogInfo("returned value outside for loop");
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

    public AlterationType findAlterationType(char[] oldText, char[] newText){

        int offsetValue = subtractLongestFromShortest(oldText, newText);
        boolean replacementCheck = ((lastDeviation - offsetValue) - firstDeviation != 0);

        if (oldText.length > newText.length) {
            return replacementCheck ? AlterationType.REPLACEMENT : AlterationType.DELETION;
        } else if (newText.length > oldText.length) {
            return replacementCheck ? AlterationType.REPLACEMENT : AlterationType.ADDITION;
        }
        return AlterationType.REPLACEMENT;
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
