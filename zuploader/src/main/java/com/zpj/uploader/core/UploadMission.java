package com.zpj.uploader.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.gson.Gson;
import com.zpj.uploader.config.UploadMissionConfig;
import com.zpj.uploader.constant.ErrorCode;
import com.zpj.uploader.util.FileUtil;
import com.zpj.uploader.util.ThreadPoolFactory;
import com.zpj.uploader.util.Utility;
import com.zpj.uploader.util.notification.NotifyUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Z-P-J
 */
public class UploadMission {
    private static final String TAG = UploadMission.class.getSimpleName();

    public interface MissionListener {
        HashMap<MissionListener, Handler> HANDLER_STORE = new HashMap<>();

        void onInit();

        void onStart();

        void onPause();

        void onWaiting();

        void onRetry();

        void onProgress(UpdateInfo update);

        void onFinish();

        void onError(int errCode);
    }

    public enum MissionStatus {
        INITING("准备中"),
        START("已开始"),
        RUNNING("下载中"),
        WAITING("等待中"),
        PAUSE("已暂停"),
        FINISHED("已完成"),
        ERROR("出错了"),
        RETRY("重试中");

        private String statusName;

        MissionStatus(String name) {
            statusName = name;
        }

        @Override
        public String toString() {
            return statusName;
        }
    }

    private String uuid = "";
    private String fileName = "";
    private String url = "";
    private String filePath;
    private long createTime = 0;
    private int notifyId = 0;
    private long totalBytes = 0;
    private long uploadedBytes = 0;
    private MissionStatus missionStatus = MissionStatus.INITING;
    private int errCode = -1;
    private UploadMissionConfig uploadMissionConfig = UploadMissionConfig.with();

    //-----------------------------------------------------transient---------------------------------------------------------------

    private transient int currentRetryCount = uploadMissionConfig.getRetryCount();

    private transient final int threadCount = 1;

    private transient ArrayList<WeakReference<MissionListener>> mListeners = new ArrayList<>();

    private transient boolean mWritingToFile = false;

    private transient ThreadPoolExecutor threadPoolExecutor;

    private transient long lastDone = -1;
    private transient String tempSpeed = "0 KB/s";
    private transient final UpdateInfo updateInfo = new UpdateInfo();
    private transient final Handler handler = new Handler(Looper.getMainLooper());


    //------------------------------------------------------runnables---------------------------------------------

