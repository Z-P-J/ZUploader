package com.zpj.uploader.config;

import android.content.Context;
import android.text.TextUtils;

import com.zpj.uploader.constant.DefaultConstant;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Z-P-J
 * */
abstract class BaseConfig<T extends BaseConfig<T>> {

    /**
     * context
     * */
    private transient Context context;

    /**
     * 线程池配置
     * */
    ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.build();

    String boundarySignature = DefaultConstant.BOUNDARY_SIGNATURE;

    /**
     * 下载缓冲大小
     * */
    int bufferSize = DefaultConstant.BUFFER_SIZE;

    long progressInterval = DefaultConstant.PROGRESS_INTERVAL;

    /**
     * 下载块大小
     * */
    int blockSize = DefaultConstant.BLOCK_SIZE;

    /**
     * 默认UserAgent
     * */
    String userAgent = DefaultConstant.USER_AGENT;

    /**
     * 默认Referer
     * */
    String referer = "";

    /**
     * 下载出错重试次数
     * */
    int retryCount = DefaultConstant.RETRY_COUNT;

    /**
     * 下载出错重试延迟时间（单位ms）
     * */
    int retryDelay = DefaultConstant.RETRY_DELAY;

    /**
     * 下载连接超时
     * */
    int connectOutTime = DefaultConstant.CONNECT_OUT_TIME;

    /**
     * 下载链接读取超时
     * */
    int readOutTime = DefaultConstant.READ_OUT_TIME;

    /**
     * 是否允许在通知栏显示任务下载进度
     * */
    boolean enableNotification = true;

    /**
     * 下载时传入的cookie额值
     * */
    String cookie = "";

    final Map<String, String> headers = new HashMap<>();

    final Map<String, String> parameters = new HashMap<>();

    Proxy proxy;
    
    String httpParamName = "";

    boolean useUtf8 = false;

    String contentType = DefaultConstant.CONTENT_TYPE;

    private T self() {
        return (T) this;
    }



    //-----------------------------------------------------------------setter------------------------------------------------------

    void setContext(Context context) {
        this.context = context;
    }

    public T setThreadPoolConfig(ThreadPoolConfig threadPoolConfig) {
        this.threadPoolConfig = threadPoolConfig;
        return self();
    }

    public T setBoundarySignature(String boundarySignature) {
        this.boundarySignature = boundarySignature;
        return self();
    }

    @Deprecated
    public T setThreadCount(int threadCount) {
        threadPoolConfig.setCorePoolSize(threadCount);
        return self();
    }

    public T setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return self();
    }

    public T setProgressInterval(long progressInterval) {
        this.progressInterval = progressInterval;
        return self();
    }

    public T setBlockSize(int blockSize) {
        this.blockSize = blockSize;
        return self();
    }

    public T setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return self();
    }

    public T setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return self();
    }

    public T setCookie(String cookie) {
        this.cookie = cookie;
        return self();
    }

    public T setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
        return self();
    }

    public T setConnectOutTime(int connectOutTime) {
        this.connectOutTime = connectOutTime;
        return self();
    }

    public T setReadOutTime(int readOutTime) {
        this.readOutTime = readOutTime;
        return self();
    }

    public T setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
        return self();
    }

    public T addHeader(String key, String value) {
        this.headers.put(key, value);
        return self();
    }

    public T setParameters(Map<String, String> parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
        return self();
    }

    public T addParameters(String key, String value) {
        this.parameters.put(key, value);
        return self();
    }

    public T setProxy(Proxy proxy) {
        this.proxy = proxy;
        return self();
    }

    public T setProxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
        return self();
    }

    public T setEnableNotification(boolean enableNotification) {
        this.enableNotification = enableNotification;
        return self();
    }

    public T setHttpParamName(String httpParamName) {
        this.httpParamName = httpParamName;
        return self();
    }

    public T setUseUtf8(boolean useUtf8) {
        this.useUtf8 = useUtf8;
        return self();
    }

    public T setContentType(String contentType) {
        if (TextUtils.isEmpty(contentType)) {
            contentType = DefaultConstant.CONTENT_TYPE;
        }
        this.contentType = contentType;
        return self();
    }

    public T setReferer(String referer) {
        this.referer = referer;
        return self();
    }

    //-----------------------------------------------------------getter-------------------------------------------------------------

    public Context getContext() {
        return context;
    }

    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }

    public String getBoundarySignature() {
        return boundarySignature;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public long getProgressInterval() {
        return progressInterval;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getCookie() {
        return cookie == null ? "" : cookie;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public int getConnectOutTime() {
        return connectOutTime;
    }

    public int getReadOutTime() {
        return readOutTime;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public boolean getEnableNotificatio() {
        return enableNotification;
    }

    public String getHttpParamName() {
        return httpParamName;
    }

    public boolean isUseUtf8() {
        return useUtf8;
    }

    public String getContentType() {
        return contentType;
    }

    public String getReferer() {
        return referer;
    }
}
