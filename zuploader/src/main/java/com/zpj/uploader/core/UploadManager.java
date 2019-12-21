package com.zpj.uploader.core;

import android.content.Context;

import com.zpj.uploader.config.UploadMissionConfig;
import com.zpj.uploader.config.UploaderConfig;
import com.zpj.uploader.config.ThreadPoolConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface UploadManager {

    List<UploadMission> ALL_MISSIONS = new ArrayList<>();

    interface DownloadManagerListener {
        void onMissionAdd();

        void onMissionDelete();

        void onMissionFinished();
    }

    UploadMission addMission(String url, File file);

    UploadMission addMission(String url, File file, UploadMissionConfig config);

    void resumeMission(int id);

    void resumeMission(String uuid);

    void resumeAllMissions();

    void pauseMission(int id);

    void pauseMission(String uuid);

    void pauseAllMissions();

    void deleteMission(int id);

    void deleteMission(String uuid);

    void deleteMission(UploadMission mission);

    void deleteAllMissions();

    void clearMission(int i);

    void clearMission(String uuid);

    void clearAllMissions();

    UploadMission getMission(int id);

    UploadMission getMission(String uuid);

    int getCount();

    Context getContext();

    UploaderConfig getQianXunConfig();

    ThreadPoolConfig getThreadPoolConfig();

    boolean shouldMissionWaiting();

    void loadMissions();

    void setDownloadManagerListener(DownloadManagerListener downloadManagerListener);

    DownloadManagerListener getDownloadManagerListener();

    List<UploadMission> getMissions();
}
