package com.yohann.jsch_test.enums.dict;


import com.yohann.jsch_test.enums.DictEnum;

/**
 * @author yohann
 * @date 2021/8/23 14:39 14
 */
public enum ConnectTcpEnum implements DictEnum {

    CONNECT_TCP_SSH_SFTP("D030_1","SSH_SFTP"),
    CONNECT_TCP_SSH_SUDO_SCP("D030_2","SSH_SUDO_SCP"),

    ;

    private final String code;
    private final String desc;

    ConnectTcpEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
