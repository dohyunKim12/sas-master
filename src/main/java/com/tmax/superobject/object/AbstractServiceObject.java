package com.tmax.superobject.object;

public abstract class AbstractServiceObject implements ServiceObject {
    private boolean completed = false;
    private BodyObject replyObject = null;

    @Override
    public BodyObject getReply() {
        return this.replyObject;
    }

    @Override
    public boolean completed() {
        return completed;
    }

    @Override
    public void service(BodyObject message) {
        return;
    };

    @Override
    public void service(BodyObject message, String fileName) {
        return;
    };
    protected void setReply(BodyObject responseBody) {
        if (!completed) {
            this.replyObject = responseBody;
            completed = true;
        } else {
            System.out.println("replyObject is already set");
        }
    }
}
