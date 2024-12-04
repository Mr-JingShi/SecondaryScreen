package com.secondaryscreen.server;

import android.util.Log;

/**
 * Log both to Android logger (so that logs are visible in "adb logcat") and standard output/error (so that they are visible in the terminal
 * directly).
 */
public final class Ln {
    public enum Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    private static Level threshold = Level.DEBUG;

    private Ln() {
        // not instantiable
    }

    /**
     * Initialize the log level.
     * <p>
     * Must be called before starting any new thread.
     *
     * @param level the log level
     */
    public static void initLogLevel(Level level) {
        threshold = level;
    }

    private static boolean isEnabled(Level level) {
        return level.ordinal() >= threshold.ordinal();
    }

    public static void v(String tag, String message) {
        if (isEnabled(Level.VERBOSE)) {
            Log.v(tag, message);
            print(tag, "VERBOSE", message);
        }
    }

    public static void d(String tag, String message) {
        if (isEnabled(Level.DEBUG)) {
            Log.d(tag, message);
            print(tag, "DEBUG", message);
        }
    }

    public static void i(String tag, String message) {
        if (isEnabled(Level.INFO)) {
            Log.i(tag, message);
            print(tag, "INFO", message);
        }
    }

    public static void w(String tag, String message, Throwable throwable) {
        if (isEnabled(Level.WARN)) {
            Log.w(tag, message, throwable);
            print(tag, "WARN", message);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    public static void w(String tag, String message) {
        w(tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        if (isEnabled(Level.ERROR)) {
            Log.e(tag, message, throwable);
            print(tag, "ERROR", message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    public static void e(String tag, String message) {
        e(tag, message, null);
    }

    private static void print(String tag, String level, String message) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        String className = stackTraceElement.getClassName();
        String methodName = stackTraceElement.getMethodName();
        int lineNumber = stackTraceElement.getLineNumber();
        String fileName = stackTraceElement.getFileName();

        StringBuilder sb = new StringBuilder();
        sb.append(level);
        sb.append(" ");
        sb.append(fileName);
        sb.append(":");
        sb.append(lineNumber);
        sb.append(" ");
        sb.append(className);
        sb.append(".");
        sb.append(methodName);
        sb.append(" ");
        sb.append(tag);
        sb.append(" ");
        sb.append(message);
        sb.append("\n");

        System.out.print(sb);
    }
}
