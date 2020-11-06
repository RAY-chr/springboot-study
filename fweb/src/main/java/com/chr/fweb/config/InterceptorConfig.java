package com.chr.fweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @author RAY
 * @descriptions
 * @since 2020/8/7
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    private static final String urlPattern = "/image/**";
    private static final String filePath = "C:/Users/RAY/Pictures/Saved Pictures/";
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TestInterceptor()).order(1).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPattern).addResourceLocations("file:///"+filePath);
    }

    private static class TestInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (!(handler instanceof HandlerMethod)) {
                System.out.println("handler of the request method not instanceof handlerMethod");
                System.out.println(handler);
                return true;
            }
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            Verify annotation = method.getAnnotation(Verify.class);
            //System.out.println(method.isAnnotationPresent(Verify.class));
            if (annotation != null) {
                System.out.println("请求url：" + request.getRequestURL());
                System.out.println("请求url对应的方法：" + method);
                return true;
            }
            return true;
        }
    }
}
