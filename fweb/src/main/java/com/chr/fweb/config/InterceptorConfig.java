package com.chr.fweb.config;

import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RAY
 * @descriptions
 * @since 2020/8/7
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    private static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String urlPattern = "/image/**";
    private static final String filePath = "C:/Users/RAY/Pictures/Saved Pictures/";
    private static final String ipPrefix = "IPADDR";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TestInterceptor()).order(1).addPathPatterns("/**");
        registry.addInterceptor(new IpLimitInterceptor()).order(2).addPathPatterns("/**");
        registry.addInterceptor(new ThreadLimitInterceptor()).order(3).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPattern).addResourceLocations("file:///" + filePath);
    }

    public boolean notInsHandlerMethod(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            System.out.println("handler of the request method not instanceof handlerMethod");
            System.out.println(handler);
            return true;
        }
        return false;
    }

    /**
     * 返回失败的信息
     *
     * @param response
     * @throws IOException
     */
    private void writeAuthFail(HttpServletResponse response, String msg) throws IOException {
        response.setContentType(APPLICATION_JSON);
        JSONObject object = new JSONObject();
        object.put("code", 400);
        object.put("msg", msg);
        PrintWriter writer = response.getWriter();
        writer.write(object.toString());
        writer.flush();
        writer.close();
    }

    /**
     * 得到请求的ip地址
     *
     * @param request
     * @return
     */
    public String getRemoteIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            //对于通过多个代理的情况，最后IP为客户端真实IP,多个IP按照','分割
            int position = ip.indexOf(",");
            if (position > 0) {
                ip = ip.substring(0, position);
            }
        }
        return ip;
    }

    private class TestInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (notInsHandlerMethod(handler)) return true;
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            Verify annotation = method.getAnnotation(Verify.class);
            //System.out.println(method.isAnnotationPresent(Verify.class));
            if (annotation != null) {
                System.out.println("请求url：" + request.getRequestURL());
                System.out.println("请求url对应的方法：" + method);
                System.out.println("请求url真实ip：" + getRemoteIP(request));
                String header = request.getHeader(AUTH_TOKEN);
                if (StringUtils.isEmpty(header) || !redisTemplate.hasKey(AUTH_TOKEN + ":" + header)) {
                    writeAuthFail(response, "Authentication Fail");
                    return false;
                }
                return true;
            }
            return true;
        }
    }


    private class IpLimitInterceptor implements HandlerInterceptor {
        private final Map<String, Object> lock = new HashMap<>();

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (notInsHandlerMethod(handler)) return true;
            return ipLimit(request, response, ((HandlerMethod) handler).getMethod());
        }

        protected boolean ipLimit(HttpServletRequest request, HttpServletResponse response, Method method) {
            IpLimit ipLimit = method.getAnnotation(IpLimit.class);
            if (ipLimit != null) {
                String remoteIP = getRemoteIP(request);
                int times = ipLimit.times();
                long expire = ipLimit.expire();
                String key = ipPrefix + ":" + (remoteIP = StringUtils.isEmpty(remoteIP) ?
                        "null" : remoteIP);
                try {
                    Boolean hasKey = redisTemplate.hasKey(key);
                    if (!hasKey) {
                        synchronized (this) {
                            if (!redisTemplate.hasKey(key)) {
                                lock.putIfAbsent(key, new Object());
                                redisTemplate.opsForValue().set(key, "0", expire, TimeUnit.SECONDS);
                            }
                        }
                    }
                    String s = redisTemplate.opsForValue().get(key);
                    int i = Integer.parseInt(s);
                    if (i >= times) {
                        writeAuthFail(response, "访问次数过多");
                        return false;
                    } else {
                        // 可以利用lua脚本
                        synchronized (lock.get(key)) {
                            if (Integer.parseInt(redisTemplate.opsForValue().get(key)) >= times) {
                                writeAuthFail(response, "访问次数过多");
                                return false;
                            }
                            redisTemplate.opsForValue().increment(key, 1L);
                        }
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        writeAuthFail(response, e.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    return false;
                }
            }
            return true;
        }
    }

    private class ThreadLimitInterceptor implements HandlerInterceptor {
        private final Map<Method, AtomicInteger> threadLimit = new HashMap<>();

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (notInsHandlerMethod(handler)) return true;
            return threadLimit(request, response, ((HandlerMethod) handler).getMethod());
        }

        public boolean threadLimit(HttpServletRequest request, HttpServletResponse response, Method method) {
            ThreadLimit annotation = method.getAnnotation(ThreadLimit.class);
            if (annotation == null) return true;
            if (!threadLimit.containsKey(method)) {
                synchronized (this) {
                    if (!threadLimit.containsKey(method)) {
                        threadLimit.put(method, new AtomicInteger(0));
                    }
                }
            }
            AtomicInteger size = threadLimit.get(method);
            int get = size.get();
            if (get >= annotation.size()) {
                try {
                    writeAuthFail(response, "thread too much");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
            while (true) {
                if (size.compareAndSet(size.get(), size.get() + 1)) {
                    return true;
                } else {
                    if (size.get() >= annotation.size()) {
                        try {
                            writeAuthFail(response, "thread too much");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                }
            }
            //size.incrementAndGet();
            //return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) throws Exception {
            if (notInsHandlerMethod(handler)) return;
            Method method = ((HandlerMethod) handler).getMethod();
            ThreadLimit annotation = method.getAnnotation(ThreadLimit.class);
            if (annotation == null) return;
            AtomicInteger size = threadLimit.get(method);
            size.decrementAndGet();
            return;
        }
    }
}
