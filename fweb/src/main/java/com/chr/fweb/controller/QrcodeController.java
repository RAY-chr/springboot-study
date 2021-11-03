package com.chr.fweb.controller;

import com.chr.fservice.entity.Book;
import com.chr.fservice.pool.CommonStoragePool;
import com.chr.fservice.quartz.JobContent;
import com.chr.fservice.quartz.QuartzJobHandle;
import com.chr.fservice.service.IBookService;
import com.chr.fservice.service.kafka.MessagingService;
import com.chr.fweb.config.Verify;
import com.chr.fweb.netty.HttpServerHandler;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author RAY
 * @descriptions
 * @since 2020/7/1
 */
@Controller
public class QrcodeController {
    private Map<String, String> map = new HashMap<>();
    private ThreadLocal<String> local = new ThreadLocal<>();

    @Value("${qrcode_info}")
    private String qrcodeInfo;

    @Autowired
    private IBookService bookService;

    @Autowired
    private QuartzJobHandle quartzJobHandle;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private CommonStoragePool<String> pool;

    @Autowired
    private CommonStoragePool<Integer> intPool;

    @Bean(name = "commonPool")
    public CommonStoragePool<String> getPool() {
        return pool;
    }

    @Bean(name = "intPool")
    public CommonStoragePool<Integer> getIntPool() {
        return intPool;
    }

    @RequestMapping("/qrcode")
    public String qrcode(Model model, HttpSession session) {
        Object code = session.getAttribute("code");
        if (code == null) {
            String s = UUID.randomUUID().toString();
            session.setAttribute("code", s);
        }
        model.addAttribute("ownCode", String.valueOf(session.getAttribute("code")));
        return "qrcode";
    }

    @RequestMapping("/checkInfo")
    @ResponseBody
    public String checkInfo(HttpSession session, @RequestParam("code") String requestCode) {
        String localCode = String.valueOf(session.getAttribute("code"));
        if (localCode == null) {
            return "fail";
        } else {
            Objects.requireNonNull(requestCode);
            if (!localCode.equals(requestCode)) {
                return "fail";
            } else if (map.get(localCode) == null) {
                return "fail";
            }
        }
        return "success check,into the User View";
    }

    @RequestMapping("/imitate")
    @ResponseBody
    public String imitate(HttpSession session) {
        String code = String.valueOf(session.getAttribute("code"));
        map.put(code, "success");
        return "imitate";
    }

    @RequestMapping("/qrimage")
    @ResponseBody
    public ResponseEntity<byte[]> getImage() {
        //二维码内的信息
        String info = qrcodeInfo;
        byte[] qrcode = null;
        try {
            qrcode = this.getQRCodeImage(info, 360, 360);
        } catch (WriterException e) {
            System.out.println("Could not generate QR Code, WriterException :: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Could not generate QR Code, IOException :: " + e.getMessage());
        }
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<byte[]>(qrcode, headers, HttpStatus.CREATED);
    }

    /**
     * 生成二维码字节数组
     *
     * @param text   文本信息
     * @param width  宽度
     * @param height 高度
     * @return
     * @throws WriterException
     * @throws IOException
     */
    public byte[] getQRCodeImage(String text, int width, int height)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }

    @RequestMapping("/netty")
    @ResponseBody
    public String sendMessage() {
        HttpServerHandler.send("测试netty........");
        return "success";
    }

    /**
     * Springmvc对每次请求线程都不同于上次的线程，但是会复用一些线程
     * 比如对同一个方法请求线程id可以是37，38，41，45，37
     *
     * @return
     */
    @RequestMapping("/threadlocal1")
    @ResponseBody
    @Verify
    public String threadlocal1() {
        local.set("RAY");
        String s = "当前线程id：" + Thread.currentThread().getId() + " " + local.get();
        System.out.println(s);
        return s;
    }

    @RequestMapping("/threadlocal2")
    @ResponseBody
    public String threadlocal2() {
        return "当前线程id：" + Thread.currentThread().getId() + " " + local.get();
    }

    /**
     * 动态切换数据源
     *
     * @return
     */
    @RequestMapping("/routeSource")
    @ResponseBody
    public List<Book> route(@RequestParam("dataSource") String dataSource) {
        List<Book> list = bookService.getBookList(dataSource);
        return list;
    }

    /**
     * @return
     */
    @RequestMapping("/quartz")
    @ResponseBody
    public String quartz() throws Exception {
        quartzJobHandle.rescheduleJob("com.chr.fservice.quartz.job.HelloJob",
                "1", "0/5 * * * * ?");
        return "job update success";
    }

    /**
     * @return
     */
    @RequestMapping("/quartzPa")
    @ResponseBody
    public String quartzPa() throws Exception {
        quartzJobHandle.pauseJob("com.chr.fservice.quartz.job.HelloJob", "1");
        Book book = bookService.list().get(0);
        /**
         * Spring框架好用的属性取值设值和属性复制的工具
         */
        BeanWrapper wrapper = new BeanWrapperImpl(book);
        System.out.println(wrapper.getPropertyValue("bookName"));
        wrapper.setPropertyValue("bookName", "高数2");
        System.out.println(book);
        Book book1 = new Book();
        BeanUtils.copyProperties(book, book1);
        System.out.println(book1);
        return "job pause success";
    }

    @RequestMapping("/listAllJobs")
    @ResponseBody
    public List<JobContent> listAllJobs() {
        return quartzJobHandle.listAllJobs();
    }

    /**
     * @return
     */
    @RequestMapping("/triggerJob")
    @ResponseBody
    public String triggerJob() throws Exception {
        quartzJobHandle.triggerJob("com.chr.fservice.quartz.job.DataCompareJob", "1");
        return "job trigger success";
    }

    /**
     * @return
     */
    @RequestMapping("/quartzRe")
    @ResponseBody
    public String quartzRe() throws Exception {
        quartzJobHandle.resumeJob("com.chr.fservice.quartz.job.DataCompareJob", "1");
        return "job resume success";
    }

    /**
     * @return
     */
    @RequestMapping("/commonPool")
    @ResponseBody
    public String commonPool() throws Exception {
        for (int i = 0; i < 30; i++) {
            pool.add("'"+String.valueOf(i)+"'");
        }
        for (int i = 0; i < 10; i++) {
            intPool.add(i);
        }
        return "success";
    }

    @RequestMapping("/kafka")
    @ResponseBody
    public String kafka() throws Exception {
        messagingService.send("topic_registration","test");
        return "success";
    }


}
