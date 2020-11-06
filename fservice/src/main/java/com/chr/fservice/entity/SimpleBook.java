package com.chr.fservice.entity;

/**
 * @author RAY
 * @descriptions
 * @since 2020/7/23
 */
public class SimpleBook {
    private Integer bookId;
    private String bookNo;
    private boolean test;

    public SimpleBook(Integer bookId, String bookNo) {
        this.bookId = bookId;
        this.bookNo = bookNo;
    }

    public SimpleBook(Integer bookId, String bookNo, boolean test) {
        this.bookId = bookId;
        this.bookNo = bookNo;
        this.test = test;
    }

    public Integer getBookId() {
        return bookId;
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

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SimpleBook{");
        sb.append("bookId=").append(bookId);
        sb.append(", bookNo='").append(bookNo).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
