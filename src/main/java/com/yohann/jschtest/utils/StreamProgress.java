package com.yohann.jschtest.utils;

/**
 * <p>
 * StreamProgress
 * </p >
 *
 * @author yuhui.fan
 * @since 2023/11/3 14:32
 */
public interface StreamProgress {
    /**
     * 开始
     */
    void start();

    /**
     * 进行中
     * @param progressSize 已经进行的大小
     */
    void progress(long progressSize);

    /**
     * 结束
     */
    void finish();

}
