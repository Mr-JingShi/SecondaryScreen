package com.secondaryscreen.server;

import android.view.IRotationWatcher;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class DisplayConnection extends ServerChannel {
    private static String TAG = "DisplayConnection";
    private static int PORT = 8404;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private int mDensityDpi;
    public DisplayConnection(int width, int height, int rotation, int densityDpi) {
       super(PORT);

        mWidth = width;
        mHeight = height;
        mRotation = rotation;
        mDensityDpi = densityDpi;

        ServiceManager.getWindowManager().registerRotationWatcher(new IRotationWatcher.Stub() {
            @Override
            public void onRotationChanged(int rotation) {
                System.out.println("onRotationChanged rotation:" + rotation);

                synchronized (Utils.class) {
                    if (Utils.isSingleMachineMode()) {
                        ServiceManager.getDisplayManager().getDisplayInfo(true);

                        changeVirtualDisplayRotation(rotation);
                    }
                }
            }
        }, 0);
    }

    private void changeVirtualDisplayRotation(int rotation) {
        int displayId = DisplayInfo.getMirrorDisplayId();

        if (!ServiceManager.getWindowManager().isRotationFrozen(displayId)) {
            ServiceManager.getWindowManager().thawRotation(displayId);
        }

        ServiceManager.getWindowManager().freezeRotation(displayId, rotation);

        ServiceManager.getWindowManager().onRotationChanged(rotation);
    }

    private void resizeVirtualDisplay(int width, int height, int densityDpi) {
        try {
            SurfaceControl.resizeVirtualDisplay(width, height, densityDpi);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("resizeVirtualDisplay exception:" + e);
        }
    }

    @Override
    public void work(byte[] buffer, int length) {
        // 1200,1920,240
        String displayInfo = new String(buffer, 0, length);
        System.out.println("displayInfo:" + displayInfo);
        String[] split = displayInfo.split(",");
        int flag = Integer.parseInt(split[0]);
        int width = Integer.parseInt(split[1]);
        int height = Integer.parseInt(split[2]);
        int rotation = Integer.parseInt(split[3]);
        int densityDpi = Integer.parseInt(split[4]);

        // 0:Local 1:Remote
        Utils.setSingleMachineMode(flag == 0);

        System.out.println("old width:" + mWidth + " height:" + mHeight + " rotation:" + mRotation + " densityDpi:" + mDensityDpi);
        System.out.println("new width:" + width + " height:" + height + " rotation:" + rotation + " densityDpi:" + densityDpi);

        boolean changed = false;
        if (mWidth != width || mHeight != height || mDensityDpi != densityDpi) {
            mWidth = width;
            mHeight = height;
            mDensityDpi = densityDpi;

            resizeVirtualDisplay(width, height, densityDpi);
            changed = true;
        }
        if (mRotation != rotation) {
            mRotation = rotation;

            changeVirtualDisplayRotation(rotation);
            changed = true;
        }
        if (changed) {
            if (mRotation % 2 == 1) {
                ServiceManager.getDisplayManager().forceDisplayInfo(mHeight, mWidth, mRotation, mDensityDpi);
            } else {
                ServiceManager.getDisplayManager().forceDisplayInfo(mWidth, mHeight, mRotation, mDensityDpi);
            }
        }
    }

    @Override
    public void accept(SocketAddress socketAddress) {
        System.out.println("socketAddress:" + socketAddress);
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

            String remoteAddress = inetSocketAddress.getHostName();
            System.out.println("remoteAddress:" + remoteAddress);
            ServiceManager.getDisplayManager().onDisplayChanged(remoteAddress);
        } else {
            System.out.println("socketAddress is not InetSocketAddress");
        }
    }
}
