package com.chr.fservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chr.fservice.entity.Book;
import com.chr.fservice.entity.SimpleBook;
import com.chr.fservice.quartz.JobContent;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 书籍表 Mapper 接口
 * </p>
 *
 * @author RAY
 * @since 2020-05-03
 */
@Repository
public interface BookMapper extends BaseMapper<Book> {

    List<Book> test();

    List<SimpleBook> testView();

    /**
     * 对于表名，字段，orderBy等需要使用 ${}
     * @param table
     * @param book
     * @return
     */
    Book getById(@Param("table") String table, Book book);

    void saveByState(@Param("table") String table, Book book);

    List<JobContent> listAllJobs();

}
