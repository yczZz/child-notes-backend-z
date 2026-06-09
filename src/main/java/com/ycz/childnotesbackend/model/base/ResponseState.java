package com.ycz.childnotesbackend.model.base;

public class ResponseState {

    private final String state;

    private final String msgTemplate;

    public ResponseState(String state, String msgTemplate) {
        this.state = state;
        this.msgTemplate = msgTemplate;
    }

    public String state() {
        return state;
    }

    public String msgTemplate() {
        return msgTemplate;
    }
}

