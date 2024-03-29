package com.chr.fservice.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serializable;

/**
 * <p>
 * 书籍表
 * </p>
 *
 * @author RAY
 * @since 2020-05-03
 */
public class Book extends Model<Book> {

    @ExcelIgnore
    private static final long serialVersionUID = 1L;

    /**
     * 书籍id
     */
    @TableId(value = "book_id")
    @ExcelProperty(value = "书籍Id", order = 1)
    private Integer bookId;

    /**
     * 书籍编号
     */
    @ExcelProperty(value = "书籍编号", order = 2)
    private String bookNo;

    /**
     * 书籍姓名
     */
    @ExcelProperty(value = "书籍名", order = 3)
    private String bookName;

    /**
     * 书的状态 1为已被借阅  0为未借阅
     */
    @ExcelProperty(value = "借阅状态", order = 4)
    private String bookState;

    public Integer getBookId() {
        return bookId;
    }

    public Book() {
    }

    public Book(Integer bookId, String bookNo, String bookName, String bookState) {
        this.bookId = bookId;
        this.bookNo = bookNo;
        this.bookName = bookName;
        this.bookState = bookState;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }
    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public String getBookState() {
        return bookState;
    }

    public void setBookState(String bookState) {
        this.bookState = bookState;
    }

    @Override
    protected Serializable pkVal() {
        return this.bookId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Book{");
        sb.append("bookId=").append(bookId);
        sb.append(", bookNo='").append(bookNo).append('\'');
        sb.append(", bookName='").append(bookName).append('\'');
        sb.append(", bookState='").append(bookState).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
