package com.tmax.superobject.object;

import com.google.gson.JsonObject;
import io.netty.buffer.CompositeByteBuf;

import java.nio.ByteBuffer;

public class DefaultBodyObject extends AbstractBodyObject {

    public DefaultBodyObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }
    public DefaultBodyObject(CompositeByteBuf compositeByteBuf) {
        this.compositeByteBuf = compositeByteBuf;
    }
    public DefaultBodyObject() {}

}