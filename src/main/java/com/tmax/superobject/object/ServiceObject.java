package com.tmax.superobject.object;

public interface ServiceObject {
    public void service(BodyObject message);
    public void service(BodyObject message, String fileName);
    public void completeService();
    public boolean completed();
    public BodyObject getReply();

}
