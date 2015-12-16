package com.werdpressed.partisan.rundo;

public interface WriteToArrayDeque {

    String getNewString();
    String getOldString();

    void notifyArrayDequeDataReady(SubtractStrings.Item item);

    void setIsRunning(boolean isRunning);

}
