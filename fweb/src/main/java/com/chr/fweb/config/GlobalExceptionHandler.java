package com.chr.fweb.config;

import net.sf.json.JSONObject;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author RAY
 * @descriptions
 * @since 2021/4/7
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object exceptionHandler(HttpServletRequest request, Exception e) {
        e.printStackTrace();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 400);
        jsonObject.put("msg", getCause(e).getMessage());
        return jsonObject.toString();
    }

    /**
     * 拿到最根源的异常信息
     * @param e 传入的异常
     * @return 根源异常
     */
    private Throwable getCause(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

}
