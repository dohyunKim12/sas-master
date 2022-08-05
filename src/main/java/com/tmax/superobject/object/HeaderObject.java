package com.tmax.superobject.object;

import com.google.gson.JsonObject;
import com.tmax.superobject.constant.SapConstants;

public interface HeaderObject {
    public void setJsonObject(JsonObject jsonObject);
    public JsonObject getJsonObject();
    public void setTargetServiceName(String targetServiceName);
    public String targetServiceName();
    public void setRequestId(Long requestId);
    public Long requestId();
    public SapConstants.MessageType messageType();
    public void setMessageType(SapConstants.MessageType messageType);
    public SapConstants.ContentType contentType();
    public void setContentType(SapConstants.ContentType contentType);
}
