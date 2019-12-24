package com.zpj.uploader;

import android.content.Context;
import android.content.IntentFilter;

import com.zpj.uploader.config.UploadMissionConfig;
import com.zpj.uploader.config.UploaderConfig;
import com.zpj.uploader.core.UploadManager;
import com.zpj.uploader.core.UploadManagerImpl;
import com.zpj.uploader.core.UploadMission;
import com.zpj.uploader.util.NetworkChangeReceiver;
import com.zpj.uploader.util.content.SPHelper;
import com.zpj.uploader.util.notification.NotifyUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Z-P-J
 * */
public class ZUploader {

    private static boolean waitingForInternet = false;

    private final File file;
    private String serverUrl;
    private UploadMissionConfig missionConfig;
    private UploadMission.MissionListener listener;

    private ZUploader(File file) {
        this.file = file;
    }

    public static ZUploader with(File file) {
        return new ZUploader(file);
    }

    public static ZUploader with(String path) {
        return new ZUploader(new File(path));
    }

    public ZUploader setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public ZUploader setMissionConfig(UploadMissionConfig missionConfig) {
        this.missionConfig = missionConfig;
        return this;
    }

    public ZUploader setMissionListener(UploadMission.MissionListener listener) {
        this.listener = listener;
        return this;
    }

    public UploadMission start() {
        UploadMission mission = UploadManagerImpl.getInstance().addMission(serverUrl, file, missionConfig);
        mission.addListener(listener);
        mission.start();
        return mission;
    }






    //------------------------------------------------------------static methods--------------------------------------------------------
    public static<T extends UploadMission> void init(Context context) {
        init(context, UploadMission.class);
    }


    public static<T extends UploadMission> void init(Context context, Class<T> clazz) {
        init(UploaderConfig.with(context), clazz);
    }

    public static<T extends UploadMission> void init(final UploaderConfig options,  Class<T> clazz) {
        final Context context = options.getContext();

        SPHelper.init(context);
        NotifyUtil.init(context);
        UploadManagerImpl.register(options, clazz);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        options.getContext().registerReceiver(NetworkChangeReceiver.getInstance(), intentFilter);
    }

    public static void onDestroy() {
        UploadManagerImpl.unRegister();
        NotifyUtil.cancelAll();
    }

    public static void pause(UploadMission mission) {
        mission.pause();
    }

    public static void pause(String uuid) {
        getDownloadManager().getMission(uuid).pause();
    }

    public static void resume(UploadMission mission) {
        mission.start();
    }

    public static void resume(String uuid) {
        getDownloadManager().getMission(uuid).start();
    }

    public static void delete(UploadMission mission) {
        UploadManagerImpl.getInstance().deleteMission(mission);
    }

    public static void clear(UploadMission mission) {
        mission.pause();
        mission.deleteMissionInfo();
        UploadManagerImpl.getInstance().getMissions().remove(mission);
    }

    public static void clear(String uuid) {
        clear(UploadManagerImpl.getInstance().getMission(uuid));
    }

    public static void pauseAll() {
        UploadManagerImpl.getInstance().pauseAllMissions();
    }

    public static void waitingForInternet() {
        waitingForInternet = true;
        for (UploadMission mission : UploadManagerImpl.getInstance().getMissions()) {
            if (mission.isRunning()) {
                mission.waiting();
            }
        }
    }

    public static boolean isWaitingForInternet() {
        return waitingForInternet;
    }

    public static void resumeAll() {
        UploadManagerImpl.getInstance().resumeAllMissions();
    }

    public static void deleteAll() {
        UploadManagerImpl.getInstance().deleteAllMissions();
    }

    public static void clearAll() {
        UploadManagerImpl.getInstance().clearAllMissions();
    }

    public static UploadManager getDownloadManager() {
        return UploadManagerImpl.getInstance();
    }

    public static List<UploadMission> getAllMissions() {
        return UploadManagerImpl.getInstance().getMissions();
    }

    public static List<UploadMission> getAllMissions(boolean downloading) {
        List<UploadMission> uploadMissionList = new ArrayList<>();
        for (UploadMission mission : getAllMissions()) {
            if (mission.isFinished() != downloading) {
                uploadMissionList.add(mission);
            }
        }
        return uploadMissionList;
    }

    public static<T extends UploadMission> List<T> getAllMissions(Class<T> obj){
        List<T> uploadMissionList = new ArrayList<>();
        for (UploadMission mission : getAllMissions()) {
            uploadMissionList.add((T) mission);
        }
        return uploadMissionList;
    }

}
