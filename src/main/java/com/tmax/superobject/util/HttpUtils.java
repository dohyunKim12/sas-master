package com.tmax.superobject.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpUtils {
    private static final Logger logger = SuperAppDefaultLogger.getInstance().getLogger(HttpUtils.class.getName());

    public static JsonObject prepareJsonRequest(FullHttpRequest httpRequest)
    {
        JsonObject fullJsonRequest = new JsonObject();
        JsonObject jsonHeader;

        //POST, PUT
        if(httpRequest.content() != null && !httpRequest.content().hasArray() ) {
            if(logger.isDebugEnabled()){
                logger.debug("Request message has HTTP body : {}", httpRequest.content().toString(StandardCharsets.UTF_8));
            }
            fullJsonRequest = new Gson().fromJson(httpRequest.content().toString(StandardCharsets.UTF_8), JsonObject.class);

            if(fullJsonRequest.get(SapConstants.HEADER_KEY) == null){
                if(logger.isDebugEnabled()){
                    logger.debug("HTTP Body has not header");
                }
                if(httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME) != null) {
                    if(logger.isDebugEnabled()){
                        logger.debug("HTTP header has targetServiceName : {}", httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME));
                    }
                    jsonHeader = new JsonObject();
                    jsonHeader.addProperty(SapConstants.HEADER_TARGET_SERVICE_NAME, httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME));
                    fullJsonRequest.add(SapConstants.HEADER_KEY, jsonHeader);
                } else {
                    logger.error("Http request has no targetServiceName");
                    fullJsonRequest = new JsonObject();
                    fullJsonRequest.addProperty("error","TargetServiceName is not defined in elsewhere");
                }
            } else {
                jsonHeader = fullJsonRequest.get(SapConstants.HEADER_KEY).getAsJsonObject();
                if(logger.isDebugEnabled()){
                    logger.debug("HTTP Body has header : {}", jsonHeader);
                }
                if(jsonHeader.get(SapConstants.HEADER_TARGET_SERVICE_NAME) == null){
                    logger.error("Http request has no targetServiceName");
                    fullJsonRequest.addProperty("error","TargetServiceName is not defined in elsewhere");
                }
            }
        } else {    //GET, DELETE
            if(logger.isDebugEnabled()){
                logger.debug("Request message has no HTTP body");
            }
            if(httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME) != null){
                if(logger.isDebugEnabled()){
                    logger.debug("HTTP header has targetServiceName : {}", httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME));
                }
                jsonHeader = new JsonObject();
                JsonObject jsonBody = new JsonObject();
                jsonHeader.addProperty(SapConstants.HEADER_TARGET_SERVICE_NAME, httpRequest.headers().get(SapConstants.HEADER_TARGET_SERVICE_NAME));
                fullJsonRequest.add(SapConstants.HEADER_KEY,jsonHeader);
                fullJsonRequest.add(SapConstants.BODY_KEY,jsonBody);
            } else {
                logger.error("Http request has no targetServiceName");
                fullJsonRequest.addProperty("error","TargetServiceName must be defined in HTTP header");
            }
        }
        return fullJsonRequest;
    }

    public static FullHttpResponse prepareContinueHttpResponse(){
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);
        return response;
    }

    public static FullHttpResponse prepareBadHttpResponse(ByteBuf content){
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    public static FullHttpResponse prepareHttpResponse(ByteBuf content){
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        return response;
    }
}