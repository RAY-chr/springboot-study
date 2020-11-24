package com.chr.fweb.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author RAY
 * @descriptions
 * @since 2020/5/3
 */
@Controller
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

    @RequestMapping("/book")
    @ResponseBody
    @Transactional
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
    public String Toupload() {
        return "upload";
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
    public void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        byte[] buffer = new byte[2048];
        String path = "C:\\Users\\RAY\\Desktop\\常用\\派拉\\周报\\9.07-9.11周报_陈红任.docx";
        // 转换为utf-8编码
        path = URLDecoder.decode(path, "UTF-8");
        File file = new File(path);
        FileInputStream in = new FileInputStream(file);
        response.setHeader("Content-Disposition", "attachment;fileName=" + file.getName());
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        OutputStream out = response.getOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @RequestMapping("uploadTask")
    @ResponseBody
    public String uploadTask(@RequestParam("source") String source) throws IOException {
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

    @RequestMapping("ftpList")
    @ResponseBody
    public List<String> ftpList() throws IOException {
        FTPClient ftpClient = ftpUtil.getFtpClient();
        List<String> files = ftpUtil.getAbsolutePathFiles("/", null);
        //System.out.println(ftpUtil.mkdirs("/test/2021/21", ftpClient));
        return files;
    }

}
