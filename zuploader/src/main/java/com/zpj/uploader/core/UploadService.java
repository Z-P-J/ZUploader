package com.zpj.uploader.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class UploadService extends Service implements UploadMission.MissionListener {

    private static final String TAG = UploadService.class.getSimpleName();

    private DMBinder mBinder;
    private UploadManager mManager;
    private Notification mNotification;
    private Handler mHandler;
    private long mLastTimeStamp = System.currentTimeMillis();

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.setReferenceCounted(false);
        if (!wakeLock.isHeld())
            wakeLock.acquire();

        mBinder = new DMBinder();
        if (mManager == null) {
            //下载地址
//			String path = Settings.DEFAULT_PATH;
//			File file = new File(path);
//			if (!file.exists()) {
//				file.mkdirs();
//			}
            mManager = UploadManagerImpl.getInstance();
            Log.d(TAG, "mManager == null");
//			Log.d(TAG, "Download directory: " + path);
        }

        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
//		i.setClass(this, Main2Activity.class);
        mNotification = new Notification.Builder(this)
                .setContentIntent(PendingIntent.getActivity(this, 0, i, 0))
                .setContentTitle("下载中")
                .setContentText("内容")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .build();

        HandlerThread thread = new HandlerThread("ServiceMessenger");
        thread.start();

        mHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    int runningCount = 0;

                    for (int i = 0; i < mManager.getCount(); i++) {
                        if (mManager.getMission(i).isRunning()) {
                            runningCount++;
                        }
                    }

                    updateState(runningCount);
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying");
        mManager.pauseAllMissions();
//		for (int i = 0; i < mManager.getCount(); i++) {
//			mManager.pauseMission(i);
//		}

        stopForeground(true);

        if (wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onInit() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onWaiting() {

    }

    @Override
    public void onRetry() {

    }

    @Override
    public void onProgress(UploadMission.UpdateInfo update) {
        postUpdateMessage();
    }

    @Override
    public void onFinish() {
        postUpdateMessage();
    }

    @Override
    public void onError(int errCode) {
        postUpdateMessage();
    }

    private void postUpdateMessage() {
        mHandler.sendEmptyMessage(0);
    }

    private void updateState(int runningCount) {
        if (runningCount == 0) {
            stopForeground(true);
        } else {
//			startForeground(1000, null);
            startForeground(1000, mNotification);
        }
    }


    // Wrapper of UploadManager
    public class DMBinder extends Binder {
        public UploadManager getDownloadManager() {
            return mManager;
        }

        public void onMissionAdded(UploadMission mission) {
            mission.addListener(UploadService.this);
            mission.setErrCode(-1);
            postUpdateMessage();
        }

        public void onMissionRemoved(UploadMission mission) {
            mission.removeListener(UploadService.this);
            postUpdateMessage();
        }

    }

}
