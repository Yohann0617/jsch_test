package com.yohann.jschtest.remote.impl;

import com.jcraft.jsch.*;
import com.yohann.jschtest.remote.DeviceConnectParams;
import com.yohann.jschtest.remote.IDeviceClient;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Random;
import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * JschSudoScpClient
 * </p >
 *
 * @author yohann
 * @since 2023/10/23 14:56
 */
@Slf4j
public class LinuxJschSudoScpClient implements IDeviceClient {

    /**
     * 目标服务器session
     */
    private static Session session;
    /**
     * 堡垒机session
     */
    private static Session bastionSession;

    public static void main(String[] args) throws FileNotFoundException {
        LinuxJschSudoScpClient client = new LinuxJschSudoScpClient();
        Map<String, String> map = new HashMap<>();
        map.put("targetIp", "10.18.93.112");
        map.put("username", "yohann");
        map.put("password", "1qaz@WSX");
        map.put("targetPort", "22");
        client.open(map);

//        boolean exists = client.checkExists("/root/test/a", false);
//        System.out.println(exists);

//        long fileSize = client.getFileSize("/root/test/a");
//        System.out.println("文件大小（字节）：" + fileSize);

//        List<String> list = client.recursionGetFileBypath("/root/test/");
//        list.forEach(System.out::println);

//        try (InputStream inputStream = client.getInputStream("/root/test/a");
//             FileOutputStream outputStream = new FileOutputStream("C:\\Users\\Administrator\\Desktop\\test\\a")) {
//            int flat;
//            byte[] buff = new byte[4 * 1024];
//
//            while ((flat = inputStream.read(buff)) >= 0) {
//                outputStream.write(buff, 0, flat);
//            }
//            outputStream.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        long fileSize = client.getFileSize("/root/test/a");
        Random random = new Random();
        int nextInt = random.nextInt(10);
        client.handleOutputStream("/root/test/test/test111/a", fileSize + (nextInt + "\n").getBytes().length, outputStream -> {
            try (InputStream inputStream = client.getInputStream("/root/test/a");) {
                int flat;
                byte[] buff = new byte[4 * 1024];

                while ((flat = inputStream.read(buff)) >= 0) {
                    outputStream.write(buff, 0, flat);
                }

                outputStream.write((nextInt + "\n").getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        client.close();
    }

    /**
     * 校验账号是否支持免密sudo
     */
    public void verifyAccountSupportsPwdLessSudo(String username) {
        checkConnection();

        String cmd = "sudo -n -l -U " + username;
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            // 获取输入流，读取命令执行的输出
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int bytesRead = in.read(buffer, 0, 1024);
                    if (bytesRead < 0) {
                        break;
                    }
                    result.append(new String(buffer, 0, bytesRead));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
            }

            in.close();
            if (!result.toString().contains("NOPASSWD")) {
                log.error("{} does not have sudo access without a password.", username);
                throw new RuntimeException("当前用户：" + username + " 不支持免密sudo，请修改相关配置");
            }
        } catch (JSchException | IOException e) {
            log.error("check account pwd less sudo fail:", e);
            throw new RuntimeException("校验账号：" + username + " 是否支持免密sudo失败");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void open(Map<String, String> params) {
        String targetIp = params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_IP);
        int targetPort = null == params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT) || "".equals(params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT)) ?
                22 : Integer.parseInt(params.get(DeviceConnectParams.CONNECT_PARAM_TARGET_PORT));
        String username = params.get(DeviceConnectParams.CONNECT_PARAM_USERNAME);
        String password = params.get(DeviceConnectParams.CONNECT_PARAM_PASSWORD);
        String isUseBastion = params.get(DeviceConnectParams.CONNECT_PARAM_USE_BASTION);
        String checkNoPwdSudo = params.get(DeviceConnectParams.CONNECT_PARAM_CHECK_PASSWORD_LESS_SUDO);

        JSch jsch = new JSch();
        try {
            // 创建目标服务器Session
            Session targetSession = jsch.getSession(username, targetIp, targetPort);
            targetSession.setConfig("StrictHostKeyChecking", "no"); // 不检查主机密钥
            targetSession.setConfig("PreferredAuthentications", "password");
            targetSession.setPassword(password);
            targetSession.setTimeout(DeviceConnectParams.CONNECT_TIMEOUT);
            targetSession.connect();

            session = targetSession;
//            log.info("连接目标服务器：{}成功", targetIp);

            // 校验用户是否支持免密sudo
            if (checkNoPwdSudo != null && checkNoPwdSudo.equals(DeviceConnectParams.CONNECT_PARAM_CHECK_PASSWORD_LESS_SUDO_YES)) {
                verifyAccountSupportsPwdLessSudo(username);
            }

            return;
        } catch (Exception e) {
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

        try {
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
            log.info("连接目标服务器：{}成功", targetIp);

        } catch (JSchException e) {
            close();
            log.error("登录目标服务器：{} 失败", targetIp, e);
            throw new RuntimeException("connect target device " + targetIp + " error");
        }

        // 校验用户是否支持免密sudo
        if (checkNoPwdSudo != null && checkNoPwdSudo.equals(DeviceConnectParams.CONNECT_PARAM_CHECK_PASSWORD_LESS_SUDO_YES)) {
            verifyAccountSupportsPwdLessSudo(username);
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
        String cmd = "if sudo test -e " + path + "; then     echo \"exist\"; fi";
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            // 获取输入流，读取命令执行的输出
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int bytesRead = in.read(buffer, 0, 1024);
                    if (bytesRead < 0) {
                        break;
                    }
                    result.append(new String(buffer, 0, bytesRead));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
            }

//            System.out.println("Command output: " + result.toString());

            in.close();
            if (result.toString().contains("exist")) {
                return true;
            }
        } catch (JSchException | IOException e) {
            log.error("checkExists fail:", e);
            throw new RuntimeException("checkExists fail");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }

        return false;
    }

    @Override
    public long getFileSize(String path) {
        checkConnection();
        String cmd = "sudo stat -c %s " + path;
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            // 获取输入流，读取命令执行的输出
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int bytesRead = in.read(buffer, 0, 1024);
                    if (bytesRead < 0) {
                        break;
                    }
                    result.append(new String(buffer, 0, bytesRead));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
            }

//            System.out.println("Command output: " + result.toString());

            in.close();
            return Long.parseLong(result.toString().trim());
        } catch (JSchException | IOException e) {
            log.error("getFileSize fail:", e);
            throw new RuntimeException("getFileSize fail");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public List<String> recursionGetFileBypath(String path) {
        checkConnection();
        String cmd = "sudo find " + path + " -type f";
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            // 获取输入流，读取命令执行的输出
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int bytesRead = in.read(buffer, 0, 1024);
                    if (bytesRead < 0) {
                        break;
                    }
                    result.append(new String(buffer, 0, bytesRead));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
            }

//            System.out.println("Command output: " + result.toString());

            in.close();

            return Arrays.asList(result.toString().split("\n"));

        } catch (JSchException | IOException e) {
            log.error("recursionGetFileBypath fail:", e);
            throw new RuntimeException("recursionGetFileBypath fail");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        final PipedInputStream pipIn = new PipedInputStream(1024 * 1024);
        final PipedOutputStream pipOut = new PipedOutputStream();
        try {
            pipIn.connect(pipOut);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("fail:" + e);
        }

        new Thread(() -> {
            Channel channel = null;
            try {
                String rfile = path;
                // exec 'scp -f rfile' remotely
                rfile = rfile.replace("'", "'\"'\"'");
                rfile = "'" + rfile + "'";

//                String command = "echo " + pdwd + " | sudo scp -f " + rfile;
//                String command = "sudo -S scp -f " + rfile + "<<EOF\n" + pwd + "\nEOF";
//                String command = "sudo -S scp -f " + rfile ;
//                String command = "su <<EOF\n" + pwd + "\nEOF\n" + " && " ;
                String command = "sudo scp -f " + rfile;

                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);

                // get I/O streams for remote scp
                OutputStream out = channel.getOutputStream();
                InputStream in = channel.getInputStream();

                channel.connect();

                byte[] buf = new byte[1024];

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                while (true) {
                    int c = checkAck(in);
                    if (c != 'C') {
//                        System.out.println("not C");
//                        System.out.println(c);
                        break;
                    }

                    // read '0644 '
                    in.read(buf, 0, 5);

                    long filesize = 0L;
                    while (true) {
                        if (in.read(buf, 0, 1) < 0) {
                            // error
                            break;
                        }
                        if (buf[0] == ' ') {
                            break;
                        }
                        filesize = filesize * 10L + (long) (buf[0] - '0');
                    }

//                    String file = null;
//                    for (int i = 0; ; i++) {
//                        in.read(buf, i, 1);
//                        if (buf[i] == (byte) 0x0a) {
//                            file = new String(buf, 0, i);
//                            break;
//                        }
//                    }

//                    System.out.println("filesize=" + filesize + ", file=" + file);

                    // send '\0'
                    buf[0] = 0;
                    out.write(buf, 0, 1);
                    out.flush();

                    // read a content of lfile
//                fos = new FileOutputStream(prefix == null ? lfile : prefix + file);
                    int foo;
                    while (true) {
                        if (buf.length < filesize) {
                            foo = buf.length;
                        } else {
                            foo = (int) filesize;
                        }
                        foo = in.read(buf, 0, foo);
                        if (foo < 0) {
                            // error
                            break;
                        }
                        pipOut.write(buf, 0, foo);
                        filesize -= foo;
                        if (filesize == 0L) {
                            break;
                        }
                    }
                    pipOut.close();

//                    if (checkAck(in) != 0) {
//                        System.exit(0);
//                    }

                    // send '\0'
                    buf[0] = 0;
                    out.write(buf, 0, 1);
                    out.flush();
                }

                out.close();
                in.close();
                channel.disconnect();
            } catch (Exception e) {
                log.error("getInputStream fail:{}", path, e);
                throw new RuntimeException("getInputStream fail");
            }
        }).start();

