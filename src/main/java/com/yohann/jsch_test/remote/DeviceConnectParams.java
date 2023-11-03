package com.yohann.jsch_test.remote;

/**
 * <p>
 * ConnectParams
 * </p >
 *
 * @author yohann
 * @since 2023/10/25 16:11
 */
public class DeviceConnectParams {

    /**
     * 用户名
     */
    public static final String CONNECT_PARAM_USERNAME = "username";
    /**
     * 密码
     */
    public static final String CONNECT_PARAM_PASSWORD = "password";
    /**
     * 目标服务器ip
     */
    public static final String CONNECT_PARAM_TARGET_IP = "targetIp";
    /**
     * 目标服务器连接端口
     */
    public static final String CONNECT_PARAM_TARGET_PORT = "targetPort";
    /**
     * 连接超时时间，单位（毫秒）
     */
    public static final int CONNECT_TIMEOUT = 10000;

    /**
     * 是否通过堡垒机连接。yes：是
     */
    public static final String CONNECT_PARAM_USE_BASTION = "useBastion";
    /**
     * 通过堡垒机连接
     */
    public static final String CONNECT_PARAM_USE_BASTION_YES = "yes";
    /**
     * 堡垒机连接ip
     */
    public static final String CONNECT_PARAM_BASTION_IP = "bastionIp";
    /**
     * 堡垒机连接端口
     */
    public static final String CONNECT_PARAM_BASTION_PORT = "bastionPort";

    /**
     * 是否校验免密sudo
     */
    public static final String CONNECT_PARAM_CHECK_PASSWORD_LESS_SUDO = "pwdLessSudo";
    /**
     * 校验免密sudo
     */
    public static final String CONNECT_PARAM_CHECK_PASSWORD_LESS_SUDO_YES = "yes";
}
