package com.werdpressed.partisan.rundo;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * <code>RunDo</code> implementations monitor and manipulate {@link EditText} fields, by
 * periodically saving snippets of text to {@link java.util.Collection}s and reinstating them
 * through {@link #undo()} and {@link #redo()} calls.
 *
 * @author Tom Calver
 */
public interface RunDo extends TextWatcher, WriteToArrayDeque {

    String TAG = "RunDo";

    String UNDO_TAG = "undo_queue";
    String REDO_TAG = "redo_queue";
    String OLD_TEXT_TAG = "old_text";
    String CONFIG_CHANGE_TAG = "return_from_config_change";

    int DEFAULT_QUEUE_SIZE = 10;
    int DEFAULT_TIMER_LENGTH = 2000;

    int TRACKING_STARTED = 12;
    int TRACKING_CURRENT = TRACKING_STARTED + 1;
    int TRACKING_ENDED = TRACKING_CURRENT + 1;

    /**
     * Sets size of Undo and Redo queues. Default size is {@value #DEFAULT_QUEUE_SIZE}.
     * Calling this clears any elements already in the queues.
     * @param size New queue size
     */
    void setQueueSize(int size);

    /**
     * Sets time in milliseconds before text is committed to the undo queue. This timer begins
     * immediately after text entry stops, and is reset if text changes before the timer can
     * complete. Default value is {@value #DEFAULT_TIMER_LENGTH}.
     * @param lengthInMillis Time in milliseconds before text is committed to undo queue
     */
    void setTimerLength(long lengthInMillis);

    /**
     * Updates attached {@link EditText} with text from the last entry in the undo queue, such that
     * it reverts to an earlier state.
     */
    void undo();

    /**
     * Reverts changes made by the last {@link #undo()} call.
     */
    void redo();

    /**
     * Removes all entries from both undo and redo queues.
     */
    void clearAllQueues();

    /**
     * Used by{@link RunDo} implementations to establish a link with an {@link EditText}
     */
    interface TextLink {

        /**
         *
         * @return The {@link EditText} to be monitored and updated by a {@link RunDo}
         * implementation.
         */
        EditText getEditTextForRunDo();

    }

    /**
     * Implement to receive callbacks whenever {@link #undo()} or {@link #redo()} methods are called
     */
    interface Callbacks {

        /**
         * {@link #undo()} called
         */
        void undoCalled();

        /**
         * {@link #redo()} called
         */
        void redoCalled();

    }

    /**
     * Returns a {@link RunDo} implementation which extends either
     * {@link android.support.v4.app.Fragment} or {@link android.app.Fragment}.
     */
    final class Factory {

        private Factory() { throw new AssertionError(); }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public static RunDo getInstance(@NonNull android.app.FragmentManager fm) {

            RunDoNative frag = (RunDoNative) fm.findFragmentByTag(RunDo.TAG);

            if (frag == null) {
                frag = RunDoNative.newInstance();
                fm.beginTransaction().add(frag, RunDo.TAG).commit();
            }

            return frag;

        }

        public static RunDo getInstance(@NonNull android.support.v4.app.FragmentManager fm) {

            RunDoSupport frag = (RunDoSupport) fm.findFragmentByTag(RunDo.TAG);

            if (frag == null) {
                frag = RunDoSupport.newInstance();
                fm.beginTransaction().add(frag, RunDo.TAG).commit();
            }

            return frag;

        }
    }

}
