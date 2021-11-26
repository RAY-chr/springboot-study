package com.chr.fservice.upload.nio;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * @author RAY
 * @descriptions Buffer clear()    把position设为0，把limit设为capacity，一般在把数据写入Buffer前调用。
 * Buffer flip() 　  把limit设为当前position，把position设为0，一般在从Buffer读出数据前调用。
 * @since 2020/3/22
 */
public class FileClient {

    private static final String REMOTE = "/home/ftptest/1.exe";
    private static final String SOURCE = "C:\\RAY\\software\\VMware-workstation-full-15.5.1-15018445.exe";
    private static final String SOURCE2 = "C:\\Users\\RAY\\Desktop\\test\\test.txt";
    private static final String TARGET_PATH = "C:\\Users\\RAY\\Desktop\\";
    private static final String TARGET_PATH2 = "C:\\Users\\RAY\\Desktop\\test\\";
    private static final String LINUX_PATH = "/home/xm/";
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String REMOTE_HOST = "192.168.108.8";
    private SocketChannel socketChannel;

    public static void main(String[] args) throws Exception {
        multiThread();
        //FileClient.download(source, targetPath + source.substring(source.lastIndexOf("\\") + 1));
        //FileClient.upload(source, targetPath + source.substring(source.lastIndexOf("\\") + 1));
    }

    public static void multiThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        new Thread(() -> {
            try {
                FileClient.upload(SOURCE, LINUX_PATH + SOURCE.substring(SOURCE.lastIndexOf("\\") + 1),
                        true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }).start();
        FileClient.download(REMOTE, TARGET_PATH + REMOTE.substring(REMOTE.lastIndexOf("/") + 1),
                true);
        latch.await();
        System.out.println((System.currentTimeMillis() - start));
    }

    public FileClient() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(REMOTE_HOST, 6667));
    }

    /**
     * @param source 源路径
     * @param remote 服务器路径
     * @throws IOException IOException
     */
    public static void upload(String source, String remote) throws IOException {
        upload(source, remote, false);
    }

    /**
     * @param source 源路径
     * @param remote 服务器路径
     * @param append 上传文件是否续传
     * @throws IOException IOException
     */
    public static void upload(String source, String remote, boolean append) throws IOException {
        new FileClient().upload0(source, remote, append);
    }

    /**
     * @param remote 服务器路径
     * @param target 服务器路径
     * @throws IOException IOException
     */
    public static void download(String remote, String target) throws IOException {
        download(remote, target, false);
    }

    /**
     * @param remote 服务器路径
     * @param target 服务器路径
     * @param append 下载文件是否续传
     * @throws IOException IOException
     */
    public static void download(String remote, String target, boolean append) throws IOException {
        new FileClient().download0(remote, target, append);
    }

    private void upload0(String source, String remote, boolean append) throws IOException {
        ByteBuffer buffer;
        if (append) {
            buffer = writePreInfo(remote, 1, 1, 0);
        } else {
            buffer = writePreInfo(remote, 1, 0, 0);
        }
        socketChannel.read(buffer);
        if (fail(buffer)) {
            socketChannel.close();
            return;
        }
        long existFileLength = 0;
        byte isAppend = buffer.get();
        if (isAppend == 1) {
            existFileLength = buffer.getLong();
        }
        buffer.clear();
        File file = new File(source);
        FileChannel channel;
        RandomAccessFile randomAccessFile = null;
        if (isAppend == 1) {
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(existFileLength);
            channel = randomAccessFile.getChannel();
        } else {
            channel = new FileInputStream(file).getChannel();
        }
        print(source + " 上传中...");
        long start = System.currentTimeMillis();
        long write = 0;
        if (isAppend == 1) {
            write = existFileLength;
        }
        long pre = System.currentTimeMillis();
        long total = file.length();
        System.err.print(Thread.currentThread().getName() + " -> " + "进度");
        while (channel.read(buffer) != -1) {
            buffer.flip();
            /**
             * socketChannel 处于阻塞模式，所以不需要循环写，它会写完数据
             */
            write += socketChannel.write(buffer);
            long next = System.currentTimeMillis();
            if ((next - pre) >= 500) {
                System.err.print(" ==> " + percent(write, total));
                pre = next;
            }
            buffer.clear();
        }
        System.err.print("\n");
        long end = System.currentTimeMillis();
        channel.close();
        if (isAppend == 1) {
            randomAccessFile.close();
        }
        /**
         * 客户端准备读需要关闭输出
         */
        socketChannel.shutdownOutput();
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        if (buffer.getInt() == 1) {
            print(source + " ==> 上传成功! 一共[" + write + "]字节 " +
                    "花费" + (end - start) + "毫秒");
        } else {
            print("上传失败!");
        }
        socketChannel.close();
    }

    private void download0(String remote, String target, boolean append) throws IOException {
        ByteBuffer buffer;
        long existDownloadLength = 0;
        if (append) {
            File file = new File(target);
            if (file.exists()) {
                existDownloadLength = file.length();
            }
            buffer = writePreInfo(remote, 0, 1, existDownloadLength);
        } else {
            buffer = writePreInfo(remote, 0, 0, 0);
        }
        socketChannel.read(buffer);
        if (fail(buffer)) {
            socketChannel.close();
            return;
        }
        long remoteLength = buffer.getLong();
        print("服务器返回文件 [" + remote + "] 大小 ==> " + remoteLength + "字节");
        FileChannel outChannel = new FileOutputStream(target, append).getChannel();
        long write = 0;
        if (append) {
            write = existDownloadLength;
        }
        print(remote + " 下载中...");
        /**
         * 服务器返回成功标志时，继续写数据，所以buffer里面可能还有最开始的数据
         */
        long start = System.currentTimeMillis();
        long pre = System.currentTimeMillis();
        System.err.print(Thread.currentThread().getName() + " -> " + "进度");
        write += outChannel.write(buffer);
        long next = System.currentTimeMillis();
        if ((next - pre) >= 500) {
            System.err.print(" ==> " + percent(write, remoteLength));
            pre = next;
        }
        buffer.clear();
        while (socketChannel.read(buffer) != -1) {
            buffer.flip();
            write += outChannel.write(buffer);
            next = System.currentTimeMillis();
            if ((next - pre) >= 500) {
                System.err.print(" ==> " + percent(write, remoteLength));
                pre = next;
            }
            buffer.clear();
        }
        System.err.print("\n");
        long end = System.currentTimeMillis();
        outChannel.close();
        print(remote + " ==> 下载成功!" + " 一共 ==> " + write + "字节 " +
                "花费" + (end - start) + "毫秒");
        socketChannel.close();
    }

    /**
     * 客户端上传为1，下载为0
     *
     * @param remote   服务器路径
     * @param downOrUp 下载or上传
     * @param isAppend 客户端告诉服务器是否是文件续传
     * @throws IOException IOException
     */
    private ByteBuffer writePreInfo(String remote, int downOrUp,
                                    int isAppend, long existDownloadLength) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        byte[] bytes = remote.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        buffer.putInt(length);
        buffer.put((byte) downOrUp);
        buffer.put(bytes);
        buffer.put((byte) isAppend);
        if (downOrUp == 0 && isAppend == 1) {
            buffer.putLong(existDownloadLength);
        }
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
            print(new String(msg, StandardCharsets.UTF_8));
            return true;
        }
        return false;
    }

    private void print(String msg) {
        System.err.println(Thread.currentThread().getName() + " -> " + msg);
    }

    private String percent(Long write, Long total) {
        return String.format("%.2f", ((write.doubleValue() / total.doubleValue()) * 100)) + "%";
    }

}
