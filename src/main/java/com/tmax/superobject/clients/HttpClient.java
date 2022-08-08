package com.tmax.superobject.clients;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {
    public static String testHttpRequest(String targetURL, String parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setRequestProperty("targetServiceName", "SaveJar");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(parameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream outputStream = new DataOutputStream (
                    connection.getOutputStream());
            outputStream.writeBytes(parameters);
            outputStream.close();

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
        HttpClient.testHttpRequest("http://127.0.0.1:8080", "dohyun'sExampleParameter");
    }
}
