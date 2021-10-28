package com.hatechnology.apps.utilities;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class JsonUtils {
    private static final Gson m_Gson = new Gson();

    public static String convertToJson(Object obj){
        return m_Gson.toJson(obj);
    }

    public static Object convertFromJsonToObject(String jsonStr, Type classType){
        return m_Gson.fromJson(jsonStr, classType);
    }
}
