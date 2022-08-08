package com.tmax.superobject.clients;

import com.tmax.superobject.Main;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class HttpSendFileClient {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(HttpSendFileClient.class.getName());
    public static String httpSendRequest(String targetURL) {
        HttpURLConnection connection = null;
        String charset = "UTF-8";
        String parameters = "exparam";
        File textFile = new File("./bin/files/ex.txt");
        File binaryFile = new File("./bin/files/super-app-server.jar");
        File imgFile = new File("./bin/files/plant.jpg");
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just to generate some unique random value.
        String CRLF = "\r\n";

        try {
            //Create connection
            URL url = new URL(targetURL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setRequestProperty("targetServiceName", "SaveJar");
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("Content-Length", Integer.toString(parameters.getBytes().length));
//            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);


            // Set OutputStream
            DataOutputStream outputStream = new DataOutputStream (connection.getOutputStream());
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, Charset.defaultCharset()),true);

//            //Send normal param.
//            writer.append("Content-type: text/plain; charset=" + Charset.defaultCharset()+"\n");
//            writer.append(parameters).flush();

//            // Send text file.
//            writer.append("--" + boundary).append(CRLF);
//            writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
//            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
//            writer.append(CRLF).flush();
//            Files.copy(textFile.toPath(), outputStream);
//            outputStream.flush(); // Important before continuing with writer!
//            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // Send binary file.
            logger.info("sendfile: " + binaryFile.getName());
            logger.info("Content-type of sending file: " + URLConnection.guessContentTypeFromName(binaryFile.getName()));
            Files.copy(binaryFile.toPath(), outputStream);
            outputStream.flush();

            // End of multipart/form-data.
//            writer.append("\n--" + boundary + "--").append(CRLF).flush();

//            ////////////////////////////////////////////////////////////////
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
//            return  null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    public static void main(String[] args) throws Exception{
        try{
            HttpSendFileClient.httpSendRequest("http://127.0.0.1:8080");

        }finally {

        }

    }
}

