package com.example.kitchenbrain.utils;

public class ChatUtils {
    public static String getChatId(String userA, String userB) {
        if (userA.compareTo(userB) < 0) {
            return userA + "_" + userB;
        } else {
            return userB + "_" + userA;
        }
    }
}