    private final transient Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "progressRunnable--start");
            if (isFinished()) {
                handler.removeCallbacks(progressRunnable);
                return;
            }
            handler.postDelayed(progressRunnable, uploadMissionConfig.getProgressInterval());
            long downloaded = uploadedBytes;
            long delta = downloaded - lastDone;
            Log.d(TAG, "progressRunnable--delta=" + delta);
            if (delta > 0) {
                lastDone = downloaded;
                float speed = delta * (uploadMissionConfig.getProgressInterval() / 1000f);
                tempSpeed = Utility.formatSpeed(speed);
            }
            String downloadedSizeStr = Utility.formatSize(downloaded);
            float progress = getProgress(downloaded, totalBytes);
            Log.d(TAG, "progressRunnable--tempSpeed=" + tempSpeed);
            updateInfo.setDone(downloaded);
            updateInfo.setSize(totalBytes);
            updateInfo.setProgress(progress);
            updateInfo.setFileSizeStr(getFileSizeStr());
            updateInfo.setDownloadedSizeStr(downloadedSizeStr);
            updateInfo.setProgressStr(String.format(Locale.US, "%.2f%%", progress));
            updateInfo.setSpeedStr(tempSpeed);
            writeMissionInfo();
            notifyStatus(MissionStatus.RUNNING);
            if (uploadMissionConfig.getEnableNotificatio()) {
                NotifyUtil.with(getContext())
                        .buildProgressNotify()
                        .setProgressAndFormat(getProgress(), false, "")
                        .setContentTitle(fileName)
                        .setId(getNotifyId())
                        .show();
            }
        }
    };

    private final transient Runnable writeMissionInfoRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "writeMissionInfoRunnable--start   getMissionInfoFilePath()=" + getMissionInfoFilePath());
            String content = new Gson().toJson(UploadMission.this);
            Log.d(TAG, "writeMissionInfoRunnable--content=" + content);
            Utility.writeToFile(getMissionInfoFilePath(),content);
            mWritingToFile = false;
            Log.d(TAG, "writeMissionInfoRunnable--finished");
        }
    };

    private UploadMission() {

    }

    public static UploadMission create(String url, File file, UploadMissionConfig config) {
        UploadMission mission = new UploadMission();
        mission.url = url;
        mission.fileName = file.getName();
        mission.filePath = file.getAbsolutePath();
        mission.totalBytes = file.length();
        mission.uuid = UUID.randomUUID().toString();
        mission.createTime = System.currentTimeMillis();
        mission.missionStatus = MissionStatus.INITING;
        mission.uploadMissionConfig = config;
        return mission;
    }

    //-------------------------下载任务状态-----------------------------------
    public boolean isIniting() {
        return missionStatus == MissionStatus.INITING;
    }

    public boolean isRunning() {
        return missionStatus == MissionStatus.RUNNING;
    }

    public boolean isWaiting() {
        return missionStatus == MissionStatus.WAITING;
    }

    public boolean isPause() {
        return missionStatus == MissionStatus.PAUSE;
    }

    public boolean isFinished() {
        return missionStatus == MissionStatus.FINISHED;
    }

    public boolean isError() {
        return missionStatus == MissionStatus.ERROR;
    }

    public boolean canPause() {
        return isRunning() || isWaiting() || isIniting();
    }

    public boolean canStart() {
        return isPause() || isError() || isIniting();
    }


    //----------------------------------------------------------operation------------------------------------------------------------
    void init() {
        currentRetryCount = uploadMissionConfig.getRetryCount();
        lastDone = uploadedBytes;
        uploadMissionConfig.getThreadPoolConfig().setCorePoolSize(2);
        uploadMissionConfig.getThreadPoolConfig().setMaximumPoolSize(4);
        if (threadPoolExecutor == null) {
            threadPoolExecutor = ThreadPoolFactory.newFixedThreadPool(uploadMissionConfig.getThreadPoolConfig());
        }
        writeMissionInfo();
    }

    public void start() {
        if (!isRunning() && !isFinished()) {
            errCode = -1;
            initCurrentRetryCount();
            if (UploadManagerImpl.getInstance().shouldMissionWaiting()) {
                waiting();
                return;
            }

            uploadedBytes = 0;

            UploadManagerImpl.increaseDownloadingCount();

            missionStatus = MissionStatus.RUNNING;

            threadPoolExecutor.submit(new UploadRunnable(this, 0));
            writeMissionInfo();
            notifyStatus(MissionStatus.START);
            handler.post(progressRunnable);
        }
    }

    public void pause() {
        initCurrentRetryCount();
        handler.removeCallbacks(progressRunnable);
        if (isRunning() || isWaiting()) {
            missionStatus = MissionStatus.PAUSE;
            writeMissionInfo();
            notifyStatus(missionStatus);

            if (missionStatus != MissionStatus.WAITING) {
                UploadManagerImpl.decreaseDownloadingCount();
            }

            if (uploadMissionConfig.getEnableNotificatio()) {
                NotifyUtil.with(getContext())
                        .buildProgressNotify()
                        .setProgressAndFormat(getProgress(), false, "")
                        .setId(getNotifyId())
                        .setContentTitle("已暂停：" + fileName)
                        .show();
            }
        }
    }

    public void waiting() {
        missionStatus = MissionStatus.WAITING;
        notifyStatus(missionStatus);
        pause();
    }

    public void delete() {
        deleteMissionInfo();
    }

    public void openFile(Context context) {
        File file = getFile();
        if (file.exists()) {
            FileUtil.openFile(context, getFile());
        } else {
            Toast.makeText(context, "下载文件不存在!", Toast.LENGTH_SHORT).show();
        }
    }

    //------------------------------------------------------------notify------------------------------------------------------------
    synchronized void notifyProgress(long deltaLen) {
        uploadedBytes += deltaLen;
        Log.d(TAG, "uploadedBytes=" + uploadedBytes + "   deltaLen" + deltaLen);
        if (uploadedBytes > totalBytes) {
            uploadedBytes = totalBytes;
        }
    }

    synchronized void notifyFinished() {
        Log.d(TAG, "notifyFinished errCode=" + errCode + " uploadedBytes=" + uploadedBytes + " totalBytes=" + totalBytes);
        if (errCode > 0) {
            return;
        }

        if (uploadedBytes == totalBytes) {
            onFinish();
        } else {
            pause();
            start();
        }
    }

    synchronized void notifyError(int err) {
        Log.d(TAG, "err=" +err);
        if (!(err == ErrorCode.ERROR_WITHOUT_STORAGE_PERMISSIONS || err == ErrorCode.ERROR_FILE_NOT_FOUND)) {
            currentRetryCount--;
            if (currentRetryCount >= 0) {
                pause();
                notifyStatus(MissionStatus.RETRY);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        start();
                    }
                }, uploadMissionConfig.getRetryDelay());
                return;
            }
        }

        missionStatus = MissionStatus.ERROR;

        currentRetryCount = uploadMissionConfig.getRetryCount();

        errCode = err;

        Log.d("eeeeeeeeeeeeeeeeeeee", "error:" + errCode);

        writeMissionInfo();

        notifyStatus(missionStatus);

        UploadManagerImpl.decreaseDownloadingCount();

        if (uploadMissionConfig.getEnableNotificatio()) {
            NotifyUtil.with(getContext())
                    .buildNotify()
                    .setContentTitle("下载出错" + errCode + ":" + fileName)
                    .setId(getNotifyId())
                    .show();
        }
    }

    private void notifyStatus(final MissionStatus status) {
        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            if (listener != null) {
                MissionListener.HANDLER_STORE.get(listener).post(new Runnable() {
                    @Override
                    public void run() {
                        switch (status) {
                            case INITING:
                                listener.onInit();
                                break;
                            case START:
                                listener.onStart();
                                break;
                            case RUNNING:
                                listener.onProgress(updateInfo);
                                break;
                            case WAITING:
                                listener.onWaiting();
                                break;
                            case PAUSE:
                                listener.onPause();
                                break;
                            case ERROR:
                                listener.onError(errCode);
                                break;
                            case RETRY:
                                listener.onRetry();
                                break;
                            case FINISHED:
                                updateInfo.setDone(getUploadedBytes());
                                updateInfo.setSize(getTotalBytes());
                                updateInfo.setProgress(100);
                                updateInfo.setFileSizeStr(getFileSizeStr());
                                updateInfo.setDownloadedSizeStr(getDownloadedSizeStr());
                                updateInfo.setProgressStr(String.format(Locale.US, "%.2f%%", getProgress()));
                                updateInfo.setSpeedStr(tempSpeed);
                                listener.onProgress(updateInfo);
                                listener.onFinish();
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        }
    }

    private void onFinish() {
        if (errCode > 0) {
            return;
        }
        Log.d(TAG, "onFinish");
        handler.removeCallbacks(progressRunnable);

        missionStatus = MissionStatus.FINISHED;

        writeMissionInfo();

        notifyStatus(missionStatus);

        UploadManagerImpl.decreaseDownloadingCount();

        if (uploadMissionConfig.getEnableNotificatio()) {
            NotifyUtil.with(getContext())
                    .buildNotify()
                    .setContentTitle(fileName)
                    .setContentText("下载已完成")
                    .setId(getNotifyId())
                    .show();
        }
        if (UploadManagerImpl.getInstance().getDownloadManagerListener() != null) {
            UploadManagerImpl.getInstance().getDownloadManagerListener().onMissionFinished();
        }
    }

    public synchronized void addListener(MissionListener listener) {
        if (listener == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        MissionListener.HANDLER_STORE.put(listener, handler);
        mListeners.add(new WeakReference<>(listener));
    }

    public synchronized void removeListener(MissionListener listener) {
        for (Iterator<WeakReference<MissionListener>> iterator = mListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<MissionListener> weakRef = iterator.next();
            if (listener != null && listener == weakRef.get()) {
                iterator.remove();
            }
        }
    }

    public synchronized void removeAllListener() {
        for (Iterator<WeakReference<MissionListener>> iterator = mListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<MissionListener> weakRef = iterator.next();
            iterator.remove();
        }
    }

    public void writeMissionInfo() {
        Log.d(TAG, "writeMissionInfo mWritingToFile=" + mWritingToFile);
        if (!mWritingToFile) {
            mWritingToFile = true;
            if (threadPoolExecutor == null) {
                threadPoolExecutor = ThreadPoolFactory.newFixedThreadPool(uploadMissionConfig.getThreadPoolConfig());
            }
            threadPoolExecutor.submit(writeMissionInfoRunnable);
        }
    }

    public void deleteMissionInfo() {
        File file = new File(getMissionInfoFilePath());
        if (file.exists()) {
            file.delete();
        }
    }

    private void initCurrentRetryCount() {
        if (currentRetryCount != uploadMissionConfig.getRetryCount()) {
            currentRetryCount = uploadMissionConfig.getRetryCount();
        }
    }

    //--------------------------------------------------------------getter-----------------------------------------------
    private Context getContext() {
        return UploadManagerImpl.getInstance().getContext();
    }

    public String getUuid() {
        return uuid;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getUploadedBytes() {
        return uploadedBytes;
    }

    public MissionStatus getStatus() {
        return missionStatus;
    }

    public int getErrCode() {
        return errCode;
    }

    public UploadMissionConfig getUploadMissionConfig() {
        return uploadMissionConfig;
    }

    public String getFilePath() {
        return filePath;
    }

    public File getFile() {
        return new File(filePath);
    }

    public String getMimeType() {
        return FileUtil.getMIMEType(getFile());
    }

    public String getFileSuffix() {
        return MimeTypeMap.getFileExtensionFromUrl(getFile().toURI().toString()).toLowerCase(Locale.US);
    }

    public String getHttpParamName() {
        return uploadMissionConfig.getHttpParamName();
    }

    public String getContentType() {
        return uploadMissionConfig.getContentType();
    }

    public String getUserAgent() {
        return uploadMissionConfig.getUserAgent();
    }

    public String getReferer() {
        String referer = uploadMissionConfig.getReferer();
        if (TextUtils.isEmpty(referer)) {
            referer = url;
        }
        return referer;
    }

    public String getCookie() {
        return uploadMissionConfig.getCookie();
    }

    public int getBlockSize() {
        return uploadMissionConfig.getBlockSize();
    }

    public int getConnectOutTime() {
        return uploadMissionConfig.getConnectOutTime() * 10;
    }

    public String getBoundary() {
        return uploadMissionConfig.getBoundarySignature() + System.nanoTime();
    }

    public int getReadOutTime() {
        return uploadMissionConfig.getConnectOutTime() * 10;
    }

    Map<String, String> getHeaders() {
        return uploadMissionConfig.getHeaders();
    }

    private float getProgress(long done, long length) {
        if (missionStatus == MissionStatus.FINISHED) {
            return 100f;
        } else if (length <= 0) {
            return 0f;
        }
        float progress = (float) done / (float) length;
        return progress * 100f;
    }

    public float getProgress() {
        return getProgress(uploadedBytes, totalBytes);
    }

    public String getProgressStr() {
        return String.format(Locale.US, "%.2f%%", getProgress());
    }

    public String getFileSizeStr() {
        return Utility.formatSize(totalBytes);
    }

    public String getDownloadedSizeStr() {
        return Utility.formatSize(uploadedBytes);
    }

    public String getSpeed() {
        return tempSpeed;
    }

    private int getNotifyId() {
        if (notifyId == 0) {
            notifyId = (int) (createTime / 10000) + (int) (createTime % 10000) * 100000;
        }
        return notifyId;
    }

    public String getMissionInfoFilePath() {
        return UploadManagerImpl.TASK_PATH + File.separator + uuid + UploadManagerImpl.MISSION_INFO_FILE_SUFFIX_NAME;
    }


    //-----------------------------------------------------setter-----------------------------------------------------------------


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    void setUrl(String url) {
        this.url = url;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public class UpdateInfo {

        private long size;
        private long done;
        private float progress;
        private String fileSizeStr;
        private String downloadedSizeStr;
        private String progressStr;
        private String speedStr;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getDone() {
            return done;
        }

        public void setDone(long done) {
            this.done = done;
        }

        public float getProgress() {
            return progress;
        }

        void setProgress(float progress) {
            this.progress = progress;
        }

        public String getFileSizeStr() {
            return fileSizeStr;
        }

        void setFileSizeStr(String fileSizeStr) {
            this.fileSizeStr = fileSizeStr;
        }

        public String getDownloadedSizeStr() {
            return downloadedSizeStr;
        }

        void setDownloadedSizeStr(String downloadedSizeStr) {
            this.downloadedSizeStr = downloadedSizeStr;
        }

        public String getProgressStr() {
            return progressStr;
        }

        void setProgressStr(String progressStr) {
            this.progressStr = progressStr;
        }

        public String getSpeedStr() {
            return speedStr;
        }

        void setSpeedStr(String speedStr) {
            this.speedStr = speedStr;
        }
    }

}
