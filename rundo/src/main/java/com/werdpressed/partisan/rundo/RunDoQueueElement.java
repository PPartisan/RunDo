package com.werdpressed.partisan.rundo;

//Immutable
final class RunDoQueueElement {

    private final String mContent;
    private final Integer mDeviationType;
    private final Integer[] mDeviations;

    RunDoQueueElement(String content, Integer deviationType, Integer[] deviations) {
        mContent = content;
        mDeviationType = deviationType;
        mDeviations = deviations;
    }

    String getContent() {
        return mContent;
    }

    Integer getDeviationType() {
        return mDeviationType;
    }

    Integer[] getDeviations() {
        return mDeviations;
    }
}
