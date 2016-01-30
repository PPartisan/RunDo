package com.werdpressed.partisan.rundo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;

/**
 * Implementation of {@link RunDo} which extends {@link Fragment}. It is best to create an
 * instance of this class with {@link com.werdpressed.partisan.rundo.RunDo.Factory}, rather than
 * with {@link #newInstance()} or {@link #RunDoSupport()} directly.
 *
 * @author Tom Calver
 */
public class RunDoSupport extends Fragment implements RunDo {

    private RunDo.TextLink mTextLink;
    private RunDo.Callbacks mCallbacks;

    private Handler mHandler;
    private WriteToArrayDequeRunnable mRunnable;
    private boolean isRunning;

    private long countdownTimerLength;
    private int queueSize;

    private FixedSizeArrayDeque<SubtractStrings.Item> mUndoQueue, mRedoQueue;

    private String mOldText, mNewText;
    private int trackingState;

    public RunDoSupport() {
        mHandler = new Handler();
        mRunnable = new WriteToArrayDequeRunnable(this);

        countdownTimerLength = DEFAULT_TIMER_LENGTH;
        queueSize = DEFAULT_QUEUE_SIZE;

        trackingState = TRACKING_ENDED;
    }

    public static RunDoSupport newInstance() {
        return new RunDoSupport();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mTextLink = (RunDo.TextLink) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement RunDo.TextLink");
        }

