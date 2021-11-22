package com.chr.fservice.upload.nio;

/**
 * @author RAY
 * @descriptions
 * @since 2021/11/22
 */
public class FileProgress implements Runnable {

    private volatile Integer write = -1;
    private Long total;
    private long sleepMillis;

    public FileProgress(long total) {
        this(total, 500);
    }

    public FileProgress(Long total, long sleepMillis) {
        this.total = total;
        this.sleepMillis = sleepMillis <= 100 ? 500 : sleepMillis;
    }

    @Override
    public void run() {
        while (write < total) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ignored) {
            }
            String result = String.format("%.2f", ((write.doubleValue() / total.doubleValue()) * 100)) + "%";
            if (!result.equals("100.00%")) {
                System.err.print(" ==> " + result);
            }
        }
    }

    public void setCurrentWrite(int write) {
        this.write = write;
    }

}
