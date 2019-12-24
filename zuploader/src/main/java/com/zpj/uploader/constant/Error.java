package com.zpj.uploader.constant;

import android.support.annotation.NonNull;

public class Error {
    
    public static final Error FILE_NOT_FOUND = new Error("文件不存在", false);
    public static final Error HTTP_404 = new Error("404");
    public static final Error HTTP_403 = new Error("403");
    public static final Error IO = new Error("未知IO错误");
    public static final Error SERVER_UNSUPPORTED = new Error("服务器不支持");
    public static final Error CONNECTION_TIME_OUT = new Error("连接超时");
    public static final Error UNKNOWN = new Error("未知错误");
    
    private int errorCode;
    private String errorMsg;
    private boolean retry = true;

    public Error(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Error(String errorMsg, boolean retry) {
        this.errorMsg = errorMsg;
        this.retry = retry;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public boolean canRetry() {
        return retry;
    }

    @NonNull
    @Override
    public String toString() {
        return errorMsg;
    }

    public static Error getHttpError(int responseCode) {
        switch (responseCode) {
            case 404:
                return Error.HTTP_404;
            case 403:
                return Error.HTTP_403;
        }
        return Error.UNKNOWN;
    }
}
