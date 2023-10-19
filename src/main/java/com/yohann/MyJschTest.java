package com.yohann;

import com.jcraft.jsch.*;

import java.util.Scanner;

/**
 * <p>
 * MyJschTest
 * </p >
 *
 * @author yuhui.fan
 * @since 2023/10/19 17:01
 */
public class MyJschTest {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入4个参数：[参数1：目标IP][参数2：目标端口（默认22）][参数3：username][参数4：password]");

        // 依次接收用户输入的4个参数
        String[] inputParams = new String[4];
        for (int i = 0; i < 4; i++) {
            System.out.print("参数 " + (i + 1) + ": ");
            inputParams[i] = scanner.nextLine();
        }

        String host = inputParams[0];
        Integer port = Integer.valueOf(inputParams[1]);
        String username = inputParams[2];
        String password = inputParams[3];

        //创建远程连接，默认连接端口为22，如果不使用默认，可以使用方法 new Connection(ip, port)创建对象
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            System.out.println("请输入文件的全路径，例：（/root/cgsa/cgsa.log）：");
            String path = scanner.nextLine();
            System.out.println("请输入需要下载到哪里，例：（C:\\Users\\Administrator\\Desktop\\test\\cgsa.log）：");
            String path2 = scanner.nextLine();

            channelSftp.get(path, path2);
            System.out.println("File downloaded successfully.");
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }


    }

}
