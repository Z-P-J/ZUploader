package com.zpj.uploader.config;

import com.zpj.uploader.core.UploadManagerImpl;

import java.io.File;

/**
 * @author Z-P-J
 * */
public class UploadMissionConfig extends BaseConfig<UploadMissionConfig> {

    private UploadMissionConfig() {

    }

    public static UploadMissionConfig with() {
        UploaderConfig config = UploadManagerImpl.getInstance().getQianXunConfig();
        if (config == null) {
            throw new RuntimeException("UploaderConfig is null in DownloadManagerImp. You must init first!");
        }
        return new UploadMissionConfig()
                .setUseUtf8(config.useUtf8)
                .setBoundarySignature(config.boundarySignature)
                .setHttpParamName(config.httpParamName)
                .setBufferSize(config.bufferSize)
                .setProgressInterval(config.progressInterval)
                .setThreadPoolConfig(config.threadPoolConfig)
                .setBlockSize(config.blockSize)
                .setRetryCount(config.retryCount)
                .setRetryDelay(config.retryDelay)
                .setConnectOutTime(config.connectOutTime)
                .setReadOutTime(config.readOutTime)
                .setUserAgent(config.userAgent)
                .setCookie(config.cookie)
                .setEnableNotification(config.enableNotification);
    }

    public static UploadMissionConfig with(File file) {
        UploaderConfig config = UploadManagerImpl.getInstance().getQianXunConfig();
        if (config == null) {
            throw new RuntimeException("UploaderConfig is null in DownloadManagerImp. You must init first!");
        }
        return new UploadMissionConfig()
                .setUseUtf8(config.useUtf8)
                .setBoundarySignature(config.boundarySignature)
                .setHttpParamName(config.httpParamName)
                .setBufferSize(config.bufferSize)
                .setProgressInterval(config.progressInterval)
                .setThreadPoolConfig(config.threadPoolConfig)
                .setBlockSize(config.blockSize)
                .setRetryCount(config.retryCount)
                .setRetryDelay(config.retryDelay)
                .setConnectOutTime(config.connectOutTime)
                .setReadOutTime(config.readOutTime)
                .setUserAgent(config.userAgent)
                .setCookie(config.cookie)
                .setEnableNotification(config.enableNotification);
    }

    public int getThreadCount() {
        return threadPoolConfig.getCorePoolSize();
    }

    public int getKeepAliveTime() {
        return threadPoolConfig.getKeepAliveTime();
    }

    public int getMaximumPoolSize() {
        return threadPoolConfig.getMaximumPoolSize();
    }
}
