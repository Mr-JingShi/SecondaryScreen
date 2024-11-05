package com.secondaryscreen.server;

import android.media.MediaCodec;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/Streamer.java

public final class Streamer {
    private OutputStream mOutputStream;
    private final ByteBuffer mHeaderBuffer;
    private final byte[] mHeader;
    private byte[] mCodec;
    public Streamer() {
        mHeaderBuffer = ByteBuffer.allocate(20);
        mHeader = new byte[20];
        mCodec = new byte[0];
    }

    public void setSocket(Socket socket) throws IOException {
        this.mOutputStream = socket.getOutputStream();
    }

    public void writeVideoHeader(Size videoSize) throws IOException {
        mHeaderBuffer.clear();
        mHeaderBuffer.putInt(videoSize.getWidth());
        mHeaderBuffer.putInt(videoSize.getHeight());
        mHeaderBuffer.flip();
        mHeaderBuffer.get(mHeader, 0, 8);

        mOutputStream.write(mHeader, 0, 8);
    }

    public void writePacket(ByteBuffer codecBuffer, MediaCodec.BufferInfo bufferInfo) throws IOException {
        mHeaderBuffer.clear();
        mHeaderBuffer.putInt(bufferInfo.flags);
        mHeaderBuffer.putInt(bufferInfo.offset);
        mHeaderBuffer.putLong(bufferInfo.presentationTimeUs);
        mHeaderBuffer.putInt(bufferInfo.size);
        mHeaderBuffer.flip();
        mHeaderBuffer.get(mHeader, 0, mHeader.length);

        mOutputStream.write(mHeader, 0, mHeader.length);

        if (mCodec.length < bufferInfo.size) {
            mCodec = new byte[bufferInfo.size];
        }
        codecBuffer.position(bufferInfo.offset);
        codecBuffer.limit(bufferInfo.offset + bufferInfo.size);
        codecBuffer.get(mCodec, 0, bufferInfo.size);

        mOutputStream.write(mCodec, 0, bufferInfo.size);
        mOutputStream.flush();
    }
}
