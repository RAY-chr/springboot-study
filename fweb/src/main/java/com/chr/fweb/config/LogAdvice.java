package com.chr.fweb.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author RAY
 * @descriptions
 * @since 2020/5/4
 */
//@Component
//@Aspect
public class LogAdvice{
    private Logger logger = LoggerFactory.getLogger(LogAdvice.class);

    /**
     * 定义一个公共切点，代码复用
     */
    @Pointcut(value = "execution(* com.chr.fweb.controller.*.*(..))")
    private void log() {}

    /**
     * 方法执行前切入
     */
    @Before(value = "log()")
    public void before(JoinPoint joinPoint) {
        ServletRequestAttributes sa = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sa.getRequest();
        logger.info("-----------------------START EXECUTE METHOD--------------------------");
        // 获取用户访问的url
        logger.info("url={}", request.getRequestURL());
        // 获取用户访问的方式，get/post
        logger.info("method={}", request.getMethod());
        // 获取的ip
        logger.info("ip={}", request.getRemoteAddr());
        // 获取用户访问的是哪个方法
        logger.info("class_method={}", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        // 获取方法的参数
        logger.info("args={}", joinPoint.getArgs());
    }

    @AfterReturning(returning = "ret", pointcut = "log()")
    public void doAfterReturning(Object ret) throws Throwable {
        // 处理完请求，返回内容
        logger.info("RESPONSE : " + ret);
        logger.info("------------------------END EXECUTE METHOD--------------------------");
    }

    @Around(value = "log()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object ob = pjp.proceed();// ob 为方法的返回值
        logger.info("SPEND TIME : " + (System.currentTimeMillis() - startTime)+"毫秒");
        return ob;
    }

}
