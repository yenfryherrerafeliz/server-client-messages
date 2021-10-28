package com.hatechnology.apps.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

public class HAStringUtils {
    public static String generateRandomString(int length){
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder rdmStr = new StringBuilder();

        for (int i = 0; i <= length; i++){
            int index = (int)(alphaNumericString.length() * Math.random());

            rdmStr.append(alphaNumericString.charAt(index));
        }

        return rdmStr.toString();
    }

    public static String lpad(String text, int length, String fillWith) {
        StringBuilder strBuilder = new StringBuilder();

        if (text.length() > length)
            return text;

        for (int i = 0; i < (length - text.length()); i++){
            strBuilder.append(fillWith);
        }

        strBuilder.append(text);

        return strBuilder.toString();
    }

    public static String encodeStringToBase64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    public static String decodeBase64ToString(String base64Text) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64Text);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder content = new StringBuilder();
        String line;

        while ( (line = reader.readLine()) != null){
            content.append(line);
        }

        return content.toString();
    }
}
