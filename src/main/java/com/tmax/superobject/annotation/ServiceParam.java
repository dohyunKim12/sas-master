package com.tmax.superobject.annotation;

import com.tmax.superobject.object.BodyObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceParam {
    // String inputClassName();
    // String outputClassName();
    Class<? extends BodyObject> inputClass();
    Class<? extends BodyObject> outputClass();
}
