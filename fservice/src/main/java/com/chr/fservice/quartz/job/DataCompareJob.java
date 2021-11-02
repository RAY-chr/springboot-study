package com.chr.fservice.quartz.job;

import com.chr.fservice.SpringUtil;
import com.chr.fservice.config.DataSourceContext;
import com.chr.fservice.entity.Book;
import com.chr.fservice.service.IBookService;
import com.chr.fservice.utils.BeanChangeUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RAY
 * @descriptions 注意在 Map中添加引用类型的时候，最好添加新的实体类
 * @since 2020/11/2
 */
public class DataCompareJob extends AbstractJob {
    private static Logger logger = LoggerFactory.getLogger(DataCompareJob.class);
    private static final Map<Integer, Book> memory_books = new HashMap<>(100);
    private static IBookService bookService = SpringUtil.getBean(IBookService.class);

    @Override
    public void doExecute(JobExecutionContext context) {
        compareData();
    }

    /**
     * 比较数据
     */
    public static void compareData() {
        long start = System.currentTimeMillis();
        check();
        // 初始化内存数据
        if (memory_books.size() == 0) {
            List<Book> initList = bookService.list();
            for (Book book : initList) {
                memory_books.put(book.getBookId(), BeanChangeUtil.getNewBean(book, new Book()));
            }
            logger.info("init memory_books over");
            DataSourceContext.setDataSource("slave");
            bookService.saveBatch(initList);
            DataSourceContext.clearCache();
            initList.clear();
        } else {
            List<Book> books = bookService.list();
            DataSourceContext.setDataSource("slave");
            // 新增或修改
            for (Book book : books) {
                Integer id = book.getBookId();
                if (!memory_books.containsKey(id)) {
                    memory_books.put(id, BeanChangeUtil.getNewBean(book, new Book()));
                    logger.info("memory_books add [{}] successfully", book);
                    bookService.save(book);
                } else {
                    Book memory = memory_books.get(id);
                    List<String> changes = null;
                    try {
                        changes = BeanChangeUtil.changedList(memory, book);
                        if (changes.size() >= 1) {
                            memory_books.put(id, BeanChangeUtil.getNewBean(book, new Book()));
                            logger.info("change of [id: {}] is {}", id, changes);
                            bookService.updateById(book);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // 删除
            Set<Integer> collect = books.stream().map(Book::getBookId).collect(Collectors.toSet());
            Iterator<Map.Entry<Integer, Book>> iterator = memory_books.entrySet().iterator();
            while (iterator.hasNext()) {
                Integer key = iterator.next().getKey();
                if (!collect.contains(key)) {
                    iterator.remove();
                    logger.info("memory_books delete the [id: {}] successfully", key);
                    bookService.removeById(key);
                }
            }
            long end = System.currentTimeMillis();
            books.clear();
            DataSourceContext.clearCache();
            logger.info("the operation cost time [{}] mills", (end - start));
        }
    }

    /**
     * 检验备份数据库是否有数据
     */
    public static void check() {
        if (memory_books.size() == 0) {
            DataSourceContext.setDataSource("slave");
            List<Book> books = bookService.list();
            books.forEach(book -> {
                memory_books.put(book.getBookId(), BeanChangeUtil.getNewBean(book, new Book()));
            });
            logger.info("the original data load over, total size [{}]", books.size());
            books.clear();
            DataSourceContext.clearCache();
        }
    }
}
