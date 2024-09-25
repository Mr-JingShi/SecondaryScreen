package com.secondaryscreen.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

// 部分逻辑参考自：
// https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/shellService/ServiceShellUtils.java

public class Shell {
    private Shell() {}

    public static Shell.Result execCommand(String command) {
        int result = -1;
        if (command == null) {
            return new Shell.Result(result, null, null);
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("sh");
            os = new DataOutputStream(process.getOutputStream());

            os.write(command.getBytes());
            os.writeBytes("\n");
            os.flush();

            os.writeBytes("exit\n");
            os.flush();

            result = process.waitFor();

            successMsg = new StringBuilder();
            errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                process.destroy();
            }
        }
        return new Shell.Result(result,
                                successMsg == null ? null : successMsg.toString(),
                                errorMsg == null ? null : errorMsg.toString()
                                );
    }

    public static class Result {
        public int mResult;
        public String mSuccessMsg;
        public String mErrorMsg;

        public Result(int result, String successMsg, String errorMsg) {
            this.mResult = result;
            this.mSuccessMsg = successMsg;
            this.mErrorMsg = errorMsg;
        }
    }
}
