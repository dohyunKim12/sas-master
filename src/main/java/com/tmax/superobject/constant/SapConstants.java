package com.tmax.superobject.constant;

public class SapConstants {
    public static final int MAGIC = 2022;
    public static final String HEADER_KEY = "header";
    public static final String BODY_KEY = "body";
    public static final String BYTE_KEY = "byte";
    public static final String HEADER_REQUEST_ID = "requestId";
    public static final String HEADER_TARGET_SERVICE_NAME = "targetServiceName";
    public static final String HEADER_MESSAGE_TYPE = "messageType";
    public static final String HEADER_CONTENT_TYPE = "contentType";

    /**
     * predefined values of key {@link SapConstants#HEADER_MESSAGE_TYPE}
     */
    public static enum MessageType {
        REQUEST, RESPONSE
    }

    /**
     * predefined values of key {@link SapConstants#HEADER_CONTENT_TYPE}
     */
    public static enum ContentType {
        TEXT, BINARY
    }
}