        return pipIn;
    }

    @Override
    public void handleOutputStream(String path, long size, Consumer<OutputStream> outputFunc) {
        checkConnection();
        Channel channel = null;

        try {
            String rfile = path;
            // exec 'scp -f rfile' remotely
            rfile = rfile.replace("'", "'\"'\"'");
            rfile = "'" + rfile + "'";

//                String command = "echo " + pdwd + " | sudo scp -f " + rfile;
//                String command = "sudo -S scp -f " + rfile + "<<EOF\n" + pwd + "\nEOF";
//                String command = "sudo -S scp -f " + rfile ;
//                String command = "su <<EOF\n" + pwd + "\nEOF\n" + " && " ;
            String command = "sudo scp -t " + rfile;

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }

            // send "C0644 filesize filename", where filename should not include '/'
            command = "C0644 " + size + " ";
            if (rfile.lastIndexOf('/') > 0) {
                command += rfile.substring(rfile.lastIndexOf('/') + 1);
            } else {
                command += rfile;
            }
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                in.close();
            }

            OutputStreamProxy outputStreamProxy = new OutputStreamProxy((ChannelExec) channel, out);
            outputFunc.accept(outputStreamProxy);

        } catch (Exception e) {
            log.error("handleOutputStream fail:{}", path, e);
            throw new RuntimeException("handleOutputStream fail");
        }
    }

    @Override
    public void handleOutputStream(String path, Consumer<OutputStream> outputFunc) {
        throw new RuntimeException("不支持");
    }

    private void checkConnection() {
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("connect error");
        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) {
            return b;
        }
        if (b == -1) {
            return b;
        }

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
//                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
//                System.out.print(sb.toString());
            }
        }
        return b;
    }

    static class OutputStreamProxy extends OutputStream {

        private final ChannelExec channel;
        private final OutputStream outputStream;

        public OutputStreamProxy(ChannelExec channel, OutputStream out) {
            this.channel = channel;
            this.outputStream = out;
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
                if (outputStream != null) {
                    outputStream.close();
                }
            } finally {
                if (channel != null) {
                    channel.disconnect();
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }
    }
}
