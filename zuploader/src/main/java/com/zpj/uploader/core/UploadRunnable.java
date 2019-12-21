package com.zpj.uploader.core;

import android.util.Log;

import com.zpj.http.ZHttp;
import com.zpj.http.core.IHttp;
import com.zpj.uploader.constant.ErrorCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class UploadRunnable implements Runnable{
    private static final String TAG = UploadRunnable.class.getSimpleName();

	private static final String END = "\r\n";
	private static final String LAST = "--";
	private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final int BUFFER_SIZE = 512;

    private boolean shouldContinue = true;

    private final UploadMission mMission;
    private int mId;

    private final byte[] buf = new byte[BUFFER_SIZE];


    UploadRunnable(UploadMission mission, int id) {
        mMission = mission;
        mId = id;
    }

    @Override
    public void run() {
        try {
            ZHttp.post(mMission.getUrl())
                    .cookie(mMission.getCookie())
                    .userAgent(mMission.getUserAgent())
                    .referrer(mMission.getReferer())
                    .data(mMission.getHttpParamName(), mMission.getFileName(), new FileInputStream(mMission.getFile()), new IHttp.OnStreamWriteListener() {
                        @Override
                        public void onBytesWritten(int bytesWritten) {
                            Log.d(TAG, "bytesWritten=" + bytesWritten + "    time=" + System.currentTimeMillis());
                            notifyProgress(bytesWritten);
                        }
                    })
                    .header("Charset", mMission.getUploadMissionConfig().isUseUtf8() ? "UTF-8" : "US-ASCII")
                    .headers(mMission.getUploadMissionConfig().getHeaders())
                    .data(mMission.getUploadMissionConfig().getParameters())
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
            notifyError(ErrorCode.ERROR_IO);
            return;
        }

        Log.d(TAG, "thread " + mId + " exited main loop");

        if (mMission.getErrCode() == -1 && mMission.isRunning() && mMission.getUploadedBytes() == mMission.getTotalBytes()) {
            Log.d(TAG, "no error has happened, notifying");
            notifyFinished();
        }

        if (!mMission.isRunning()) {
            Log.d(TAG, "The mission has been paused. Passing.");
        }
    }

    public void notifyProgress(final int len) {
        Log.d("notifyProgress", "len=" + len);
		synchronized (mMission) {
			mMission.notifyProgress(len);
		}
    }

    private void notifyError(final int err) {
        synchronized (mMission) {
            mMission.notifyError(err);
        }
    }

    private void notifyFinished() {
        synchronized (mMission) {
            mMission.notifyFinished();
        }
    }
}
