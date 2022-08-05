package com.tmax.superobject.object;

import com.google.gson.JsonObject;

import java.nio.ByteBuffer;

public interface BodyObject {
    public JsonObject getJsonObject();
    public void setJsonObject(JsonObject jsonObject);
    public ByteBuffer getByteBuffer();
    public void setByteBuffer(ByteBuffer byteBuffer);
}
