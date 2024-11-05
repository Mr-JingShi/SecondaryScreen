package com.secondaryscreen.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public abstract class ServerChannel {
    private Thread mThread;
    private int mPort;
    public ServerChannel(int port) {
        mPort = port;
        mThread = new ServerChannelThread();
    }

    public void start() {
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    public abstract void work(byte[] buffer, int length);
    public void accept(String remoteAddress) { }

    private class ServerChannelThread extends Thread {
        public ServerChannelThread() {
            super("ServerChannelThread");
            System.out.println("ServerChannelThread");
        }
        @Override
        public void run() {
            try (Selector selector = Selector.open();
                 ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
                serverSocket.socket().bind(new InetSocketAddress(mPort));
                serverSocket.socket().setReuseAddress(true);
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                ByteBuffer headerBuffer = ByteBuffer.allocate(4);
                ByteBuffer eventBuffer = ByteBuffer.allocate(0);

                SocketChannel currentSocketChannel = null;

                boolean needRecvHeader = true;

                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select() != 0) {
                        Set keys = selector.selectedKeys();
                        Iterator iterator = keys.iterator();

                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            iterator.remove();

                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                SocketChannel socketChannel = server.accept();
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);

                                if (currentSocketChannel != null) {
                                    currentSocketChannel.close();
                                    System.out.println("close old sockectChannel");
                                }
                                currentSocketChannel = socketChannel;

                                SocketAddress socketAddress = socketChannel.socket().getRemoteSocketAddress();
                                if (socketAddress instanceof InetSocketAddress) {
                                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

                                    String remoteAddress = inetSocketAddress.getHostName();
                                    accept(remoteAddress);
                                } else {
                                    System.out.println("socketAddress is not InetSocketAddress");
                                }
                            } else if (key.isReadable()) {
                                SocketChannel socketChannel = (SocketChannel) key.channel();

                                try {
                                    if (needRecvHeader) {
                                        if (4 == recv(socketChannel, headerBuffer, 4)) {
                                            needRecvHeader = false;
                                        }
                                    } else {
                                        int len = headerBuffer.getInt();
                                        if (eventBuffer.capacity() < len) {
                                            System.out.println("need bigger len:" + len);
                                            eventBuffer = ByteBuffer.allocate(len);
                                        }
                                        if (len == recv(socketChannel, eventBuffer, len)) {
                                            needRecvHeader = true;

                                            work(eventBuffer.array(), len);
                                        }
                                    }
                                } catch (Exception e) {
                                    socketChannel.close();

                                    System.out.println("ServerChannelThread recv exception:" + e);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ServerChannelThread exception:" + e);
            }
        }

        private int recv(SocketChannel socketChannel, ByteBuffer buffer, int length) throws Exception {
            buffer.clear();
            buffer.limit(length);
            int len = socketChannel.read(buffer);
            buffer.flip();
            if (len == -1) {
                throw new RuntimeException("socket closed");
            }

            return len;
        }
    }
}