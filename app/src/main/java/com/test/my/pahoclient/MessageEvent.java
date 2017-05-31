package com.test.my.pahoclient;

/**
 * Created by Administrator on 2017/5/31 0031.
 */

class MessageEvent {
    String message;
    String type;

    MessageEvent(String type,String message) {
        this.message = message;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
