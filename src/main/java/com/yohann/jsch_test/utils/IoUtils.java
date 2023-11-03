package com.yohann.jsch_test.utils;

import com.yohann.jsch_test.exceptions.IORuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * <p>
 * IoUtils
 * </p >
 *
 * @author yohann
 * @since 2023/11/3 14:31
 */
public class IoUtils {

    /**
     * 默认缓存大小 8192
     */
    public static final int DEFAULT_BUFFER_SIZE = 2 << 12;
    /**
     * 默认中等缓存大小 16384
     */
    public static final int DEFAULT_MIDDLE_BUFFER_SIZE = 2 << 13;
    /**
     * 默认大缓存大小 32768
     */
    public static final int DEFAULT_LARGE_BUFFER_SIZE = 2 << 14;

    /**
     * 数据流末尾
     */
    public static final int EOF = -1;


    /**
     * 拷贝流，使用默认Buffer大小，拷贝后不关闭流
     *
     * @param in  输入流
     * @param out 输出流
     * @return 传输的byte数
     * @throws IORuntimeException IO异常
     */
    public static long copy(InputStream in, OutputStream out) throws IORuntimeException {
        return copy(in, out, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 拷贝流，拷贝后不关闭流
     *
     * @param in         输入流
     * @param out        输出流
     * @param bufferSize 缓存大小
     * @return 传输的byte数
     * @throws IORuntimeException IO异常
     */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IORuntimeException {
        return copy(in, out, bufferSize, null);
    }

    /**
     * 拷贝流，拷贝后不关闭流
     *
     * @param in             输入流
     * @param out            输出流
     * @param bufferSize     缓存大小
     * @param streamProgress 进度条
     * @return 传输的byte数
     * @throws IORuntimeException IO异常
     */
    public static long copy(InputStream in, OutputStream out, int bufferSize, StreamProgress streamProgress) throws IORuntimeException {
        Optional.ofNullable(in).orElseThrow(() -> new RuntimeException("InputStream is null !"));
        Optional.ofNullable(out).orElseThrow(() -> new RuntimeException("OutputStream is null !"));

        if (bufferSize <= 0) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }

        byte[] buffer = new byte[bufferSize];
        if (null != streamProgress) {
            streamProgress.start();
        }
        long size = 0;
        try {
            for (int readSize; (readSize = in.read(buffer)) != EOF; ) {
                out.write(buffer, 0, readSize);
                size += readSize;
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
            out.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }
        return size;
    }
}
