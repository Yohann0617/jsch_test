package com.yohann.jsch_test;

import com.yohann.jsch_test.enums.EnumUtils;
import com.yohann.jsch_test.enums.dict.ConnectTcpEnum;
import com.yohann.jsch_test.remote.DeviceClientFactory;
import com.yohann.jsch_test.remote.IDeviceClient;
import com.yohann.jsch_test.utils.IoUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * <p>
 * 支持堡垒机连接目标服务器，并完成文件上传和下载
 * </p >
 *
 * @author yohann
 * @since 2023/10/17 15:29
 */
public class BastionHostSftpClientTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入7个参数：[参数1：堡垒机IP][参数2：堡垒机端口][参数3：目标IP][参数4：目标端口][参数5：username][参数6：password][参数7：协议枚举值（D030_1：SFTP；D030_2：SCP）]");

        // 依次接收用户输入的7个参数
        String[] inputParams = new String[7];
        for (int i = 0; i < 7; i++) {
            System.out.print("参数 " + (i + 1) + ": ");
            inputParams[i] = scanner.nextLine();
        }

        Map<String, String> map = new HashMap<>();
        map.put("bastionIp", inputParams[0]);
        map.put("bastionPort", inputParams[1]);
        map.put("targetIp", inputParams[2]);
        map.put("targetPort", inputParams[3]);
        map.put("username", inputParams[4]);
        map.put("password", inputParams[5]);
        map.put("agreement", inputParams[6]);

        IDeviceClient client = DeviceClientFactory.getDeviceClient(EnumUtils.getEnumByCode(ConnectTcpEnum.class, map.get("agreement")));
        client.open(map);

        System.out.println("请输入操作类型：[0：获取文件大小] [1：查询文件是否存在] [2：查询指定目录下所有文件并输出全路径]");
        System.out.println("请输入操作类型：[3：下载文件] [4：上传文件]");
        String operateType = scanner.nextLine();

        switch (operateType) {
            case "0":
                System.out.println("请输入文件全路径：");
                String path0 = scanner.nextLine();
                long fileSize = client.getFileSize(path0);
                System.out.println("文件" + path0 + "大小：" + fileSize);

                break;
            case "1":
                System.out.println("请输入文件全路径：");
                String path1 = scanner.nextLine();
                boolean exists = client.checkExists(path1, false);
                System.out.println("文件" + path1 + "是否存在：" + exists);

                break;
            case "2":
                System.out.println("请输入目录全路径：");
                String path2 = scanner.nextLine();
                List<String> filePathList = client.recursionGetFileBypath(path2);
                System.out.println("目录：" + path2 + "下文件如下：");
                filePathList.forEach(System.out::println);

                break;
            case "3":
                System.out.println("请输入文件全路径，例（/etc/group）：");
                String downloadFilePath = scanner.nextLine();
                System.out.println("请输入文件需要下载到哪里，例如：（/home/xuhuiadmin/test/group）");
                String downloadFileTargetPath = scanner.nextLine();

                boolean checkDownloadFileExists = client.checkExists(downloadFilePath, false);
                if (!checkDownloadFileExists) {
                    throw new RuntimeException("文件：" + downloadFilePath + " 不存在！");
                }

                try (InputStream inputStream = client.getInputStream(downloadFilePath);
                     FileOutputStream downloadFileOutputStream = new FileOutputStream(downloadFileTargetPath);) {
                    IoUtils.copy(inputStream, downloadFileOutputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("文件下载成功，位置：" + downloadFileTargetPath);
                break;
            case "4":
                System.out.println("请输入本地文件全路径，例（/etc/group）：");
                String localFilePath = scanner.nextLine();
                System.out.println("请输入文件需要上传到哪里，例如：（/home/xuhuiadmin/test/group），会自动创建文件");
                String uploadFileTargetPath = scanner.nextLine();

                File localFile = new File(localFilePath);
                if (!localFile.exists()) {
                    throw new RuntimeException("文件：" + localFilePath + " 不存在！");
                }

                client.handleOutputStream(uploadFileTargetPath, localFile.length(), outputStream -> {
                    try (InputStream localInputStream = new FileInputStream(localFile);) {
                        IoUtils.copy(localInputStream, outputStream);
                        System.out.println("文件上传至：" + map.get("targetIp") + "成功！位置：" + uploadFileTargetPath);
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

                break;
            default:
                throw new RuntimeException("不支持的操作类型");
        }


        // 关闭Scanner
        scanner.close();
        client.close();
    }
}
