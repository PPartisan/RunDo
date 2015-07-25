package com.werdpressed.partisan.undoredo;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.werdpressed.partisan.undoredo.SubtractStrings.AlterationType;

public class EditTextWatcher implements TextWatcher, View.OnClickListener {

    private static final int DEFAULT_COUNTDOWN = 2000;
    private static final int DEFAULT_ARRAY_DEQUE_SIZE = 10;

    private boolean autoSaveSwitch = true;
    private TrackingState mTrackingState = TrackingState.ENDED;

    private int countdown, arraySize;
    private Integer[] index;

    private Context mContext;
    private EditText mEditText;

    private SubtractStrings mSubtractStrings;

    private Handler mHandler;
    private Runnable mRunnable;

    private String oldText, newText;

    private CustomArrayDeque<String> mArrayDequeUndo, mArrayDequeRedo;
    private CustomArrayDeque<Integer[]> mArrayDequeUndoIndex, mArrayDequeRedoIndex;
    private CustomArrayDeque<AlterationType> mArrayDequeUndoAlt, mArrayDequeRedoAlt;

    enum TrackingState {
        STARTED, CURRENT, ENDED
    }

    public EditTextWatcher(Context context, Object object) {
        if (!(object instanceof EditText)) {
            throw new ClassCastException(object.toString() +
                    context.getString(R.string.etw_constructor_error_class_cast));
        }
        mContext = context;
        mEditText = (EditText) object;
        countdown = DEFAULT_COUNTDOWN;
        arraySize = DEFAULT_ARRAY_DEQUE_SIZE;
        main();
    }

    public EditTextWatcher(Context context, Object object, int countdown) {
        if (!(object instanceof EditText)) {
            throw new ClassCastException(object.toString() +
                    context.getString(R.string.etw_constructor_error_class_cast));
        }
        mContext = context;
        mEditText = (EditText) object;
        this.countdown = (countdown < 1) ? DEFAULT_COUNTDOWN : countdown;
        arraySize = DEFAULT_ARRAY_DEQUE_SIZE;
        main();
    }

    public EditTextWatcher(Context context, Object object, int countdown, int arraySize) {
        if (!(object instanceof EditText)) {
            throw new ClassCastException(object.toString() +
                    context.getString(R.string.etw_constructor_error_class_cast));
        }
        mContext = context;
        mEditText = (EditText) object;
        this.countdown = (countdown < 1) ? DEFAULT_COUNTDOWN : countdown;
        this.arraySize = (arraySize < 1) ? DEFAULT_ARRAY_DEQUE_SIZE : arraySize;
        main();
    }

    private void main() {

        mEditText.addTextChangedListener(this);
        //mEditText.setOnClickListener(this);

        mSubtractStrings = new SubtractStrings();

        mArrayDequeUndo = new CustomArrayDeque<>(arraySize);
        mArrayDequeRedo = new CustomArrayDeque<>(arraySize);
        mArrayDequeUndoIndex = new CustomArrayDeque<>(arraySize);
        mArrayDequeRedoIndex = new CustomArrayDeque<>(arraySize);
        mArrayDequeUndoAlt = new CustomArrayDeque<>(arraySize);
        mArrayDequeRedoAlt = new CustomArrayDeque<>(arraySize);

        index = new Integer[]{0,0};

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                AlterationType mAlt;
                String storedString;
                newText = mEditText.getText().toString();

                storedString = mSubtractStrings.findAlteredText(oldText, newText);
                index = new Integer[]{
                        mSubtractStrings.getFirstDeviation(),
                        mSubtractStrings.getLastDeviation()};

                mAlt = mSubtractStrings.findAlterationType(oldText, newText);

                if (mAlt == AlterationType.REPLACEMENT) {
                    storedString = mSubtractStrings.findAlteredTextInContext(oldText.toCharArray(), newText.toCharArray());
                    index = new Integer[] {
                            mSubtractStrings.getFirstDeviation(),
                            mSubtractStrings.getLastDeviationNewText()
                    };
                }

                if (storedString != null) {
                    mArrayDequeUndoIndex.addFirst(index);
                    mArrayDequeUndoAlt.addFirst(mAlt);
                    mArrayDequeUndo.addFirst(storedString);
                }

                mTrackingState = TrackingState.ENDED;
            }
        };
        mTrackingState = TrackingState.ENDED;
    }

    public boolean isAutoSave() {
        return autoSaveSwitch;
    }

    public void setAutoSaveSwitch(boolean autoSaveSwitch) {
        this.autoSaveSwitch = autoSaveSwitch;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (autoSaveSwitch && (mTrackingState == TrackingState.ENDED)){
            oldText = mEditText.getText().toString();
            mHandler.postDelayed(mRunnable, countdown);
            mTrackingState = TrackingState.CURRENT;
        }
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

    }

    @Override
    public void onClick(View v) {
        if (autoSaveSwitch && mTrackingState == TrackingState.CURRENT) {
            mHandler.removeCallbacks(mRunnable);
            mHandler.post(mRunnable);
            mTrackingState = TrackingState.ENDED;
        }
    }

    public void undo(){
        mHandler.removeCallbacks(mRunnable);
        mTrackingState = TrackingState.STARTED;

        String temp;
        AlterationType tempAlt;
        Integer[] tempIndex;

        if (mArrayDequeUndo.peek() == null) {
            Toast.makeText(mContext, mContext.getString(R.string.etw_undo_array_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        temp = mArrayDequeUndo.poll();
        tempAlt = mArrayDequeUndoAlt.poll();
        tempIndex = mArrayDequeUndoIndex.poll();

        try {
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
        } catch (StringIndexOutOfBoundsException s) {
            Toast.makeText(mContext, s.toString(), Toast.LENGTH_SHORT).show();
            s.printStackTrace();
            clearAllArrayDequeue();
        }

        mArrayDequeRedo.addFirst(temp);
        mArrayDequeRedoAlt.addFirst(tempAlt);
        mArrayDequeRedoIndex.addFirst(tempIndex);
    }

    public void redo(){
        mHandler.removeCallbacks(mRunnable);
        mTrackingState = TrackingState.STARTED;

        String temp;
        AlterationType tempAlt;
        Integer[] tempIndex;

        if (mArrayDequeRedo.peek() == null) {
            Toast.makeText(mContext, mContext.getString(R.string.etw_redo_array_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        temp = mArrayDequeRedo.poll();
        tempAlt = mArrayDequeRedoAlt.poll();
        tempIndex = mArrayDequeRedoIndex.poll();

        try {
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
        } catch (StringIndexOutOfBoundsException s) {
            Toast.makeText(mContext, s.toString(), Toast.LENGTH_SHORT).show();
            s.printStackTrace();
            clearAllArrayDequeue();
        }

        mArrayDequeUndo.addFirst(temp);
        mArrayDequeUndoAlt.addFirst(tempAlt);
        mArrayDequeUndoIndex.addFirst(tempIndex);
    }

    private void clearAllArrayDequeue(){
        mArrayDequeUndo.clear();
        mArrayDequeUndoAlt.clear();
        mArrayDequeUndoIndex.clear();
        mArrayDequeRedo.clear();
        mArrayDequeRedoAlt.clear();
        mArrayDequeRedoIndex.clear();
    }

    private void sendLogInfo(String message) {
        Log.e(getClass().getSimpleName(), message);
    }

}
