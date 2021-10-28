package com.hatechnology.apps.utilities;

import java.io.*;
import java.util.Base64;

public class HAFileHelper {

    public static boolean createFile(String filePath, String content) throws IOException {
        String[] treePath = filePath.split("/");
        StringBuilder pathBuilder = new StringBuilder();

        for (String path: treePath){
            pathBuilder.append("/").append(path);

            if (path.equals(treePath[treePath.length - 1]))
                break;

            File file = new File(pathBuilder.toString());

            if ( !file.exists()){
                file.mkdir();
            }
        }

        File newFile = new File(pathBuilder.toString());
        PrintWriter writer = new PrintWriter(new FileWriter(newFile));
        writer.append(content);
        writer.close();

        return true;
    }

    public static String getFileContent(String path) throws IOException {
        return getFileContent(new File(path));
    }

    public static String getFileContent(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        StringBuilder content = new StringBuilder();
        String line;

        while ( (line = reader.readLine()) != null){
            content.append(line);
        }

        reader.close();

        return content.toString();
    }

    public static File[] listFilesFromDir(String path) {
        return new File(path).listFiles();
    }

    public static void deleteFile(String path) {
        new File(path).delete();
    }

    public static String encodeFileToBase64(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];

        fileInputStream.read(bytes);

        fileInputStream.close();

        return Base64.getEncoder().encodeToString(bytes);
    }

    public static File decodeFileFromBase64(String base64File, String outputFile) throws IOException {
        byte[] bytesDecoded = Base64.getDecoder().decode(base64File.getBytes());

        File file = new File(outputFile);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(bytesDecoded);
        fileOutputStream.close();

        return file;
    }

    public static String getFileExtension(String fileName) {
        StringBuilder ext = new StringBuilder();

        char[] charName = fileName.toCharArray();

        for (int i = charName.length - 1; i > -1; i--){

            if (charName[i] == '.'){break;}

            ext.append(charName[i]);
        }

        return ext.reverse().toString();
    }
}
