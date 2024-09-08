package com.aus.framework.common.utils;

import java.util.regex.Pattern;

public class ParamUtils {

    private ParamUtils() {

    }

    // ===================== 校验昵称 =====================
    // 定义昵称长度范围
    private static final int NICK_NAME_MIN_LENGTH = 2;
    private static final int NICK_NAME_MAX_LENGTH = 24;

    // 定义特殊字符的正则表达式
    private static final String NICK_NAME_REGEX = "[!@#$%^&*(),.?\":{}|<>]";

    /**
     * 昵称校验
     * @param nickName
     * @return
     */
    public static boolean checkNickName(String nickName) {
        // 检查长度
        if (nickName.length() < NICK_NAME_MIN_LENGTH || nickName.length() > NICK_NAME_MAX_LENGTH) {
            return false;
        }

        // 检查是否含有特殊字符
        Pattern pattern = Pattern.compile(NICK_NAME_REGEX);
        return !pattern.matcher(nickName).find();
    }

    // ===================== 校验 Linker 号 =====================
    // 定义 ID 长度范围
    private static final int ID_MIN_LENGTH = 6;
    private static final int ID_MAX_LENGTH = 15;

    // 定义正则表达式
    private static final String ID_REGEX = "^[a-zA-Z0-9_]+$";

    /**
     * Linker ID 校验
     * @param linkerId
     * @return
     */
    public static boolean checkLinkerId(String linkerId) {
        // 检查长度
        if (linkerId.length() < ID_MIN_LENGTH || linkerId.length() > ID_MAX_LENGTH) {
            return false;
        }
        Pattern pattern = Pattern.compile(ID_REGEX);
        return pattern.matcher(linkerId).matches();
    }

    /**
     * 检验字符串长度
     * @param str
     * @param length
     * @return
     */
    public static boolean checkLength(String str, int length){
        // 检查长度
        if (str.isEmpty() || str.length() > length) {
            return false;
        }
        return true;
    }

}
