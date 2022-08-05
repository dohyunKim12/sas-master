package com.tmax.superobject.object;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.constant.SapConstants.ContentType;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Base64;

public class MessageObject {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(MessageObject.class.getName());
    private HeaderObject header;
    private BodyObject body;
    private BodyObject responseBody;

    public MessageObject(HeaderObject header, BodyObject body) {
        this.header = header;
        if (header == null) {
            this.header = new DefaultHeaderObject();
        }
        this.body = body;
        if (body == null) {
            this.body = new DefaultBodyObject();
        }
    }

    public static MessageObject newInstanceFromJsonObject(JsonObject jsonObject) {
        JsonObject headerJson = jsonObject.get(SapConstants.HEADER_KEY).getAsJsonObject();
        JsonObject bodyJson = jsonObject.get(SapConstants.BODY_KEY).getAsJsonObject();
        HeaderObject header = new DefaultHeaderObject();
        header.setJsonObject(headerJson);
        header.setContentType(ContentType.TEXT);
        BodyObject body = new DefaultBodyObject();
        body.setJsonObject(bodyJson);
        return new MessageObject(header, body);
    }

    @Override
    public String toString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SapConstants.HEADER_KEY, header.getJsonObject());
        jsonObject.add(SapConstants.BODY_KEY, body.getJsonObject());
        return new Gson().toJson(jsonObject);
    }

    public void setHeader(HeaderObject header) {
        this.header = header;
    }

    public void setBody(BodyObject body) {
        this.body = body;
    }

    public HeaderObject header() {
        return header;
    }

    public BodyObject body() {
        return body;
    }

    public void setResponseBody(BodyObject responseBody) {
        this.responseBody = responseBody;
    }

    public BodyObject responseBody() {
        return responseBody;
    }

    /**
     * |4bytes magic|4bytes header length|header contents|4bytes body length|body|4bytes byte length|bytes|
     * @param binaryWebSocketFrame
     * @return {@link MessageObject}
     */
    public static MessageObject newInstanceFromBinaryWebSocketFrame(BinaryWebSocketFrame binaryWebSocketFrame) {
        // ByteBuffer magicByteBuffer = ByteBuffer.allocate(4);
        int magic = binaryWebSocketFrame.content().readInt();
        logger.info("decode binary websocket frame: " + magic);
        int headerlength = binaryWebSocketFrame.content().readInt();
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(headerlength);
        binaryWebSocketFrame.content().readBytes(headerByteBuffer);
        int bodyLength = binaryWebSocketFrame.content().readInt();
        ByteBuffer bodyByteBuffer = ByteBuffer.allocate(bodyLength);
        binaryWebSocketFrame.content().readBytes(bodyByteBuffer);
        int byteBufferLength = binaryWebSocketFrame.content().readInt();
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferLength);
        binaryWebSocketFrame.content().readBytes(byteBuffer);
        DefaultHeaderObject header = new DefaultHeaderObject();
        DefaultBodyObject body = new DefaultBodyObject();
        Gson gson = new Gson();
        header.setJsonObject(gson.fromJson(new String(headerByteBuffer.array()), JsonObject.class));
        header.setContentType(ContentType.BINARY);
        body.setJsonObject(gson.fromJson(new String(bodyByteBuffer.array()), JsonObject.class));
        body.setByteBuffer(byteBuffer);

        return new MessageObject(header, body);
    }

    public TextWebSocketFrame getAsTextWebSocketFrame() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SapConstants.HEADER_KEY, header.getJsonObject());
        jsonObject.add(SapConstants.BODY_KEY, body.getJsonObject());

        if (body.getByteBuffer() != null) {
            jsonObject.addProperty(SapConstants.BYTE_KEY,
                    Base64.getEncoder().encodeToString(body.getByteBuffer().array()));
        }

        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(gson.toJson(jsonObject));
        return textWebSocketFrame;
    }

    /**
     * |4bytes magic|4bytes header length|header contents|4bytes body length|body|4bytes byte length|bytes|
     */
    public BinaryWebSocketFrame getAsBinaryWebSocketFrame() {
        BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame();
        Gson gson = new Gson();
        String headerString = gson.toJson(header.getJsonObject());
        String bodyString = gson.toJson(body.getJsonObject());

        binaryWebSocketFrame.content()
                .writeInt(SapConstants.MAGIC)
                .writeInt(headerString.getBytes().length)
                .writeBytes(headerString.getBytes())
                .writeInt(bodyString.getBytes().length)
                .writeBytes(bodyString.getBytes());

        if (body.getByteBuffer() != null) {
            binaryWebSocketFrame.content()
                    .writeInt(body.getByteBuffer().remaining())
                    .writeBytes(body.getByteBuffer());
        }
        return binaryWebSocketFrame;
    }

}
