package com.zpj.uploader.config;

import android.content.Context;

import com.zpj.uploader.constant.DefaultConstant;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;


/**
* @author Z-P-J
* */
public class UploaderConfig extends BaseConfig<UploaderConfig> {

    private int concurrentMissionCount = DefaultConstant.CONCURRENT_MISSION_COUNT;

    private UploaderConfig() {

    }

    public static UploaderConfig with(Context context) {
        UploaderConfig options = new UploaderConfig();
        options.setContext(context);
        return options;
    }

    public int getConcurrentMissionCount() {
        return concurrentMissionCount;
    }

    public UploaderConfig setConcurrentMissionCount(int concurrentMissionCount) {
        this.concurrentMissionCount = concurrentMissionCount;
        return this;
    }
}
