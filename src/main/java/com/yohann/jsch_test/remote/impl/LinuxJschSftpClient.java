package com.yohann.jsch_test.remote.impl;

import com.jcraft.jsch.*;
import com.yohann.jsch_test.remote.DeviceConnectParams;
import com.yohann.jsch_test.remote.IDeviceClient;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * BastionHostClient
 * </p >
 *
 * @author yohann
 * @since 2023/8/3 17:35
 */
@Slf4j
public class LinuxJschSftpClient implements IDeviceClient {

    /**
     * 目标服务器session
     */
    private static Session session;
    /**
     * 堡垒机session
     */
    private static Session bastionSession;

    @Override
    public void open(Map<String, String> params) {
        String targetIp = params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_IP);
        int targetPort = null == params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT) || "".equals(params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT)) ?
                22 : Integer.parseInt(params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT));
        String username = params.get(DeviceConnectParams.CONNECT_PARAM_USERNAME);
        String password = params.get(DeviceConnectParams.CONNECT_PARAM_PASSWORD);
        String isUseBastion = params.get(DeviceConnectParams.CONNECT_PARAM_USE_BASTION);

        JSch jsch = new JSch();

        // 直接连接目标服务器
        try {
            // 创建目标服务器Session
            Session targetSession = jsch.getSession(username, targetIp, targetPort);
            targetSession.setConfig("StrictHostKeyChecking", "no"); // 不检查主机密钥
            targetSession.setConfig("PreferredAuthentications", "password");
            targetSession.setPassword(password);
            targetSession.setTimeout(DeviceConnectParams.CONNECT_TIMEOUT);
            targetSession.connect();

            session = targetSession;
            return;
        } catch (JSchException e) {
            close();
            // 如果没有配置通过堡垒机连接，则直接报错，反之不处理异常，通过堡垒机连接
            if (isUseBastion == null) {
                log.error("登录目标服务器：{} 失败：", targetIp, e);
                throw new RuntimeException("connect target device " + targetIp + " error");
            }
            if (!DeviceConnectParams.CONNECT_PARAM_USE_BASTION_YES.equals(isUseBastion)) {
                log.error("登录目标服务器：{} 失败：", targetIp, e);
                throw new RuntimeException("connect target device " + targetIp + " error");
            }
        }

        // 通过堡垒机连接
        try {
//            log.info("直接连接目标服务器失败，正在通过堡垒机连接...");
            String bastionIp = params.get(DeviceConnectParams.CONNECT_PARAM_BASTION_IP);
            int bastionPort = Integer.parseInt(params.get(DeviceConnectParams.CONNECT_PARAM_BASTION_PORT));

            // 创建堡垒机Session
            Session bSession = jsch.getSession(username, bastionIp, bastionPort);
            bSession.setConfig("StrictHostKeyChecking", "no"); // 不检查主机密钥
            bSession.setConfig("PreferredAuthentications", "password");
            bSession.setPassword(password);
            bSession.setTimeout(DeviceConnectParams.CONNECT_TIMEOUT);
            bSession.connect();

            bastionSession = bSession;
//            log.info("连接堡垒机：{}成功", bastionIp);

            // 设置本地端口转发，将本地端口与目标服务器建立隧道
            int localPort = bastionSession.setPortForwardingL(0, targetIp, targetPort);
//            log.info("本地转发端口：{}", localPort);

            // 创建目标服务器Session
            Session targetSession = jsch.getSession(username, "127.0.0.1", localPort);
            targetSession.setConfig("StrictHostKeyChecking", "no"); // 不检查主机密钥
            targetSession.setConfig("PreferredAuthentications", "password");
            targetSession.setPassword(password);
            targetSession.setTimeout(DeviceConnectParams.CONNECT_TIMEOUT);
            targetSession.connect();

            session = targetSession;
//            log.info("连接目标服务器：{}成功", targetIp);

        } catch (JSchException e) {
            close();
            log.error("登录目标服务器：{} 失败：", targetIp, e);
            throw new RuntimeException("connect target device " + targetIp + " error");
        }
    }

    @Override
    public void close() {
        if (null != session) {
            session.disconnect();
        }
        if (null != bastionSession) {
            bastionSession.disconnect();
        }
    }

    @Override
    public boolean checkExists(String path, boolean isDirectory) {
        checkConnection();
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            SftpATTRS attributes = channel.stat(path);
            return isDirectory ? attributes.isDir() : attributes.isReg();
        } catch (SftpException e) {
            if (e.getMessage().contains("No such file")) {
                return false;
            }
            log.error("check file exists error,file:{},error:", path, e);
            throw new RuntimeException("check file exists error");
        } catch (JSchException e) {
            log.error("check file exists error,file:{},error:", path, e);
            throw new RuntimeException("check file exists error");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public long getFileSize(String path) {
        checkConnection();
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            SftpATTRS attributes = channel.stat(path);
            return attributes.getSize();
        } catch (SftpException | JSchException e) {
            log.error("get file size error,file:{},error:", path, e);
            throw new RuntimeException("get file size error");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public List<String> recursionGetFileBypath(String path) {
        checkConnection();
        List<String> result = new ArrayList<>();
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            recursion(channel, path, result);
        } catch (Exception e) {
            log.error("recursionGetFileBypath error,file:{},error:", path, e);
            throw new RuntimeException("recursionGetFileBypath error");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return result;
    }

    private void recursion(ChannelSftp channel, String path, List<String> result) {
        try {
            SftpATTRS attributes = channel.stat(path);
            if (attributes.isReg()) {
                result.add(path);
                return;
            }
            if (attributes.isDir()) {
                Vector files = channel.ls(path);
                for (Object object : files) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) object;
                    if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                        continue;
                    }
                    String filePath = path + "/" + entry.getFilename();
                    recursion(channel, filePath, result);
                }
            }
        } catch (SftpException e) {
            log.error("recursion get file error,file:{},error:", path, e);
            throw new RuntimeException("recursion get file error");
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        checkConnection();
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            InputStream inputStream = channel.get(path);
            return new InputStreamProxy(channel, inputStream);
        } catch (SftpException | JSchException e) {
            log.error("can not read file:{}", path, e);
            throw new RuntimeException("read file:" + path + " error");
        }
    }

    @Override
    public void handleOutputStream(String path, Consumer<OutputStream> outputFunc) {
        checkConnection();
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            OutputStream outputStream = channel.put(path);
            OutputStreamProxy outputStreamProxy = new OutputStreamProxy(channel, outputStream);
            outputFunc.accept(outputStreamProxy);
        } catch (SftpException | JSchException e) {
            log.error("can not write file:{}", path, e);
            throw new RuntimeException("write file:" + path + "error");
        }
    }

    private void checkConnection() {
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("connect error");
        }
    }

    static class InputStreamProxy extends InputStream {

        private final ChannelSftp channel;
        private final InputStream inputStream;

        public InputStreamProxy(ChannelSftp channel, InputStream stream) {
            this.channel = channel;
            this.inputStream = stream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(@NotNull byte[] b) throws IOException {
            return inputStream.read(b);
        }

        @Override
        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return inputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public void close() throws IOException {
            try {
                inputStream.close();
            } finally {
                channel.disconnect();
            }
        }

        @Override
        public synchronized void mark(int readLimit) {
            inputStream.mark(readLimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            inputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }
    }

    static class OutputStreamProxy extends OutputStream {

        private final ChannelSftp channel;
        private final OutputStream outputStream;

        public OutputStreamProxy(ChannelSftp channel, OutputStream stream) {
            this.channel = channel;
            this.outputStream = stream;
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                outputStream.close();
            } finally {
                channel.disconnect();
            }
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }
    }

    public static void main(String[] args) throws IOException {
        LinuxJschSftpClient client = new LinuxJschSftpClient();
        Map<String, String> map = new HashMap<>();
        map.put("bastionIp", "10.18.93.133");
        map.put("bastionPort", "60022");
        map.put("targetIp", "10.18.93.201");
        map.put("targetPort", "22");
        map.put("username", "liujian");
        map.put("password", "1qaz@WSX");
        client.open(map);

        long fileSize = client.getFileSize("/home/liujian/test.txt");
        System.out.println("文件大小：" + fileSize);

//        InputStream inputStream = client.getInputStream("/home/liujian/test.txt");
//        FileUtil.writeFromStream(inputStream, "C:\\Users\\Administrator\\Desktop\\test.txt");
//        inputStream.close();

        client.close();
    }
}
