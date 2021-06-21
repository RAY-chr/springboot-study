package com.chr.fservice.upload.ftp;

import com.chr.fservice.service.FtpProperties;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author RAY
 * @descriptions
 * @since 2020/11/3
 */
@Component
public class FTPUtil {
    @Autowired
    private FtpProperties pro;

    public FTPClient getFtpClient() throws IOException {
        FTPClient ftpClient = new FTPClient();
        getFtpClient0(ftpClient);
        return ftpClient;
    }

    private void getFtpClient0(FTPClient ftpClient) throws IOException {
        ftpClient.connect(pro.getHost(), pro.getPort());
        ftpClient.login(pro.getUsername(), pro.getPassword());
        int replyCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            ftpClient.disconnect();
        }
        if (!ftpClient.isConnected()) {
            System.out.println("ftp connect fail");
        }
    }

    /**
     * 返回指定的文件大小  不要用同一个FTPClient先调用获取输入流的方法，在调用listFiles方法
     * 因为这样流没关闭，ftpClient.completePendingCommand()没执行，会一直等待
     *
     * @param remote 文件的路径
     * @return
     * @throws IOException
     */
    private synchronized long getSize(String remote) throws IOException {
        return getSize(remote, null);
    }

    public long getSize(String remote, FTPClient ftpClient) throws IOException {
        if (ftpClient == null) {
            ftpClient = getFtpClient();
        }
        FTPFile[] files = ftpClient.listFiles(remote);
        long fileSize = 0;
        if (files.length == 1 && files[0].isFile()) {
            fileSize = files[0].getSize();
        }
        return fileSize;
    }

    /**
     * 在每次执行完下载操作之后，completePendingCommand()会一直在等FTP Server返回226 Transfer complete，
     * 但是FTP Server只有在接受到InputStream 执行close方法时，才会返回。
     * 所以一定先要执行close方法。不然在第一次下载一个文件成功之后，之后再次获取inputStream 就会返回null。
     *
     * @throws IOException
     */
    private void completePendingCommand() throws IOException {
        getFtpClient().completePendingCommand();
    }

    /**
     * 得到ftp服务器上的路径下的全部文件
     *
     * @param remote 文件路径是目录的话后面必须加 /
     * @return
     * @throws IOException
     */
    public List<String> getAbsolutePathFiles(String remote, FTPClient ftpClient) throws IOException {
        List<String> list = new ArrayList<>();
        if (ftpClient == null) {
            ftpClient = getFtpClient();
        }
        this.getAbsolutePathFiles0(remote, list, ftpClient);
        return list;
    }

    /**
     * 调用FTPClient.enterLocalPassiveMode();这个方法的意思就是每次数据连接之前，
     * ftp client告诉ftp server开通一个端口来传输数据。为什么要这样做呢，
     * 因为ftp server可能每次开启不同的端口来传输数据，但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞
     * @param remote
     * @param list
     * @param ftpClient
     * @throws IOException
     */
    private void getAbsolutePathFiles0(String remote, List<String> list, FTPClient ftpClient) throws IOException {
        //ftpClient.enterLocalPassiveMode();
        FTPFile[] files = ftpClient.listFiles(remote);
        for (FTPFile file : files) {
            if (file.isFile()) {
                list.add(remote + file.getName());
            } else {
                getAbsolutePathFiles0(remote + file.getName() + "/", list, ftpClient);
            }
        }
    }

    /**
     * 于ftp服务器递归创建目录
     *
     * @param remote    须是完整地址 例如 /test/2020
     * @param ftpClient
     * @return
     * @throws IOException
     */
    public boolean mkdirs(String remote, FTPClient ftpClient) throws IOException {
        if (ftpClient == null) {
            ftpClient = getFtpClient();
        }
        if (!ftpClient.changeWorkingDirectory(remote)) {
            String[] dirs = remote.split("/");
            String tempPath = "";
            for (String dir : dirs) {
                if (dir == null || "".equals(dir)) continue;
                tempPath += "/" + dir;
                if (!ftpClient.changeWorkingDirectory(tempPath)) {
                    if (!ftpClient.makeDirectory(tempPath)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
