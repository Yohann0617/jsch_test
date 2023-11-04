package com.yohann.jschtest.remote;


import com.yohann.jschtest.enums.dict.ConnectTcpEnum;
import com.yohann.jschtest.remote.impl.LinuxJschSftpClient;
import com.yohann.jschtest.remote.impl.LinuxJschSudoScpClient;

/**
 * 远程设备客户端工厂类
 * @author yohann
 */
public class DeviceClientFactory {

    public static IDeviceClient getDeviceClient(ConnectTcpEnum type){

        switch (type){
            case CONNECT_TCP_SSH_SFTP:
                return new LinuxJschSftpClient();
            case CONNECT_TCP_SSH_SUDO_SCP:
                return new LinuxJschSudoScpClient();
            default:
               throw new RuntimeException("not support");
        }
    }
}
