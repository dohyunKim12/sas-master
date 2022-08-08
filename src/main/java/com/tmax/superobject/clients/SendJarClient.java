package com.tmax.superobject.clients;

import com.google.gson.JsonObject;
import com.tmax.superobject.Main;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.MessageObject;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class SendJarClient {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(SendJarClient.class.getName());
    public static String httpSendRequest(String targetURL, String filePath, String fileName) {
        HttpURLConnection connection = null;
        String fullPath = filePath + fileName;
        File binaryFile = new File(fullPath);

        try {
            //Create connection
            URL url = new URL(targetURL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setRequestProperty("targetServiceName", "SaveJar");
            connection.setRequestProperty("fileName", fileName);

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Make json
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(SapConstants.HEADER_KEY, new JsonObject());
            jsonObject.add(SapConstants.BODY_KEY, new JsonObject());
            MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
            messageObject.header().setMessageType(SapConstants.MessageType.REQUEST);
            messageObject.header().setTargetServiceName("SaveJar");
            messageObject.header().setRequestId(1L);
            messageObject.body().getJsonObject().addProperty("path", "./bin/tmp/super-app-server.jar");
            File file = new File(fullPath);
            try{
                messageObject.body().setByteBuffer(ByteBuffer.wrap(Files.readAllBytes(file.toPath())));
            } catch (IOException e){
                e.printStackTrace();
            }
            // Cannot append messageObject to outputStream... (messageObject.getAsBinaryWebSocketFrame() method returns BinaryWebSocketFrame TT...

            // Set OutputStream
            DataOutputStream outputStream = new DataOutputStream (connection.getOutputStream());

            Files.copy(binaryFile.toPath(), outputStream);
            outputStream.flush();

            logger.info("File Sent: " + binaryFile.getName());
            logger.info("Type of Sent file: " + URLConnection.guessContentTypeFromName(binaryFile.getName()));
            //Get Response
            InputStream inputStream = connection.getInputStream();
            // get response in BufferedReader
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            // Use StringBuffer to read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            logger.info("Response from server: " + response);
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    public static void main(String[] args) {
        SendJarClient.httpSendRequest("http://127.0.0.1:8080", "./bin/files/", "super-app-server.jar");
    }
}

