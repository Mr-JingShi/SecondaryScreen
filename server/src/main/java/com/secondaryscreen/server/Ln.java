package com.secondaryscreen.server;

import android.util.Log;

import java.io.OutputStream;

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
            System.out.print("VERBOSE " + tag + " " + message + "\n");
        }
    }

    public static void d(String tag, String message) {
        if (isEnabled(Level.DEBUG)) {
            Log.d(tag, message);
            System.out.print("DEBUG " + tag + " " + message + "\n");
        }
    }

    public static void i(String tag, String message) {
        if (isEnabled(Level.INFO)) {
            Log.i(tag, message);
            System.out.print("INFO " + tag + " " + message + "\n");
        }
    }

    public static void w(String tag, String message, Throwable throwable) {
        if (isEnabled(Level.WARN)) {
            Log.w(tag, message, throwable);
            System.out.print("WARN " + tag + " " + message + "\n");
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
            System.err.print("ERROR " + tag + " " + message + "\n");
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    public static void e(String tag, String message) {
        e(tag, message, null);
    }
}
