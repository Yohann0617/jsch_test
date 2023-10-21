package com.yohann;

import com.jcraft.jsch.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SSHFileTransferExample {
    public static void main(String[] args) {
        JSch jsch = new JSch();
        Session session = null;

        try {
            // 设置SSH连接参数
            String username = "root";
            String host = "1.1.1.1";
            int port = 22;
            String password = "123456789";

            // 创建SSH会话
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.setTimeout(10000);
            session.connect();

            System.out.println("连接成功");

            // 打开SSH通道
            Channel channel = session.openChannel("exec");

            // 命令：获取文件的InputStream
            String remoteFile = "/root/clean_docker_logs.sh";
            String command = "sudo cat " + remoteFile;
            ((ChannelExec) channel).setCommand(command);

            // 连接通道
            channel.connect();

            // 获取InputStream
            InputStream inputStream = channel.getInputStream();

            // 从InputStream读取文件内容
            byte[] buffer = new byte[1024];
            int bytesRead;
            FileOutputStream outputStream = new FileOutputStream("C:\\Users\\Yohann\\Desktop\\test\\clean_docker_logs.sh");
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                // 处理文件内容，可以将其写入本地文件或进行其他操作
                outputStream.write(buffer);
            }

            // 关闭通道和会话
            inputStream.close();
            outputStream.close();
            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }
    }
}
