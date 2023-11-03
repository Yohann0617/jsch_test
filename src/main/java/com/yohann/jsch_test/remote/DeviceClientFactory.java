package com.yohann.jsch_test.remote;


import com.yohann.jsch_test.enums.dict.ConnectTcpEnum;
import com.yohann.jsch_test.remote.impl.LinuxJschSftpClient;
import com.yohann.jsch_test.remote.impl.LinuxJschSudoScpClient;

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
