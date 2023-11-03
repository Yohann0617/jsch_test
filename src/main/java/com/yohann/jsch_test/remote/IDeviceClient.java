package com.yohann.jsch_test.remote;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 远程设备客户端
 *
 * @author yohann
 */
public interface IDeviceClient {

    /**
     * 打开设备
     *
     * @param params 初始化参数
     */
    void open(Map<String, String> params);

    /**
     * 关闭设备
     */
    void close();

    /**
     * 检查文件/路径是否存在
     *
     * @param path        路径
     * @param isDirectory 是否为目录
     * @return true/false
     */
    boolean checkExists(String path, boolean isDirectory);

    /**
     * get file size
     *
     * @param path 路径
     * @return size
     */
    long getFileSize(String path);

    /**
     * 递归获取设备上某路径下所有文件
     *
     * @param path 文件路径
     * @return 文件信息 无 返回 空list
     */
    List<String> recursionGetFileBypath(String path);

    /**
     * 获取设备文件输入流
     *
     * @param path 文件路径
     * @return InputStream
     */
    InputStream getInputStream(String path);

    /**
     * 处理设备文件输出流
     *
     * @param path       文件路径
     * @param size       文件大小
     * @param outputFunc OutputStream
     */
    default void handleOutputStream(String path, long size, Consumer<OutputStream> outputFunc) {
        handleOutputStream(path, outputFunc);
    }

    /**
     * 处理设备文件输出流
     *
     * @param path       文件路径
     * @param outputFunc OutputStream
     */
    void handleOutputStream(String path, Consumer<OutputStream> outputFunc);

}