        if (context instanceof RunDo.Callbacks) mCallbacks = (RunDo.Callbacks) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mUndoQueue == null) mUndoQueue = new FixedSizeArrayDeque<>(queueSize);
        if (mRedoQueue == null) mRedoQueue = new FixedSizeArrayDeque<>(queueSize);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mUndoQueue = savedInstanceState.getParcelable(UNDO_TAG);
            mRedoQueue = savedInstanceState.getParcelable(REDO_TAG);

            mOldText = savedInstanceState.getString(OLD_TEXT_TAG);

            isRunning = savedInstanceState.getBoolean(CONFIG_CHANGE_TAG);

            if(isRunning) startCountdownRunnable();

            trackingState = TRACKING_STARTED;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mTextLink.getEditTextForRunDo().addTextChangedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(UNDO_TAG, mUndoQueue);
        outState.putParcelable(REDO_TAG, mRedoQueue);

        outState.putString(OLD_TEXT_TAG, mOldText);

        outState.putBoolean(CONFIG_CHANGE_TAG, isRunning);

        if (isRunning) stopCountdownRunnable();

    }

    @Override
    public void onDetach() {
        mTextLink = null;
        super.onDetach();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        if (mOldText == null) mOldText = mTextLink.getEditTextForRunDo().getText().toString();

        if (trackingState == TRACKING_ENDED) {

            //Redo Queue should only be required as response to Undo calls. Otherwise clear.
            mRedoQueue.clear();

            startCountdownRunnable();
            trackingState = TRACKING_CURRENT;
        }

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        switch (trackingState) {
            case TRACKING_STARTED:
                trackingState = TRACKING_ENDED;
                break;
            case TRACKING_CURRENT:
                restartCountdownRunnable();
                break;
        }

    }

    @Override
    public void afterTextChanged(Editable s) {
        //Unused
    }

    /**
     *
     * @see {@link WriteToArrayDeque#getNewString()}
     */
    @Override
    public String getNewString() {
        mNewText = mTextLink.getEditTextForRunDo().getText().toString();
        return mNewText;
    }

    /**
     *
     * @see {@link WriteToArrayDeque#getOldString()}
     */
    @Override
    public String getOldString() {
        return mOldText;
    }

    /**
     *
     * @see {@link WriteToArrayDeque#notifyArrayDequeDataReady(com.werdpressed.partisan.rundo.SubtractStrings.Item)}
     */
    @Override
    public void notifyArrayDequeDataReady(SubtractStrings.Item item) {

        mUndoQueue.addFirst(item);

        mOldText = mTextLink.getEditTextForRunDo().getText().toString();

        trackingState = TRACKING_ENDED;

    }

    /**
     *
     * @see {@link WriteToArrayDeque#setIsRunning(boolean)}
     */
    @Override
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     *
     * @see {@link RunDo#setQueueSize(int)}
     */
    @Override
    public void setQueueSize(int size) {
        queueSize = size;
        mUndoQueue = new FixedSizeArrayDeque<>(queueSize);
        mRedoQueue = new FixedSizeArrayDeque<>(queueSize);
    }

    /**
     *
     * @see {@link RunDo#setTimerLength(long)}
     */
    @Override
    public void setTimerLength(long lengthInMillis) {
        countdownTimerLength = lengthInMillis;
    }

    /**
     *
     * @see {@link RunDo#undo()}
     */
    @Override
    public void undo() {

        if (isRunning) {
            restartCountdownRunnableImmediately();
            return;
        }

        trackingState = TRACKING_STARTED;

        if (mUndoQueue.peek() == null) {
            //Log.e(TAG, "Undo Queue Empty");
            return;
        }

        try {

            SubtractStrings.Item temp = mUndoQueue.poll();

            switch (temp.getDeviationType()) {
                case SubtractStrings.ADDITION:
                    mTextLink.getEditTextForRunDo().getText().delete(
                            temp.getFirstDeviation(),
                            temp.getLastDeviationNewText()
                    );
                    break;
                case SubtractStrings.DELETION:
                    mTextLink.getEditTextForRunDo().getText().insert(
                            temp.getFirstDeviation(),
                            temp.getReplacedText());
                    break;
                case SubtractStrings.REPLACEMENT:
                    mTextLink.getEditTextForRunDo().getText().replace(
                            temp.getFirstDeviation(),
                            temp.getLastDeviationNewText(),
                            temp.getReplacedText());
                    break;
                case SubtractStrings.UNCHANGED:
                    break;
                default:
                    break;
            }

            mTextLink.getEditTextForRunDo().setSelection(temp.getFirstDeviation());

            mRedoQueue.addFirst(temp);

            if (mCallbacks != null) mCallbacks.undoCalled();

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } finally {
            mOldText = mTextLink.getEditTextForRunDo().getText().toString();
        }

    }

    /**
     *
     * @see {@link RunDo#redo()}
     */
    @Override
    public void redo() {

        trackingState = TRACKING_STARTED;

        if (mRedoQueue.peek() == null) {
            //Log.e(TAG, "Redo Queue Empty");
            return;
        }

        try {

            SubtractStrings.Item temp = mRedoQueue.poll();

            switch (temp.getDeviationType()) {
                case SubtractStrings.ADDITION:
                    mTextLink.getEditTextForRunDo().getText().insert(
                            temp.getFirstDeviation(),
                            temp.getAlteredText());
                    break;
                case SubtractStrings.DELETION:
                    mTextLink.getEditTextForRunDo().getText().delete(
                            temp.getFirstDeviation(),
                            temp.getLastDeviationOldText()
                    );
                    break;
                case SubtractStrings.REPLACEMENT:
                    mTextLink.getEditTextForRunDo().getText().replace(
                            temp.getFirstDeviation(),
                            temp.getLastDeviationOldText(),
                            temp.getAlteredText());
                    break;
                case SubtractStrings.UNCHANGED:
                    break;
                default:
                    break;

            }

            mTextLink.getEditTextForRunDo().setSelection(temp.getFirstDeviation());

            mUndoQueue.addFirst(temp);

            if (mCallbacks != null) mCallbacks.redoCalled();

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } finally {
            mOldText = mTextLink.getEditTextForRunDo().getText().toString();
        }

    }

    /**
     *
     * @see {@link RunDo#clearAllQueues()}
     */
    @Override
    public void clearAllQueues() {
        mUndoQueue.clear();
        mRedoQueue.clear();
    }

    private void restartCountdownRunnableImmediately() {
        stopCountdownRunnable();
        mHandler.post(mRunnable);
    }

    private void startCountdownRunnable() {
        isRunning = true;
        mHandler.postDelayed(mRunnable, countdownTimerLength);
    }

    private void stopCountdownRunnable() {
        mHandler.removeCallbacks(mRunnable);
        isRunning = false;
    }

    private void restartCountdownRunnable() {
        stopCountdownRunnable();
        startCountdownRunnable();
    }

}