package com.tmax.superobject.object;

import com.google.gson.JsonObject;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.constant.SapConstants.ContentType;
import com.tmax.superobject.constant.SapConstants.MessageType;

public class DefaultHeaderObject implements HeaderObject{
    private JsonObject jsonObject = null;

    public DefaultHeaderObject() {
        super();
        jsonObject = new JsonObject();
    }

    @Override
    public JsonObject getJsonObject() {
        return jsonObject;
    }

    @Override
    public void setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String targetServiceName() {
        return jsonObject.get(SapConstants.HEADER_TARGET_SERVICE_NAME).getAsString();
    }

    @Override
    public void setTargetServiceName(String targetServiceName) {
        jsonObject.addProperty(SapConstants.HEADER_TARGET_SERVICE_NAME, targetServiceName);
    }

    @Override
    public void setRequestId(Long requestId) {
        jsonObject.addProperty(SapConstants.HEADER_REQUEST_ID, requestId);
    }

    @Override
    public Long requestId() {
        return jsonObject.get(SapConstants.HEADER_REQUEST_ID).getAsLong();
    }

    @Override
    public MessageType messageType() {
        return MessageType.valueOf(jsonObject.get(SapConstants.HEADER_MESSAGE_TYPE).getAsString());
    }

    @Override
    public void setMessageType(MessageType messageType) {
        jsonObject.addProperty(SapConstants.HEADER_MESSAGE_TYPE, messageType.name());
    }

    @Override
    public ContentType contentType() {
        return ContentType.valueOf(jsonObject.get(SapConstants.HEADER_CONTENT_TYPE).getAsString());
    }

    @Override
    public void setContentType(ContentType contentType) {
        jsonObject.addProperty(SapConstants.HEADER_CONTENT_TYPE, contentType.name());
    }
}
