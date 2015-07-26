package com.werdpressed.partisan.rundo;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.werdpressed.partisan.rundo.SubtractStrings.AlterationType;

/**
 * Adds Undo/Redo functions to either an instance of, or a class that inherits from, {@link EditText}
 * <br>
 * <br>
 * Implementation will often simply be a case of instantiating a <code>RunDoMixer</code> object,
 * passing an <code>EditText</code> argument and calling {@link #undo()} and {@link #redo()} methods.
 *
 * @author Tom Calver
 */

public class RunDoMixer extends Fragment implements TextWatcher, View.OnKeyListener {

    private static final int DEFAULT_COUNTDOWN = 2000;
    private static final int DEFAULT_ARRAY_DEQUE_SIZE = 10;

    /**
     * Optional tag for use when calling {@link #newInstance(int, int, int)} in {@link android.app.FragmentManager}
     */
    public static final String RUNDO_MIXER_TAG = "undo_redo_mixer_tag";

    private static final String KEYBOARD_SHORTCUTS_ID = "keyboard_shortcuts_id";

    private static final String UNDO_ARRAY_ID = "undo_array_id";
    private static final String UNDO_ARRAY_ALT_TYPE_ID = "undo_array_alt_type_id";
    private static final String UNDO_ARRAY_INDEX_ID = "undo_array_index_id";
    private static final String REDO_ARRAY_ID = "uredo_array_id";
    private static final String REDO_ARRAY_ALT_TYPE_ID = "redo_array_alt_type_id";
    private static final String REDO_ARRAY_INDEX_ID = "redo_array_index_id";

    private static final String OLD_TEXT_ID = "old_text_id";
    private static final String NEW_TEXT_ID = "new_text_id";

    private static final String SS_FIRST_DEVIATION = "ss_first_deviation";
    private static final String SS_SECOND_DEVIATION = "ss_second_deviation";
    private static final String SS_NEW_TEXT_LAST_DEVIATION = "ss_new_text_last_deviation";
    private static final String SS_OLD_TEXT_LAST_DEVIATION = "ss_old_text_last_deviation";

    private static final String EDIT_TEXT_RESOURCE_ID = "edit_text_resource_id";
    private static final String COUNTDOWN_TIME = "countdown_id";
    private static final String UNDO_ARRAY_SIZE = "undo_array_size_id";

    private UndoRedoCallbacks mCallbacks;

    private boolean autoSaveSwitch = true, returnFromConfigChange = false;
    private boolean hardwareShortcutsActive = true;

    private boolean sendUndoQueueEmptyMessage = true, sendRedoQueueEmptyMessage = true;
    private String undoQueueEmptyString = null, redoQueueEmptyString = null;

    private TrackingState mTrackingState = TrackingState.ENDED;

    private int countdown, arraySize;
    private Integer[] index;

    private EditText mEditText;

    private SubtractStrings mSubtractStrings;

    private Handler mHandler;
    private Runnable mRunnable;

    private String oldText, newText;

    private CustomArrayDeque<String> mArrayDequeUndo, mArrayDequeRedo;
    private CustomArrayDeque<Integer[]> mArrayDequeUndoIndex, mArrayDequeRedoIndex;
    private CustomArrayDeque<SubtractStrings.AlterationType> mArrayDequeUndoAlt, mArrayDequeRedoAlt;

    /**
     * Tracks whether the user is currently typing (<code>CURRENT</code>), whether user has finished
     * typing and tracking is ready to restart (<code>ENDED</code>), or whether user has finished
     * typing but tracking is not yet ready to begin (<code>STARTED</code>).
     */
    public enum TrackingState {
        STARTED, CURRENT, ENDED
    }

    /**
     * Provides callbacks whenever {@link #undo()} and {@link #redo()} methods are called.
     */
    public interface UndoRedoCallbacks {
        void undoCalled();
        void redoCalled();
    }

