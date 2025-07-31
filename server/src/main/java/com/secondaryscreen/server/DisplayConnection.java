package com.secondaryscreen.server;

import java.util.concurrent.TimeUnit;

public final class DisplayConnection extends ServerChannel {
    private static String TAG = "DisplayConnection";
    public DisplayConnection() {
       super(Utils.DISPLAY_CHANNEL_PORT);
    }

    @Override
    public void work(byte[] buffer, int length) {
        String displayInfo = new String(buffer, 0, length);
        Ln.d(TAG, "displayInfo:" + displayInfo);
        String[] infos = displayInfo.split(",");
        int displayId = Integer.parseInt(infos[0]);
        SecondaryDisplayLauncher.startSelfSecondaryLauncher(displayId);
        if (infos.length > 2) {
            int index = Integer.parseInt(infos[1]);
            String activityName = infos[2];
            Utils.schedule(() -> {
                Utils.startActivity(activityName, displayId);
                Utils.schedule(() -> {
                    Utils.startActivity(activityName, displayId);
                }, 5, TimeUnit.SECONDS);
            }, index + 1, TimeUnit.SECONDS);
        }
    }
}
