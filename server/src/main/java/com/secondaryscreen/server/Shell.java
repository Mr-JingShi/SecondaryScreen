package com.secondaryscreen.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Shell {
    public static void exec(String... cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command " + Arrays.toString(cmd) + " returned with value " + exitCode);
        }
    }
    public static String execReadOutput(String... cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(process.getInputStream());
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine()).append('\n');
        }
        String output = builder.toString();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command " + Arrays.toString(cmd) + " returned with value " + exitCode);
        }
        return output;
    }
}
