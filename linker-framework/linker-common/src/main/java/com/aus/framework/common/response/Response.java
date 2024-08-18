package com.aus.framework.common.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 7711181539023493398L;

    private boolean success = true;

    private String message;

    private String errorCode;

    private T data;

    // --------------------成功响应-----------------------
    public static <T> Response<T> success() {
        return new Response<>();
    }

    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setData(data);
        return response;
    }

    // --------------------失败响应------------------------
    public static <T> Response<T> fail() {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        return response;
    }

    public static <T> Response<T> fail(String errorMessage) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        return response;
    }

    public static <T> Response<T> fail(String errorCode, String errorMessage) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setErrorCode(errorCode);
        return response;
    }

}
