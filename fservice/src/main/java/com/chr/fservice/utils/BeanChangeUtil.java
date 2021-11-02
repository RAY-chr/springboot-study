package com.chr.fservice.utils;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RAY
 * @descriptions 比较同属一个类的两个对象
 * @since 2020/10/31
 */
public class BeanChangeUtil {
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    /**
     * 判断两个对象属性值是否变化
     *
     * @param first
     * @param another
     * @return
     * @throws Exception
     */
    public static boolean isChanged(Object first, Object another) throws Exception {
        return changedList(first, another).size() >= 1;
    }

    /**
     * 得到两个对象的属性值变化的List
     *
     * @param first
     * @param another
     * @return
     * @throws Exception
     */
    public static List<String> changedList(Object first, Object another) throws Exception {
        if (first.getClass() != another.getClass()) {
            throw new IllegalArgumentException("the Class type must be the same");
        }
        List<String> list = new ArrayList<>();
        Class<?> aClass = first.getClass();
        Method[] methods = aClass.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)) {
                Object o = method.invoke(first);
                Object o1 = method.invoke(another);
                if (!String.valueOf(o).equals(String.valueOf(o1))) {
                    list.add(name + " : " + o + " -> " + o1);
                }
            }
        }
        return list;
    }

    public static <T> T getNewBean(T source, T target) {
        BeanUtils.copyProperties(source, target);
        return target;
    }

}
