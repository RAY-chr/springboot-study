package com.chr.fservice.upload.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author RAY
 * @descriptions
 * @since 2021/6/30
 */
@Component
public class HdfsUtil {

    @Value("${hdfs.uri}")
    private String uri;

    @Value("${hdfs.user}")
    private String user;

    private FileSystem fs;

    //@PostConstruct
    public void initFs() throws Exception {
        fs = getFileSystem();
    }

    /**
     * 使用FileSystem fileSystem = FileSystem.get(uri, conf, "hadoopuser");
     * 这种方式获取确实会出现无法利用缓存导致OOM，使用下面的写法可以利用：
     * System.setProperty("HADOOP_USER_NAME","hadoopuser");
     * FileSystem fileSystem = FileSystem.get(uri, conf);
     * 在多线程使用时，最好不要关闭 FileSystem，因为带缓存获取的都是同一个 FileSystem
     *
     * @return
     * @throws Exception
     */
    public FileSystem getFileSystem() throws Exception {
        Configuration configuration = new Configuration();
        System.setProperty("HADOOP_USER_NAME", user);
        return FileSystem.get(new URI(uri), configuration);
    }

    public OutputStream getOutputStream(String path, boolean overwrite) {
        try {
            return fs.create(new Path(path), overwrite);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
