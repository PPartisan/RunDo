package com.werdpressed.partisan.undoredo;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.werdpressed.partisan.undoredo.SubtractStrings.AlterationType;

public class UndoRedoMixer extends Fragment implements TextWatcher {

    private static final int DEFAULT_COUNTDOWN = 2000;
    private static final int DEFAULT_ARRAY_DEQUE_SIZE = 10;

    public static final String UNDO_REDO_MIXER_TAG = "undo_redo_mixer_tag";

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

    enum TrackingState {
        STARTED, CURRENT, ENDED
    }

    public interface UndoRedoCallbacks {
        void undoCalled();
        void redoCalled();
    }

    public static UndoRedoMixer newInstance(int editTextResourceId, int countDown, int arraySize) {
        UndoRedoMixer frag = new UndoRedoMixer();
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

        mSubtractStrings = new SubtractStrings(getActivity());

        index = new Integer[]{0,0};

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                AlterationType mAlt;
                String storedString;
                newText = mEditText.getText().toString();

                if (!nullCheck()) {
                    storedString = mSubtractStrings.findAlteredText(oldText, newText);
                    index = new Integer[]{
                            mSubtractStrings.getFirstDeviation(),
                            mSubtractStrings.getLastDeviation()};

                    mAlt = mSubtractStrings.findAlterationType(oldText, newText);

                    if (mAlt == AlterationType.REPLACEMENT) {
                        storedString = mSubtractStrings.findAlteredTextInContext(oldText.toCharArray(), newText.toCharArray());
                        index = new Integer[]{
                                mSubtractStrings.getFirstDeviation(),
                                mSubtractStrings.lastDeviationNewText
                        };
                    }

                    if (storedString != null) {
                        mArrayDequeUndoIndex.addFirst(index);
                        mArrayDequeUndoAlt.addFirst(mAlt);
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
            mSubtractStrings.setFirstDeviation(savedInstanceState.getInt(SS_FIRST_DEVIATION));
            mSubtractStrings.setLastDeviation(savedInstanceState.getInt(SS_SECOND_DEVIATION));
            mSubtractStrings.setLastDeviationNewText(savedInstanceState.getInt(SS_NEW_TEXT_LAST_DEVIATION));
            mSubtractStrings.setLastDeviationOldText(savedInstanceState.getInt(SS_OLD_TEXT_LAST_DEVIATION));
            mEditText = (EditText) getActivity().findViewById(savedInstanceState.getInt(EDIT_TEXT_RESOURCE_ID));
        }

        mEditText.addTextChangedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveArraysToBundle(outState);
        saveStringsToBundle(outState);
        outState.putInt(EDIT_TEXT_RESOURCE_ID, mEditText.getId());
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
                    undoQueueEmptyString = getString(R.string.etw_undo_array_empty);
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
                            mSubtractStrings.lastDeviationNewText
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
                    redoQueueEmptyString = getString(R.string.etw_redo_array_empty);
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
                            mSubtractStrings.lastDeviationNewText
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
        outState.putInt(SS_OLD_TEXT_LAST_DEVIATION, mSubtractStrings.lastDeviationOldText);
        outState.putInt(SS_NEW_TEXT_LAST_DEVIATION, mSubtractStrings.lastDeviationNewText);
    }

    public void setEditText(Object object) {
        if (!(object instanceof EditText)) {
            throw new ClassCastException(object.toString() +
                    getString(R.string.etw_constructor_error_class_cast));
        }
        mEditText = (EditText) object;
    }

    public Object getEditText(){
        return mEditText;
    }

    public int getEditTextResourceId(){
        return getArguments().getInt(EDIT_TEXT_RESOURCE_ID, 0);
    }

    public int getCountdownSize(){
        if (getArguments().getInt(COUNTDOWN_TIME, 0) < 1) {
            return DEFAULT_COUNTDOWN;
        }
        return getArguments().getInt(COUNTDOWN_TIME);
    }

    public int getUndoRedoArraySize(){
        if (getArguments().getInt(UNDO_ARRAY_SIZE, 0) < 1) {
            return DEFAULT_ARRAY_DEQUE_SIZE;
        }
        return getArguments().getInt(UNDO_ARRAY_SIZE);
    }

    public boolean isAutoSave() {
        return autoSaveSwitch;
    }

    public void setAutoSaveSwitch(boolean autoSaveSwitch) {
        this.autoSaveSwitch = autoSaveSwitch;
    }

    public void setUndoQueueEmptyMessage(boolean condition, String message){
        sendUndoQueueEmptyMessage = condition;
        undoQueueEmptyString = message;
    }

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
