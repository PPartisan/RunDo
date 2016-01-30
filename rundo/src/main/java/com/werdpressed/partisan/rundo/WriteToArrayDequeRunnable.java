package com.werdpressed.partisan.rundo;

import java.lang.ref.WeakReference;

final class WriteToArrayDequeRunnable implements Runnable {

    private final WeakReference<WriteToArrayDeque> mWriteToArrayDeque;

    WriteToArrayDequeRunnable(WriteToArrayDeque writeToArrayDeque) {
        mWriteToArrayDeque = new WeakReference<>(writeToArrayDeque);
    }

    @Override
    public void run() {

        try {

            mWriteToArrayDeque.get().setIsRunning(true);

            String mNewString = mWriteToArrayDeque.get().getNewString();
            String mOldString = mWriteToArrayDeque.get().getOldString();

            SubtractStrings.Item mItem = new SubtractStrings(mOldString, mNewString).getItem();

            mWriteToArrayDeque.get().notifyArrayDequeDataReady(mItem);

            mWriteToArrayDeque.get().setIsRunning(false);

        } catch (NullPointerException e) {
            //Occurs on config change
        }

    }

}
