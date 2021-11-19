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
    private static final String source = "C:\\RAY\\software\\VMware-workstation-full-15.5.1-15018445.exe";
    private static final String remote = "C:\\Users\\RAY\\Desktop\\test\\test.txt";
    private static final String targetPath = "C:\\Users\\RAY\\Desktop\\";
    private static final String linuxPath = "/home/xm/";
    private static final String localAddr = "127.0.0.1";
    private static final String linuxAddr = "192.168.108.8";

    public static void main(String[] args) throws Exception {
        FileClient client = new FileClient();
        client.upload(source, linuxPath + source.substring(source.lastIndexOf(File.separator) + 1));
        //client.download(source, targetPath + source.substring(source.lastIndexOf("/") + 1));
    }

    public FileClient() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.socket().connect(new InetSocketAddress(linuxAddr, 6667));
    }

    /**
     * @param source 源路径
     * @param remote 服务器路径
     * @throws Exception Exception
     */
    public void upload(String source, String remote) throws Exception {
        ByteBuffer buffer = writePreInfo(remote, 1);
        socketChannel.read(buffer);
        if (fail(buffer)) {
            return;
        }
        buffer.clear();
        File file = new File(source);
        FileChannel channel = new FileInputStream(file).getChannel();
        while (channel.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
            buffer.clear();
        }
        channel.close();
        /**
         * 客户端准备读需要关闭输出
         */
        socketChannel.shutdownOutput();
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        if (buffer.getInt() == 1) {
            System.err.println("上传成功!");
        }
        socketChannel.close();
    }

    /**
     * @param remote 服务器路径
     * @param target 目标路径
     * @throws IOException IOException
     */
    public void download(String remote, String target) throws IOException {
        ByteBuffer buffer = writePreInfo(remote, 0);
        socketChannel.read(buffer);
        if (fail(buffer)) {
            return;
        }
        FileChannel outChannel = new FileOutputStream(target).getChannel();
        int write = 0;
        write += outChannel.write(buffer);
        buffer.clear();
        while (socketChannel.read(buffer) != -1) {
            buffer.flip();
            write += outChannel.write(buffer);
            buffer.clear();
        }
        outChannel.close();
        System.err.println(target + " ==> 下载成功!" + " 一共 ==> " + write + "字节");
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
