package com.chr.fservice.upload.nio;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author RAY
 * @descriptions
 * @since 2020/3/22
 */
public class FileServer {
    private static final String remoteHost = "192.168.108.8";
    private static final String localHost = "127.0.0.1";
    private static final int bufferSize = 8192;
    private static final int PORT = 6667;
    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(10);
    private static final LinkedBlockingQueue<Runnable> READS = new LinkedBlockingQueue<>();
    private ServerSocketChannel serverSocketChannel;
    private Selector mainSelector;
    private Selector subSelector;

    public static void main(String[] args) throws IOException {
        //new FileServer().listen();
        doListen();
    }

    public FileServer() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(localHost, PORT));
            System.err.println("[FileServer] 服务器启动成功!");
            mainSelector = Selector.open();
            subSelector = Selector.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void doListen() {
        FileServer server = new FileServer();
        new Thread(() -> {
            try {
                server.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "mainSelector").start();
        new Thread(() -> {
            try {
                server.handle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "subSelector").start();
    }

    /**
     * mainSelector 只负责接收请求，然后把socketChannel注册到subSelector
     *
     * @throws IOException IOException
     */
    public void accept() throws IOException {
        serverSocketChannel.register(mainSelector, SelectionKey.OP_ACCEPT);
        while (true) {
            int select = mainSelector.select();
            if (select > 0) {
                Iterator<SelectionKey> keyIterator = mainSelector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    //此操作必不可少
                    keyIterator.remove();
                    System.out.println(Thread.currentThread().getName() + " ==> " + key.interestOps());
                    if (key.isAcceptable()) {
                        try {
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            if (socketChannel != null) {
                                socketChannel.configureBlocking(false);
                                /**
                                 * @1 socketChannel.register(subSelector, SelectionKey.OP_READ)
                                 * @2 int select = subSelector.select();
                                 * 如果 @2 先执行，且没有就绪事件时，会阻塞住，导致 @1 也会阻塞 在之前调用subSelector.wakeup(),
                                 * 不大有用，用队列的方式先把任务添加至队列，然后 wakeup() 唤醒 select()，接着执行队列的任务
                                 * 这种就能避免注册时阻塞了
                                 */
                                //subSelector.wakeup();
                                //socketChannel.register(subSelector, SelectionKey.OP_READ);
                                READS.add(() -> {
                                    try {
                                        socketChannel.register(subSelector, SelectionKey.OP_READ);
                                    } catch (ClosedChannelException ignored) {
                                    }
                                });
                                subSelector.wakeup();
                            }
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    /**
     * 负责读写的处理
     *
     * @throws IOException IOException
     */
    public void handle() throws IOException {
        while (true) {
            int select = subSelector.select();
            while (READS.peek() != null) {
                Runnable poll = READS.poll();
                poll.run();
            }
            if (select > 0) {
                Iterator<SelectionKey> keyIterator = subSelector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    //此操作必不可少
                    keyIterator.remove();
                    System.out.println(Thread.currentThread().getName() + " ==> " + key.interestOps());
                    if (key.isReadable()) {
                        // 移除掉这个key的可读事件，已经在线程池里面处理，因为文件传输会持续读写数据(使用线程池处理的必要操作)
                        key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                        SERVICE.execute(() -> read(key));
                        //read(key);
                    }
                }
            }
        }
    }

    /**
     * 服务器的监听器
     */
    public void listen() throws IOException {
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            int select = selector.select();
            if (select > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    //此操作必不可少
                    keyIterator.remove();
                    System.out.println(Thread.currentThread().getName() + " ==> " + key.interestOps());
                    if (key.isAcceptable()) {
                        try {
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            if (socketChannel != null) {
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);
                                selector.wakeup();
                            }
                        } catch (IOException ignored) {
                        }
                    }
                    if (key.isReadable()) {
                        // 移除掉这个key的可读事件，已经在线程池里面处理(使用线程池处理的必要操作)
                        key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                        SERVICE.execute(() -> read(key));
                        //read(key);
                    }

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
        byte b;
        byte[] msg;
        try {
            pathLength = buffer.getInt();
            b = buffer.get();
            msg = new byte[pathLength];
            buffer.get(msg);
        } catch (BufferUnderflowException e) {
            String s = "The format of msg is not correct";
            System.err.println(s);
            buffer.clear();
            buffer.put(s.getBytes());
            buffer.flip();
            try {
                socketChannel.write(buffer);
            } catch (IOException ignored) {
            }
            close(null, socketChannel, key);
            return;
        }

        String targetPath = new String(msg, StandardCharsets.UTF_8);
        FileChannel fileChannel = null;
        try {
            if (b == 1) { // 客户端上传文件
                print("客户端上传的文件路径为 ==> " + targetPath);
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
                print(targetPath + " ==> 上传完成! 一共[" + write + "]字节 " +
                        "花费" + (end - start) + "毫秒\n");
                buffer.putInt(1);
                buffer.flip();
                socketChannel.write(buffer);
            }
            if (b == 0) {  //客户端下载文件
                print("客户端下载的文件路径为 ==> " + targetPath);
                File file;
                try {
                    file = new File(targetPath);
                    fileChannel = new FileInputStream(targetPath).getChannel();
                } catch (FileNotFoundException e) {
                    writeError("File Not Found", buffer, socketChannel);
                    close(null, socketChannel, key);
                    return;
                }
                buffer.clear();
                buffer.putInt(1);
                buffer.put((byte) 1);
                // 向客户端声明源文件多大
                buffer.putLong(file.length());
                buffer.flip();
                socketChannel.write(buffer);
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
                     * 非阻塞模式下，write()方法在尚未写出任何内容时可能就返回了。所以需要在循环中调用write()
                     */
                    while (buffer.hasRemaining()) {
                        write += socketChannel.write(buffer);
                    }
                    buffer.clear();
                }
                print("服务器一共从[" + targetPath + "]读出 ==> " + totalRead + "字节");
                print("服务器一共从[" + targetPath + "]写出 ==> " + write + "字节\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 调用wakeup，使得选择器上的第一个还没有返回的选择操作立即返回即重新select(使用线程池处理的非必要操作)
            //key.selector().wakeup();
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

    private void print(String msg) {
        System.err.println(Thread.currentThread().getName() + " -> " + msg);
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
        byte[] info = msg.getBytes(StandardCharsets.UTF_8);
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
