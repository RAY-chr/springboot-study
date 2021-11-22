package com.chr.fservice.upload.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author RAY
 * @descriptions Buffer clear()    把position设为0，把limit设为capacity，一般在把数据写入Buffer前调用。
 * Buffer flip() 　  把limit设为当前position，把position设为0，一般在从Buffer读出数据前调用。
 * @since 2020/3/22
 */
public class FileClient {

    private SocketChannel socketChannel;
    private static final String remote = "/home/ftptest/1.exe";
    private static final String source = "C:\\RAY\\software\\VMware-workstation-full-15.5.1-15018445.exe";
    private static final String targetPath = "C:\\Users\\RAY\\Desktop\\";
    private static final String linuxPath = "/home/xm/";
    private static final String localAddr = "127.0.0.1";
    private static final String linuxAddr = "192.168.108.8";

    public static void main(String[] args) throws Exception {
        //FileClient.upload(source, linuxPath + source.substring(source.lastIndexOf("\\") + 1));
        FileClient.download(remote, targetPath + remote.substring(remote.lastIndexOf("/") + 1));
    }

    public FileClient() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.socket().connect(new InetSocketAddress(linuxAddr, 6667));
    }

    public static void upload(String source, String remote) throws Exception {
        new FileClient().upload0(source, remote);
    }

    public static void download(String remote, String target) throws IOException {
        new FileClient().download0(remote, target);
    }

    /**
     * @param source 源路径
     * @param remote 服务器路径
     * @throws Exception Exception
     */
    private void upload0(String source, String remote) throws Exception {
        ByteBuffer buffer = writePreInfo(remote, 1);
        socketChannel.read(buffer);
        if (fail(buffer)) {
            socketChannel.close();
            return;
        }
        buffer.clear();
        File file = new File(source);
        FileChannel channel = new FileInputStream(file).getChannel();
        System.err.println(source + " 上传中...");
        long start = System.currentTimeMillis();
        int write = 0;
        FileProgress fileProgress = new FileProgress(file.length());
        new Thread(fileProgress).start();
        while (channel.read(buffer) != -1) {
            buffer.flip();
            /**
             * socketChannel 处于阻塞模式，所以不需要循环写，它会写完数据
             */
            write += socketChannel.write(buffer);
            fileProgress.setCurrentWrite(write);
            buffer.clear();
        }
        long end = System.currentTimeMillis();
        channel.close();
        /**
         * 客户端准备读需要关闭输出
         */
        socketChannel.shutdownOutput();
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        if (buffer.getInt() == 1) {
            System.err.println("\n" + source + " ==> 上传成功! 一共[" + write + "]字节 " +
                    "花费" + (end - start) + "毫秒");
        } else {
            System.err.println("上传失败!");
        }
        socketChannel.close();
    }

    /**
     * @param remote 服务器路径
     * @param target 目标路径
     * @throws IOException IOException
     */
    private void download0(String remote, String target) throws IOException {
        ByteBuffer buffer = writePreInfo(remote, 0);
        socketChannel.read(buffer);
        if (fail(buffer)) {
            socketChannel.close();
            return;
        }
        long remoteLength = buffer.getLong();
        System.err.println("服务器返回文件 [" + remote + "] 大小 ==> " + remoteLength + "字节");
        FileChannel outChannel = new FileOutputStream(target).getChannel();
        int write = 0;
        System.err.println(remote + " 下载中...");
        /**
         * 服务器返回成功标志时，继续写数据，所以buffer里面可能还有最开始的数据
         */
        long start = System.currentTimeMillis();
        write += outChannel.write(buffer);
        FileProgress fileProgress = new FileProgress(remoteLength);
        fileProgress.setCurrentWrite(write);
        new Thread(fileProgress).start();
        buffer.clear();
        while (socketChannel.read(buffer) != -1) {
            buffer.flip();
            write += outChannel.write(buffer);
            fileProgress.setCurrentWrite(write);
            buffer.clear();
        }
        long end = System.currentTimeMillis();
        outChannel.close();
        System.err.println("\n" + remote + " ==> 下载成功!" + " 一共 ==> " + write + "字节 " +
                "花费" + (end - start) + "毫秒");
        socketChannel.close();
    }

    /**
     * 客户端上传为1，下载为0
     *
     * @param remote   服务器路径
     * @param downOrUp 下载or上传
     * @throws IOException IOException
     */
    private ByteBuffer writePreInfo(String remote, int downOrUp) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        byte[] bytes = remote.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        buffer.putInt(length);
        buffer.put((byte) downOrUp);
        buffer.put(bytes);
        buffer.flip();
        socketChannel.write(buffer);
        buffer.clear();
        return buffer;
    }

    private boolean fail(ByteBuffer buffer) {
        buffer.flip();
        int info = buffer.getInt();
        byte b = buffer.get();
        if (b == 0) {
            byte[] msg = new byte[info];
            buffer.get(msg);
            System.err.println(new String(msg, StandardCharsets.UTF_8));
            return true;
        }
        return false;
    }

}
