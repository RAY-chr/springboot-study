package com.chr.fservice.upload.nio;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * @author RAY
 * @descriptions
 * @since 2020/3/22
 */
public class FileServer {
    private static final int bufferSize = 8192;
    private static final int PORT = 6667;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public static void main(String[] args) throws IOException {
        FileServer server = new FileServer();
        server.listen();
    }

    public FileServer() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1", PORT));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.err.println("[FileServer] 服务器启动成功!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器的监听器
     */
    public void listen() throws IOException {
        while (true) {
            int select = selector.select();
            if (select > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        try {
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        } catch (IOException ignored) {
                        }
                    }
                    if (key.isReadable()) {
                        read(key);
                    }
                    //此操作必不可少
                    keyIterator.remove();
                }
            }
        }
    }


    /**
     * 根据客户端前置信息判断是客户端上传还是下载，然后进行相应的传输
     *
     * @param key SelectionKey
     */
    public void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        try {
            socketChannel.read(buffer);
        } catch (IOException e) {
            try {
                System.out.println(socketChannel.getRemoteAddress() + "离线了");
            } catch (IOException ignored) {
            }
            close(null, socketChannel, key);
            return;
        }
        buffer.flip();
        int pathLength;
        try {
            pathLength = buffer.getInt();
        } catch (BufferUnderflowException e) {
            System.err.println("no bytes to read");
            close(null, socketChannel, key);
            return;
        }
        byte b = buffer.get();
        byte[] msg = new byte[pathLength];
        buffer.get(msg);
        String targetPath = new String(msg, StandardCharsets.UTF_8);
        FileChannel fileChannel = null;
        try {
            if (b == 1) { // 客户端上传文件
                System.err.println("客户端上传的文件路径为 ==> " + targetPath);
                try {
                    fileChannel = new FileOutputStream(targetPath).getChannel();
                } catch (FileNotFoundException e) {
                    writeError("File Not Found", buffer, socketChannel);
                    close(null, socketChannel, key);
                    return;
                }
                writeSuccess(buffer, socketChannel);
                int write = 0;
                long start = System.currentTimeMillis();
                buffer.clear();
                while (socketChannel.read(buffer) != -1) {
                    buffer.flip();
                    write += fileChannel.write(buffer);
                    buffer.clear();
                }
                long end = System.currentTimeMillis();
                System.err.println(targetPath + " ==> 上传完成! 一共[" + write + "]字节 " +
                        "花费" + (end - start) + "毫秒");
                buffer.putInt(1);
                buffer.flip();
                socketChannel.write(buffer);
            }
            if (b == 0) {  //客户端下载文件
                System.err.println("客户端下载的文件路径为 ==> " + targetPath);
                try {
                    fileChannel = new FileInputStream(targetPath).getChannel();
                } catch (FileNotFoundException e) {
                    writeError("File Not Found", buffer, socketChannel);
                    close(null, socketChannel, key);
                    return;
                }
                writeSuccess(buffer, socketChannel);
                buffer.clear();
                int totalRead = 0;
                int read;
                int write = 0;
                while ((read = fileChannel.read(buffer)) != -1) {
                    buffer.flip();
                    totalRead += read;
                    write += socketChannel.write(buffer);
                    /**
                     * socketChannel.write(buffer) 不一定能写完数据 所以需要判断是否有剩余
                     */
                    while (buffer.hasRemaining()) {
                        write += socketChannel.write(buffer);
                    }
                    buffer.clear();
                }
                System.err.println("一共从文件读出 ==> " + totalRead + "字节");
                System.err.println("一共写出 ==> " + write + "字节");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(fileChannel, socketChannel, key);
        }

    }

    private void close(FileChannel fileChannel, SocketChannel socketChannel, SelectionKey key) {
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
            socketChannel.close();
            key.cancel();
        } catch (IOException ignored) {
        }
    }

    /**
     * 写入失败的信息（特定格式）给客户端
     *
     * @param msg           String
     * @param buffer        ByteBuffer
     * @param socketChannel SocketChannel
     * @throws IOException IOException
     */
    private void writeError(String msg, ByteBuffer buffer, SocketChannel socketChannel) throws IOException {
        buffer.clear();
        byte[] info = msg.getBytes();
        buffer.putInt(info.length);
        buffer.put((byte) 0);
        buffer.put(info);
        buffer.flip();
        socketChannel.write(buffer);
    }

    /**
     * 客户端请求的路径服务器有则返回此标志
     *
     * @param buffer        ByteBuffer
     * @param socketChannel SocketChannel
     * @throws IOException IOException
     */
    private void writeSuccess(ByteBuffer buffer, SocketChannel socketChannel) throws IOException {
        buffer.clear();
        buffer.putInt(1);
        buffer.put((byte) 1);
        buffer.flip();
        socketChannel.write(buffer);
    }

}
