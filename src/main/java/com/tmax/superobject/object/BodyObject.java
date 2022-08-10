package com.tmax.superobject.object;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.CompositeByteBuf;

import java.nio.ByteBuffer;

public interface BodyObject {
    public JsonObject getJsonObject();
    public void setJsonObject(JsonObject jsonObject);
    public ByteBuffer getByteBuffer();
    public void setByteBuffer(ByteBuffer byteBuffer);
    public CompositeByteBuf getCompositeByteBuf();
    public void setCompositeByteBuf(CompositeByteBuf compositeByteBuf);
    public Class<? extends BodyObject> getBodyClass();
    public void setBodyClass(Class<? extends BodyObject> bodyClass);
    public JsonElement toJsonElement();
}
