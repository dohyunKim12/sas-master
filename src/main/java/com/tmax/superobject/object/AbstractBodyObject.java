package com.tmax.superobject.object;

import com.google.gson.JsonObject;

import java.nio.ByteBuffer;

public abstract class AbstractBodyObject implements BodyObject {
    protected JsonObject jsonObject = null;
    protected ByteBuffer byteBuffer = null;

    @Override
    public String toString() {
        if (jsonObject != null) {
            return jsonObject.toString();
        } else {
            return null;
        }
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
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }
}
