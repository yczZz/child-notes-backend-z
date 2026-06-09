package com.ycz.childnotesbackend.model.base;

public class Response<T> {

    public static final Response<Void> SUCCESS = new Response<>(
            ResponseStateFactory.getOk().state(),
            ResponseStateFactory.getOk().msgTemplate()
    );

    private String state;

    private String msg;

    private T data;

    public Response() {
    }

    public Response(T data) {
        this(ResponseStateFactory.getOk().state(), ResponseStateFactory.getOk().msgTemplate(), data);
    }

    public Response(String state, String msg) {
        this(state, msg, null);
    }

    public Response(String state, String msg, T data) {
        this.state = state;
        this.msg = msg;
        this.data = data;
    }

    public boolean isSuccess() {
        return ResponseStateFactory.getOk().state().equals(state);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

