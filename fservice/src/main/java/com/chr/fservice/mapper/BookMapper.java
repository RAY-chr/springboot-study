package com.chr.fservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chr.fservice.entity.Book;
import com.chr.fservice.entity.SimpleBook;

import java.util.List;

/**
 * <p>
 * 书籍表 Mapper 接口
 * </p>
 *
 * @author RAY
 * @since 2020-05-03
 */
public interface BookMapper extends BaseMapper<Book> {

    List<Book> test();

    List<SimpleBook> testView();
}
