package com.werdpressed.partisan.rundo;

public interface WriteToArrayDeque {

    String getNewString();
    String getOldString();

    void notifyArrayDequeDataReady(SubtractStrings subtractStrings);

    void setIsRunning(boolean isRunning);

}
