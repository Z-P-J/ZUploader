package com.zpj.uploader.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.zpj.uploader.config.UploadMissionConfig;
import com.zpj.uploader.config.UploaderConfig;
import com.zpj.uploader.config.ThreadPoolConfig;
import com.zpj.uploader.constant.DefaultConstant;
import com.zpj.uploader.util.NetworkChangeReceiver;
import com.zpj.uploader.util.Utility;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Z-P-J
 */
public class UploadManagerImpl implements UploadManager {

	private static final String TAG = UploadManagerImpl.class.getSimpleName();

	private static final String MISSIONS_PATH = "upload_missions";

	static String TASK_PATH;

	static String MISSION_INFO_FILE_SUFFIX_NAME = ".zpj";

	private static UploadManager mManager;
	
	private Context mContext;

	private DownloadManagerListener downloadManagerListener;

	private UploaderConfig options;

	private static AtomicInteger downloadingCount = new AtomicInteger(0);

	private UploadManagerImpl(Context context, UploaderConfig options) {
		mContext = context;
		this.options = options;

		File path = new File(context.getFilesDir(), MISSIONS_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}
		TASK_PATH = path.getPath();
	}

	public static UploadManager getInstance() {
		if (mManager == null) {
			throw new RuntimeException("must register first!");
		}
		return mManager;
	}

	public static void register(UploaderConfig options) {
		if (mManager == null) {
			mManager = new UploadManagerImpl(options.getContext(), options);
			mManager.loadMissions();
		}
	}

	public static void unRegister() {
		getInstance().pauseAllMissions();
		getInstance().getContext().unregisterReceiver(NetworkChangeReceiver.getInstance());
	}

	private static int getDownloadingCount() {
		return downloadingCount.get();
	}

	static void decreaseDownloadingCount() {
		downloadingCount.decrementAndGet();
		for (UploadMission mission : ALL_MISSIONS) {
			if (!mission.isFinished() && mission.isWaiting()) {
				mission.start();
			}
		}
	}

	static void increaseDownloadingCount() {
		downloadingCount.incrementAndGet();
	}

	@Override
	public Context getContext() {
		return mContext;
	}

	@Override
	public UploaderConfig getQianXunConfig() {
		return options;
	}

	@Override
	public ThreadPoolConfig getThreadPoolConfig() {
		return options.getThreadPoolConfig();
	}

	@Override
	public List<UploadMission> getMissions() {
		Collections.sort(ALL_MISSIONS, new Comparator<UploadMission>() {
			@Override
			public int compare(UploadMission o1, UploadMission o2) {
				return - (int) (o1.getCreateTime() - o2.getCreateTime());
			}
		});
		return ALL_MISSIONS;
	}

	@Override
	public void loadMissions() {
		long time1 = System.currentTimeMillis();
		ALL_MISSIONS.clear();
		File f;
		if (TASK_PATH != null) {
			f = new File(TASK_PATH);
		} else {
			f = new File(getContext().getFilesDir(), MISSIONS_PATH);
		}

		if (f.exists() && f.isDirectory()) {
			for (final File sub : f.listFiles()) {
				if (sub.isDirectory()) {
					continue;
				}
				if (sub.getName().endsWith(MISSION_INFO_FILE_SUFFIX_NAME)) {
					String str = Utility.readFromFile(sub.getAbsolutePath());
					if (!TextUtils.isEmpty(str)) {
						UploadMission mis = new Gson().fromJson(str, UploadMission.class);
						Log.d("initMissions", "mis=null? " + (mis == null));
						if (mis != null) {
							mis.init();
							insertMission(mis);
						}
					}
				}
			}
		} else {
			f.mkdirs();
		}

		Collections.sort(ALL_MISSIONS, new Comparator<UploadMission>() {
			@Override
			public int compare(UploadMission o1, UploadMission o2) {
				return - (int) (o1.getCreateTime() - o2.getCreateTime());
			}
		});
		long time2  = System.currentTimeMillis();
		Log.d(TAG, "deltaTime=" + (time2 - time1));
	}

	@Override
	public void setDownloadManagerListener(DownloadManagerListener downloadManagerListener) {
		this.downloadManagerListener = downloadManagerListener;
	}

	@Override
	public DownloadManagerListener getDownloadManagerListener() {
		return downloadManagerListener;
	}

	@Override
	public UploadMission addMission(String url, File file) {
		return addMission(url, file, UploadMissionConfig.with());
	}

	@Override
	public UploadMission addMission(String url, File file, UploadMissionConfig config) {
		UploadMission mission = UploadMission.create(url, file, config);
		insertMission(mission);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionAdd();
		}
		mission.init();
		return mission;
	}

	@Override
	public void resumeMission(int i) {
		UploadMission d = getMission(i);
		d.start();
	}

	@Override
	public void resumeMission(String uuid) {
		UploadMission d = getMission(uuid);
		d.start();
	}

	@Override
	public void resumeAllMissions() {
		for (UploadMission uploadMission : ALL_MISSIONS) {
			uploadMission.start();
		}
	}

	@Override
	public void pauseMission(int i) {
		UploadMission d = getMission(i);
		d.pause();
	}

	@Override
	public void pauseMission(String uuid) {
		UploadMission d = getMission(uuid);
		d.pause();
	}

	@Override
	public void pauseAllMissions() {
		for (UploadMission uploadMission : ALL_MISSIONS) {
			uploadMission.pause();
		}
	}
	
	@Override
	public void deleteMission(int i) {
		UploadMission d = getMission(i);
		d.pause();
		d.delete();
		ALL_MISSIONS.remove(i);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void deleteMission(String uuid) {
		UploadMission d = getMission(uuid);
		d.pause();
		d.delete();
		ALL_MISSIONS.remove(d);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void deleteMission(UploadMission mission) {
		mission.pause();
		mission.delete();
		ALL_MISSIONS.remove(mission);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void deleteAllMissions() {
		for (UploadMission mission : ALL_MISSIONS) {
			mission.pause();
			mission.delete();
		}
		ALL_MISSIONS.clear();
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void clearMission(int i) {
		UploadMission d = getMission(i);
		d.pause();
		d.deleteMissionInfo();
		ALL_MISSIONS.remove(i);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void clearMission(String uuid) {
		UploadMission d = getMission(uuid);
		d.pause();
		d.deleteMissionInfo();
		ALL_MISSIONS.remove(d);
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public void clearAllMissions() {
		for (UploadMission mission : ALL_MISSIONS) {
			mission.pause();
			mission.deleteMissionInfo();
		}
		ALL_MISSIONS.clear();
		if (downloadManagerListener != null) {
			downloadManagerListener.onMissionDelete();
		}
	}

	@Override
	public UploadMission getMission(int i) {
		return ALL_MISSIONS.get(i);
	}

	@Override
	public UploadMission getMission(String uuid) {
		for (UploadMission mission : ALL_MISSIONS) {
			if (TextUtils.equals(mission.getUuid(), uuid)) {
				return mission;
			}
		}
		return null;
	}

	@Override
	public int getCount() {
		return ALL_MISSIONS.size();
	}
	
	private int insertMission(UploadMission mission) {
		ALL_MISSIONS.add(mission);
		return ALL_MISSIONS.size() - 1;
	}

	@Override
	public boolean shouldMissionWaiting() {
		return UploadManagerImpl.getDownloadingCount() >= getQianXunConfig().getConcurrentMissionCount();
	}

}
