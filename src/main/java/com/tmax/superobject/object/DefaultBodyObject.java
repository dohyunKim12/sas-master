package com.tmax.superobject.object;

import com.google.gson.JsonObject;

public class DefaultBodyObject extends AbstractBodyObject {

    public DefaultBodyObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public DefaultBodyObject() {
    }

}
