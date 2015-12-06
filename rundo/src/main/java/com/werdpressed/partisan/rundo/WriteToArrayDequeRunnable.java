package com.werdpressed.partisan.rundo;

import java.lang.ref.WeakReference;

final class WriteToArrayDequeRunnable implements Runnable {

    private final WeakReference<WriteToArrayDeque> mWriteToArrayDeque;

    WriteToArrayDequeRunnable(WriteToArrayDeque writeToArrayDeque) {
        mWriteToArrayDeque = new WeakReference<>(writeToArrayDeque);
    }

    @Override
    public void run() {

        mWriteToArrayDeque.get().setIsRunning(true);

        String mNewString = mWriteToArrayDeque.get().getNewString();
        String mOldString = mWriteToArrayDeque.get().getOldString();

        SubtractStrings mSubtractStrings = new SubtractStrings(mOldString, mNewString);

        mWriteToArrayDeque.get().notifyArrayDequeDataReady(mSubtractStrings);

        mWriteToArrayDeque.get().setIsRunning(false);

    }

}
