package com.tmax.superobject.listener;

import com.tmax.superobject.object.BodyObject;

public interface CompletionListener {
    public void afterCompletion(BodyObject responseBody);
}
