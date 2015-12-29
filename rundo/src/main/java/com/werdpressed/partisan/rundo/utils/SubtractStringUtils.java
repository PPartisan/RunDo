package com.werdpressed.partisan.rundo.utils;

import java.util.Arrays;

/**
 * Static methods which operate on {@code char[]}, predominantly for use with
 * {@link com.werdpressed.partisan.rundo.SubtractStrings}
 */
public final class SubtractStringUtils {

    private SubtractStringUtils() { throw new AssertionError(); }

    /**
     * Reverses input {@code char[]}.
     * @param input {@code char[]} to reverse.
     * @return input {@code char[]} reversed.
     */
    public static char[] reverseCharArray(char[] input){

        char[] output = new char[input.length];
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
     */
    public static boolean isArrayEqualWithOmission(
            char[] arrOne, char[] arrTwo, int omissionStart, int omissionEnd
    ) {

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
    public static char[] omitCharArrayEntriesAtIndexes(char[] arr, int omissionStart, int omissionEnd) {

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

}
