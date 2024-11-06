package com.secondaryscreen.server;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public final class DisplayConnection extends ServerChannel {
    private static String TAG = "DisplayConnection";
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private int mDensityDpi;
    private String mRemoteAddress;
    public DisplayConnection(int width, int height, int rotation, int densityDpi) {
       super(Utils.DISPLAY_CHANNEL_PORT);

        mWidth = width;
        mHeight = height;
        mRotation = rotation;
        mDensityDpi = densityDpi;
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
            Ln.w(TAG, "resizeVirtualDisplay exception", e);
        }
    }

    @Override
    public void work(byte[] buffer, int length) {
        // 1200,1920,240
        String displayInfo = new String(buffer, 0, length);
        Ln.d(TAG, "displayInfo:" + displayInfo);
        String[] split = displayInfo.split(",");
        int flag = Integer.parseInt(split[0]);
        int width = Integer.parseInt(split[1]);
        int height = Integer.parseInt(split[2]);
        int rotation = Integer.parseInt(split[3]);
        int densityDpi = Integer.parseInt(split[4]);

        // 0:Local 1:Remote
        Utils.setSingleMachineMode(flag == 0);

        Ln.d(TAG, "old width:" + mWidth + " height:" + mHeight + " rotation:" + mRotation + " densityDpi:" + mDensityDpi);
        Ln.d(TAG, "new width:" + width + " height:" + height + " rotation:" + rotation + " densityDpi:" + densityDpi);

        boolean needResize = false;
        boolean needRotate = false;
        if (mWidth != width || mHeight != height || mDensityDpi != densityDpi) {
            mWidth = width;
            mHeight = height;
            mDensityDpi = densityDpi;
            needResize = true;
        }
        if (mRotation != rotation) {
            mRotation = rotation;
            needRotate = true;
        }
        if (needResize || needRotate) {
            if (mRotation % 2 == 1) {
                ServiceManager.getDisplayManager().forceDisplayInfo(mHeight, mWidth, mRotation, mDensityDpi);
            } else {
                ServiceManager.getDisplayManager().forceDisplayInfo(mWidth, mHeight, mRotation, mDensityDpi);
            }

            if (needResize) {
                resizeVirtualDisplay(width, height, densityDpi);
            }
            if (needRotate) {
                changeVirtualDisplayRotation(rotation);
            }
        }

        if (mRemoteAddress != null) {
            ServiceManager.getDisplayManager().onDisplayChanged(mRemoteAddress);
            mRemoteAddress = null;
        }
    }

    @Override
    public void accept(SocketChannel socketChannel) {
        super.accept(socketChannel);

        SocketAddress socketAddress = socketChannel.socket().getRemoteSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            // getHostName有时会查询NDS，8～10秒才会退出，所以改用getHostAddress
            // mRemoteAddress = inetSocketAddress.getHostName();
            mRemoteAddress = inetSocketAddress.getAddress().getHostAddress();
        } else {
            Ln.w(TAG, "socketAddress is not InetSocketAddress");
        }
    }
}
