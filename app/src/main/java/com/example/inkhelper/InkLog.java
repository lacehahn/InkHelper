package com.example.inkhelper;

import java.lang.reflect.Method;

public final class InkLog {
    private static final String TAG = "InkHelper";

    private InkLog() {
    }

    public static void d(String message) {
        write("d", message, null);
    }

    public static void w(String message, Throwable throwable) {
        write("w", message, throwable);
    }

    private static void write(String methodName, String message, Throwable throwable) {
        String line = message == null ? "" : message;
        try {
            Class<?> logClass = Class.forName("android.util.Log");
            Method method = logClass.getMethod(methodName, String.class, String.class);
            method.invoke(null, TAG, throwable == null ? line : line + " error=" + throwable.getClass().getSimpleName()
                    + " message=" + sanitize(throwable.getMessage()));
        } catch (Throwable ignored) {
            System.out.println(TAG + " " + line
                    + (throwable == null ? "" : " error=" + throwable.getClass().getSimpleName()
                    + " message=" + sanitize(throwable.getMessage())));
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