    /**
     * Returns a new <code>RunDoMixer</code> instance.
     * <br>
     * <br>
     * Fragment is used to retain information through configuration changes.
     * @param editTextResourceId Optional resource id for an <code>EditText</code> instance.
     *                           The simplest way to achieve this is to use the <code>getId()</code>
     *                           method.
     *                           <br><br>
     *                           It is possible to pass <code>0</code>, and provide an
     *                           <code>EditText</code> instance with {@link #setEditText(Object)}.
     * @param countDown Optional countdown in milliseconds. This defines the period of time between
     *                  when a user stops typing, and a {@link Runnable} is called to being saving
     *                  data to the undo array.
     *                  <br><br>
     *                  A value of less than <code>1</code> will result in the default <code>2000</code>
     *                  millisecond countdown.
     * @param arraySize Optional value that specifies the size of the Undo and Redo
     *                  array queues.
     *                  <br><br>
     *                  An argument with a value of less than
     *                  <code>1</code> will use the default value of <code>10</code>.
     * @return New <code>RunDoMixer</code> instance, with arguments.
     */
    public static RunDoMixer newInstance(int editTextResourceId, int countDown, int arraySize) {
        RunDoMixer frag = new RunDoMixer();
        Bundle args = new Bundle();

        args.putInt(EDIT_TEXT_RESOURCE_ID, editTextResourceId);
        args.putInt(COUNTDOWN_TIME, countDown);
        args.putInt(UNDO_ARRAY_SIZE, arraySize);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof UndoRedoCallbacks) {
            mCallbacks = (UndoRedoCallbacks) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(false);

        countdown = getCountdownSize();
        arraySize = getUndoRedoArraySize();

        mSubtractStrings = new SubtractStrings();

        index = new Integer[]{0,0};

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                String storedString;
                newText = mEditText.getText().toString();

                if (!nullCheck()) {
                    storedString = mSubtractStrings.findAlteredTextInContext(oldText.toCharArray(),
                            newText.toCharArray());

                    if (mSubtractStrings.getAlterationType() == AlterationType.REPLACEMENT) {
                        index = new Integer[]{
                                mSubtractStrings.getFirstDeviation(),
                                mSubtractStrings.getLastDeviationNewText()
                        };
                    } else {
                        index = new Integer[] {
                                mSubtractStrings.getFirstDeviation(),
                                mSubtractStrings.getLastDeviation()
                        };
                    }

                    if (storedString != null) {
                        mArrayDequeUndoIndex.addFirst(index);
                        mArrayDequeUndoAlt.addFirst(mSubtractStrings.getAlterationType());
                        mArrayDequeUndo.addFirst(storedString);
                    }
                    mTrackingState = TrackingState.ENDED;
                }
            }
        };
        mTrackingState = TrackingState.ENDED;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initArrays(savedInstanceState);

        if (savedInstanceState == null && getEditTextResourceId() != 0) {
            mEditText = (EditText) getActivity().findViewById(getEditTextResourceId());
        }

        if (savedInstanceState != null) {
            returnFromConfigChange = true;
            oldText = savedInstanceState.getString(OLD_TEXT_ID);
            newText = savedInstanceState.getString(NEW_TEXT_ID);
            hardwareShortcutsActive = savedInstanceState.getBoolean(KEYBOARD_SHORTCUTS_ID, true);
            mSubtractStrings.setFirstDeviation(savedInstanceState.getInt(SS_FIRST_DEVIATION));
            mSubtractStrings.setLastDeviation(savedInstanceState.getInt(SS_SECOND_DEVIATION));
            mSubtractStrings.setLastDeviationNewText(savedInstanceState.getInt(SS_NEW_TEXT_LAST_DEVIATION));
            mSubtractStrings.setLastDeviationOldText(savedInstanceState.getInt(SS_OLD_TEXT_LAST_DEVIATION));
            mEditText = (EditText) getActivity().findViewById(savedInstanceState.getInt(EDIT_TEXT_RESOURCE_ID));
        }

        mEditText.addTextChangedListener(this);
        if (hardwareShortcutsActive) {
            mEditText.setOnKeyListener(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveArraysToBundle(outState);
        saveStringsToBundle(outState);
        outState.putInt(EDIT_TEXT_RESOURCE_ID, mEditText.getId());
        outState.putBoolean(KEYBOARD_SHORTCUTS_ID, hardwareShortcutsActive);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubtractStrings = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        if (autoSaveSwitch && (mTrackingState == TrackingState.ENDED) && !returnFromConfigChange){
            oldText = mEditText.getText().toString();
            mHandler.postDelayed(mRunnable, countdown);
            mTrackingState = TrackingState.CURRENT;
        }
        if (returnFromConfigChange) returnFromConfigChange = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        if (autoSaveSwitch) {
            switch (mTrackingState) {
                case STARTED:
                    mTrackingState = TrackingState.ENDED;
                    break;
                case CURRENT:
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, countdown);
                    break;
                case ENDED:
                    break;
                default:
                    mTrackingState = TrackingState.ENDED;
                    break;
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        //Empty
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (event.isCtrlPressed()) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_Z:
                        undo();
                        return true;
                    case KeyEvent.KEYCODE_Y:
                        redo();
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the following:
     * <ul>
     *     <li>{@link String} containing the last saved section of text</li>
     *     <li>{@link Integer} array containing first and last points of deviation</li>
     *     <li>{@link AlterationType} <code>enum</code> that specifies whether text was last
     *     replaced, added or deleted.</li>
     * </ul>
     * An optional message will appear to prompt the user that the Undo queue is empty if the queue
     * is either at max capacity, or the next entry in the queue is <code>null</code>
     * <br><br>
     * If an error occurs, by default a {@link Toast} will appear specifying details of the error,
     *  and all queues will be cleared.
     */
    public void undo(){

        if (nullCheck()) return;

        mHandler.removeCallbacks(mRunnable);
        mTrackingState = TrackingState.STARTED;

        String temp;
        AlterationType tempAlt;
        Integer[] tempIndex;

        if (mArrayDequeUndo.peek() == null) {
            if (sendUndoQueueEmptyMessage) {
                if (undoQueueEmptyString == null) {
                    undoQueueEmptyString = getString(R.string.rundo_rd_undo_array_empty);
                }
                Toast.makeText(getActivity(), undoQueueEmptyString, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {

            temp = mArrayDequeUndo.poll();
            tempAlt = mArrayDequeUndoAlt.poll();
            tempIndex = mArrayDequeUndoIndex.poll();

            switch (tempAlt) {
                case ADDITION:
                    mEditText.getText().delete(tempIndex[0], tempIndex[1]);
                    tempAlt = AlterationType.DELETION;
                    break;
                case DELETION:
                    mEditText.getText().insert(tempIndex[0], temp);
                    tempAlt = AlterationType.ADDITION;
                    break;
                case REPLACEMENT:
                    oldText = mEditText.getText().toString();
                    mEditText.getText().replace(tempIndex[0], tempIndex[1], temp);
                    newText = mEditText.getText().toString();
                    temp = mSubtractStrings.findAlteredTextInContext(oldText.toCharArray(), newText.toCharArray());
                    tempIndex = new Integer[] {
                            mSubtractStrings.getFirstDeviation(),
                            mSubtractStrings.getLastDeviationNewText()
                    };
                    break;
            }

            mEditText.setSelection(tempIndex[0]);

            mArrayDequeRedo.addFirst(temp);
            mArrayDequeRedoAlt.addFirst(tempAlt);
            mArrayDequeRedoIndex.addFirst(tempIndex);

            if (mCallbacks != null) mCallbacks.undoCalled();

        } catch (IndexOutOfBoundsException i) {
            Toast.makeText(getActivity(), i.toString(), Toast.LENGTH_SHORT).show();
            sendLogInfo(Log.getStackTraceString(i));
            clearAllArrayDequeue();
        }
    }

    /**
     * Works in parallel with {@link #undo()}. Retrieves the following:
     * <ul>
     *     <li>{@link String} containing the last saved section of text</li>
     *     <li>{@link Integer} array containing first and last points of deviation</li>
     *     <li>{@link AlterationType} <code>enum</code> that specifies whether text was last
     *     replaced, added or deleted.</li>
     * </ul>
     * An optional message will appear to prompt the user that the Undo queue is empty if the queue
     * is either at max capacity, or the next entry in the queue is <code>null</code>
     * <br><br>
     * If an error occurs, by default a {@link Toast} will appear specifying details of the error,
     *  and all queues will be cleared.
     */
    public void redo(){

        if (nullCheck()) return;

        mHandler.removeCallbacks(mRunnable);
        mTrackingState = TrackingState.STARTED;

        String temp;
        AlterationType tempAlt;
        Integer[] tempIndex;

        if (mArrayDequeRedo.peek() == null) {
            if (sendRedoQueueEmptyMessage) {
                if (redoQueueEmptyString == null) {
                    redoQueueEmptyString = getString(R.string.rundo_rd_redo_array_empty);
                }
                Toast.makeText(getActivity(), redoQueueEmptyString, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {

            temp = mArrayDequeRedo.poll();
            tempAlt = mArrayDequeRedoAlt.poll();
            tempIndex = mArrayDequeRedoIndex.poll();

            switch (tempAlt) {
                case ADDITION:
                    mEditText.getText().delete(tempIndex[0], tempIndex[1]);
                    tempAlt = AlterationType.DELETION;
                    break;
                case DELETION:
                    mEditText.getText().insert(tempIndex[0], temp);
                    tempAlt = AlterationType.ADDITION;
                    break;
                case REPLACEMENT:
                    oldText = mEditText.getText().toString();
                    mEditText.getText().replace(tempIndex[0], tempIndex[1], temp);
                    newText = mEditText.getText().toString();
                    temp = mSubtractStrings.findAlteredTextInContext(oldText.toCharArray(), newText.toCharArray());
                    tempIndex = new Integer[] {
                            mSubtractStrings.getFirstDeviation(),
                            mSubtractStrings.getLastDeviationNewText()
                    };
                    break;
            }

            mEditText.setSelection(tempIndex[0]);

            mArrayDequeUndo.addFirst(temp);
            mArrayDequeUndoAlt.addFirst(tempAlt);
            mArrayDequeUndoIndex.addFirst(tempIndex);

            if (mCallbacks != null) mCallbacks.redoCalled();

        } catch (IndexOutOfBoundsException i) {
            Toast.makeText(getActivity(), i.toString(), Toast.LENGTH_SHORT).show();
            sendLogInfo(Log.getStackTraceString(i));
            clearAllArrayDequeue();
        }
    }

    private void clearAllArrayDequeue(){
        mArrayDequeUndo.clear();
        mArrayDequeUndoAlt.clear();
        mArrayDequeUndoIndex.clear();
        mArrayDequeRedo.clear();
        mArrayDequeRedoAlt.clear();
        mArrayDequeRedoIndex.clear();
    }

    @SuppressWarnings("unchecked")
    private void initArrays(Bundle savedInstanceState){
        if (savedInstanceState == null) {
            mArrayDequeUndo = new CustomArrayDeque<>(arraySize);
            mArrayDequeRedo = new CustomArrayDeque<>(arraySize);
            mArrayDequeUndoIndex = new CustomArrayDeque<>(arraySize);
            mArrayDequeRedoIndex = new CustomArrayDeque<>(arraySize);
            mArrayDequeUndoAlt = new CustomArrayDeque<>(arraySize);
            mArrayDequeRedoAlt = new CustomArrayDeque<>(arraySize);
        } else {
            mArrayDequeUndo = (CustomArrayDeque<String>) savedInstanceState.getSerializable(UNDO_ARRAY_ID);
            mArrayDequeRedo = (CustomArrayDeque<String>) savedInstanceState.getSerializable(REDO_ARRAY_ID);
            mArrayDequeUndoIndex = (CustomArrayDeque<Integer[]>) savedInstanceState.getSerializable(UNDO_ARRAY_INDEX_ID);
            mArrayDequeRedoIndex = (CustomArrayDeque<Integer[]>) savedInstanceState.getSerializable(REDO_ARRAY_INDEX_ID);
            mArrayDequeUndoAlt = (CustomArrayDeque<AlterationType>) savedInstanceState.getSerializable(UNDO_ARRAY_ALT_TYPE_ID);
            mArrayDequeRedoAlt = (CustomArrayDeque<AlterationType>) savedInstanceState.getSerializable(REDO_ARRAY_ALT_TYPE_ID);
        }
    }

    private void saveArraysToBundle(Bundle outState){
        outState.putSerializable(UNDO_ARRAY_ID, mArrayDequeUndo);
        outState.putSerializable(UNDO_ARRAY_ALT_TYPE_ID, mArrayDequeUndoAlt);
        outState.putSerializable(UNDO_ARRAY_INDEX_ID, mArrayDequeUndoIndex);
        outState.putSerializable(REDO_ARRAY_ID, mArrayDequeRedo);
        outState.putSerializable(REDO_ARRAY_INDEX_ID, mArrayDequeRedoIndex);
        outState.putSerializable(REDO_ARRAY_ALT_TYPE_ID, mArrayDequeRedoAlt);
    }

    private void saveStringsToBundle(Bundle outState){
        outState.putString(OLD_TEXT_ID, oldText);
        outState.putString(NEW_TEXT_ID, newText);
        outState.putInt(SS_FIRST_DEVIATION, mSubtractStrings.getFirstDeviation());
        outState.putInt(SS_SECOND_DEVIATION, mSubtractStrings.getLastDeviation());
        outState.putInt(SS_OLD_TEXT_LAST_DEVIATION, mSubtractStrings.getLastDeviationOldText());
        outState.putInt(SS_NEW_TEXT_LAST_DEVIATION, mSubtractStrings.getLastDeviationNewText());
    }

    /**
     * Provide an {@link Object} that either extends, or is an instance of, {@link EditText}.
     * <br><br>
     * This is essential for all method calls to avoid a {@link NullPointerException}
     * @param object Argument must be of a <code>Class</code> that either inherits from, or is a
     *               direct instance of, {@link EditText}.
     * @throws ClassCastException
     */
    public void setEditText(Object object) {
        if (!(object instanceof EditText)) {
            throw new ClassCastException(object.toString() +
                    getString(R.string.rundo_rd_constructor_error_class_cast));
        }
        mEditText = (EditText) object;
    }

    /**
     * Retrieve the <code>EditText</code> associated with this <code>Class</code> instance.
     * @return {@link Object} that either inherits from, or is a direct instance of, <code>EditText</code>
     */
    public Object getEditText(){
        return mEditText;
    }

    /**
     * Enables common shortcuts for hardware keyboards. <code>Ctrl + Z</code> will call {@link #undo()},
     * whilst <code>Ctrl + Y</code> will call {@link #redo()}.
     * @param shortcutsActive Pass <code>true</code> to enable keyboard shortcuts.
     * @see #areKeyboardShortcutsActive()
     */
    public void setKeyboardShortcuts(boolean shortcutsActive) {

        hardwareShortcutsActive = shortcutsActive;
        if (mEditText != null){
            if (shortcutsActive) {
                mEditText.setOnKeyListener(this);
            } else {
                mEditText.setOnKeyListener(null);
            }
        }
    }

    /**
     * @return Returns <code>true</code> if common hardware keyboard shortcuts are currently active.
     * @see #setKeyboardShortcuts(boolean)
     */
    public boolean areKeyboardShortcutsActive(){
        return hardwareShortcutsActive;
    }

    /**
     * Retrieve the resource id for the <code>EditText</code> associated with this <code>Class</code> instance.
     * @return Resource id for <code>EditText</code>, as <code>int</code> value.
     */
    public int getEditTextResourceId(){
        return getArguments().getInt(EDIT_TEXT_RESOURCE_ID, 0);
    }

    /**
     * Retrieve time in milliseconds between user finishing typing and a {@link Runnable} launching
     * the Undo save process
     * @return Time in milliseconds, as <code>int</code> value;
     */
    public int getCountdownSize(){
        if (getArguments().getInt(COUNTDOWN_TIME, 0) < 1) {
            return DEFAULT_COUNTDOWN;
        }
        return getArguments().getInt(COUNTDOWN_TIME);
    }

    /**
     * Size of the Undo and Redo arrays. This value determines the maximum amount of Undo queue storage
     * before items are deleted.
     * @return Size of Undo/Redo queue, as <code>int</code> value;
     */
    public int getUndoRedoArraySize(){
        if (getArguments().getInt(UNDO_ARRAY_SIZE, 0) < 1) {
            return DEFAULT_ARRAY_DEQUE_SIZE;
        }
        return getArguments().getInt(UNDO_ARRAY_SIZE);
    }

    /**
     * Specifies whether AutoSave feature is currently active. When this returns <code>false</code>, no further
     * data will be saved to the Undo queue. This value is <code>true</code> by default.
     * @return <code>true</code> if AutoSave is currently active, otherwise <code>false</code>
     */
    public boolean isAutoSave() {
        return autoSaveSwitch;
    }

    /**
     * Set to <code>true</code> to activate AutoSave. When AutoSave is inactive, no data will be saved to the
     * Undo queue.
     * @param autoSaveSwitch <code>true</code> to enabled the AutoSave feature, otherwise <code>false</code>
     */
    public void setAutoSaveSwitch(boolean autoSaveSwitch) {
        this.autoSaveSwitch = autoSaveSwitch;
    }

    /**
     * Specify whether a {@link Toast} will appear when {@link #undo()} is called, but the Undo queue
     * is either empty or the next item is null, and whether a custom message will appear instead of
     * the default.
     * @param condition <code>true</code> will send a <code>Toast</code> message if <code>undo()</code>
     *                  is called when Undo queue is empty, or next value is null.
     * @param message Optional message to send in the <code>Toast</code> notification. Passing <code>null</code>
     *                here will use the default value.
     * @see #setRedoQueueEmptyMessage(boolean, String)
     */
    public void setUndoQueueEmptyMessage(boolean condition, String message){
        sendUndoQueueEmptyMessage = condition;
        undoQueueEmptyString = message;
    }
    /**
     * Specify whether a {@link Toast} will appear when {@link #redo()} is called, but the Redo queue
     * is either empty or the next item is null, and whether a custom message will appear instead of
     * the default.
     * @param condition <code>true</code> will send a <code>Toast</code> message if <code>redo()</code>
     *                  is called when Redo queue is empty, or next value is null.
     * @param message Optional message to send in the <code>Toast</code> notification. Passing <code>null</code>
     *                here will use the default value.
     * @see #setUndoQueueEmptyMessage(boolean, String)
     */
    public void setRedoQueueEmptyMessage(boolean condition, String message){
        sendRedoQueueEmptyMessage = condition;
        redoQueueEmptyString = message;
    }

    private boolean nullCheck(){
        return (oldText == null || mSubtractStrings == null || mEditText == null);
    }

    private boolean nullCheck(String tag){

        sendLogInfo(tag);

        if (mEditText == null) {
            sendLogInfo("mEditText is null");
            return true;
        }

        if (mSubtractStrings == null) {
            sendLogInfo("mSubtractString is null");
            return true;
        }

        if (oldText == null) {
            sendLogInfo("oldText is null");
            return true;
        }

        return false;
    }

    private void sendLogInfo(String message) {
        Log.e(getClass().getSimpleName(), message);
    }
}
