package com.chr.fweb.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chr.fservice.config.DataSourceContext;
import com.chr.fservice.entity.Book;
import com.chr.fservice.entity.Picture;
import com.chr.fservice.entity.SimpleBook;
import com.chr.fservice.entity.Task;
import com.chr.fservice.mapper.BookMapper;
import com.chr.fservice.mapper.PictureMapper;
import com.chr.fservice.service.IBookService;
import com.chr.fservice.service.ITaskService;
import com.chr.fservice.service.PictureConfig;
import com.chr.fservice.service.async.AsyncTestService;
import com.chr.fservice.upload.TaskDetailContainer;
import com.chr.fservice.upload.UploadTask;
import com.chr.fservice.upload.ftp.FTPUtil;
import com.chr.fweb.config.IpLimit;
import com.chr.fweb.config.ThreadLimit;
import netty.dao.entity.Renter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author RAY
 * @descriptions
 * @since 2020/5/3
 */
@Controller
@Validated
public class BookController {

    @Value("${linuxPath}")
    private String path;
    @Value("${linuxUrl}")
    private String url;

    @Autowired
    private PictureConfig pictureConfig;

    @Autowired
    private IBookService iBookService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AsyncTestService asyncTestService;

    @Autowired
    private PictureMapper pictureMapper;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private FTPUtil ftpUtil;

    @Autowired
    private RenterDao renterDao;

    @RequestMapping("/book")
    @ResponseBody
    @Transactional
    @ThreadLimit(size = 1)
    public List<Book> list() {
        List<Book> books = iBookService.list();
        Book book = iBookService.getOne(new QueryWrapper<Book>()
                .eq("book_id", 2));
        List<Book> test = bookMapper.test();
        List<SimpleBook> testView = bookMapper.testView();
        testView.forEach(System.out::println);
        //iBookService.save(new Book(3,"1003","物理","1"));
        /**
         * lamda表达式写法
         */
        Book book2 = iBookService.getOne(new QueryWrapper<Book>().lambda()
                .eq(Book::getBookId, 1));
        //System.out.println(book);
        System.out.println("当前线程id为：" + Thread.currentThread().getId());
        return books;
    }

    @RequestMapping("/Toupload")
    @IpLimit(/*times = 1, expire = 7200L*/)
    public String Toupload() {
        try {
            Book book = new Book();
            book.setBookId(1);
            DataSourceContext.setDataSource("slave");
            Book book_1 = bookMapper.getById("book_1", book);
            DataSourceContext.clearCache();
            Renter renter = renterDao.selectById(1);
            System.out.println(renter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "upload";
    }

    @RequestMapping("/saveByState")
    @ResponseBody
    public String saveByState(Integer id, String state) {
        Book book = new Book();
        book.setBookId(id);
        book.setBookNo("1001");
        book.setBookName("11");
        book.setBookState(state);
        iBookService.save(book);
        return "success";
    }


    @RequestMapping("/upload")
    public void upload(MultipartFile pictureFile) throws Exception {
        if (null == pictureFile || pictureFile.isEmpty()) {
            return;
        }
        String oldName = pictureFile.getOriginalFilename();
        File file = new File(path + System.currentTimeMillis() + oldName);
        /*String s = iBookService.transfer(new UploadFile(pictureFile, file));*/
        InputStream inputStream = pictureFile.getInputStream();
        String s = iBookService.uploadTask(inputStream, oldName,
                file.getAbsolutePath(), pictureFile.getSize());
        return;
    }


    @RequestMapping("/test")
    @ResponseBody
    @ThreadLimit
    public String test() {
        String s = iBookService.transfer(() -> {
            int i = 0;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println(" " + s);
        return s;
    }

    @RequestMapping("/testAsync")
    @ResponseBody
    public String testAsync() {
        Future<String> firstJob = asyncTestService.firstJob();
        Future<String> secondJob = asyncTestService.secondJob();
        while (!firstJob.isDone() && !secondJob.isDone()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return "job execute success";
    }

    @RequestMapping("/picture")
    public String picture(Model model) {
        List<Picture> pictures = pictureMapper.selectList(null);
        model.addAttribute("pictures", pictures);
        return "image/showPicture";
    }

    @RequestMapping("/updatePicture")
    public String update(MultipartFile pictureFile, Integer id) throws Exception {
        if (null == pictureFile || pictureFile.isEmpty()) {
            return "fail";
        }
        String oldName = pictureFile.getOriginalFilename();
        String currentTime = String.valueOf(System.currentTimeMillis());
        File file = new File(path + currentTime + oldName);
        pictureFile.transferTo(file);
        pictureMapper.updateById(new Picture(id, url + currentTime + oldName));
        return "redirect:/picture";
    }

    @RequestMapping("/download")
    public void download(String remote, HttpServletResponse response) throws Exception {
        byte[] buffer = new byte[2048];
        /*String path = "C:\\Users\\RAY\\Desktop\\常用\\派拉\\周报\\9.07-9.11周报_陈红任.docx";
        // 转换为utf-8编码
        path = URLDecoder.decode(path, "UTF-8");
        File file = new File(path);
        InputStream in = new FileInputStream(file);*/
        String name = remote.substring(remote.lastIndexOf("/") + 1);
        FTPClient ftpClient = ftpUtil.getFtpClient();
        InputStream in = ftpClient.retrieveFileStream(remote);
        response.setHeader("Content-Disposition", "attachment;fileName=" + name);
        response.setHeader("fileName", name);
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        OutputStream out = response.getOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
        ftpClient.disconnect();
    }

    @RequestMapping("uploadTask")
    @ResponseBody
    public String uploadTask(@Valid @Length(max = 12) @RequestParam("source") String source) throws IOException {
        String s = iBookService.uploadTask(source, path);
        return s;
    }

    @RequestMapping("countPercent")
    @ResponseBody
    public List<UploadTask> countPercent() {
        List<UploadTask> result = TaskDetailContainer.get();
        return result;
    }

    @RequestMapping("cancel")
    @ResponseBody
    public String cancel(@RequestParam("source") String source) {
        TaskDetailContainer.cancel(source);
        return "success";
    }

    @RequestMapping("pause")
    @ResponseBody
    public String pause(@RequestParam("source") String source) {
        TaskDetailContainer.pause(source);
        return "success";
    }

    @RequestMapping("resume")
    @ResponseBody
    public String resume(@RequestParam("source") String source) {
        TaskDetailContainer.resume(source);
        return "success";
    }

    @RequestMapping("donePercent")
    @ResponseBody
    public List<Task> donePercent() {
        List<Task> tasks = taskService.list();
        return tasks;
    }

    @RequestMapping("ftptest")
    @ResponseBody
    public String ftptest() throws IOException {
        String source = "C:\\Users\\RAY\\Desktop\\test\\1.exe";
        //InputStream inputStream = new FileInputStream(source);
        String s = iBookService.uploadTask(source, "/ff/xx/rr/1.exe", true);
        return s;
    }

    @RequestMapping("/ftpList/**")
    @ResponseBody
    public FTPFile[] ftpList(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String s = uri.substring("ftpList".length() + 1);
        FTPClient ftpClient = ftpUtil.getFtpClient();
        FTPFile[] ftpFiles = ftpClient.listFiles(s);
        List<String> files = ftpUtil.getAbsolutePathFiles(s, ftpClient);
        ftpClient.disconnect();
        //System.out.println(ftpUtil.mkdirs("/test/2021/21", ftpClient));
        return ftpFiles;
    }

}
