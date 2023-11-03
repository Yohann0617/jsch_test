package com.yohann.jsch_test.exceptions;

/**
 * <p>
 * IORuntimeException
 * </p >
 *
 * @author yuhui.fan
 * @since 2023/11/3 14:33
 */
public class IORuntimeException extends RuntimeException {
    private static final long serialVersionUID = 8247610319171014183L;


    /**
     * 获得完整消息，包括异常名，消息格式为：{SimpleClassName}: {ThrowableMessage}
     *
     * @param e 异常
     * @return 完整消息
     */
    private static String getMessage(Throwable e) {
        if (null == e) {
            return "null";
        }
        return String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage());
    }

    public IORuntimeException(Throwable e) {
        super(getMessage(e), e);
    }

    public IORuntimeException(String message) {
        super(message);
    }

    public IORuntimeException(String messageTemplate, Object... params) {
        super(String.format(messageTemplate, params));
    }

    public IORuntimeException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public IORuntimeException(Throwable throwable, String messageTemplate, Object... params) {
        super(String.format(messageTemplate, params), throwable);
    }

    /**
     * 导致这个异常的异常是否是指定类型的异常
     *
     * @param clazz 异常类
     * @return 是否为指定类型异常
     */
    public boolean causeInstanceOf(Class<? extends Throwable> clazz) {
        final Throwable cause = this.getCause();
        return null != clazz && clazz.isInstance(cause);
    }
}
