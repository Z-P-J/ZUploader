# ZUploader
 使用自己开发的网络框架[ZHttp](https://github.com/Z-P-J/ZHttp)实现的文件上传框架（完善中）

## 使用
```java
UploadMissionConfig config = UploadMissionConfig.with()
                .setHttpParamName("name")
                .setCookie("Your cookie")
                .setUserAgent("your User-Agent")
                .setReferer("Your referer")
                .setUseUtf8(true)
                .addHeader("Content-Transfer-Encoding", "binary")
                .addHeader("Charset", "UTF-8")
                .setRetryCount(3);

ZUploader.with(path)
                .setServerUrl("上传服务器url")
                .setMissionConfig(config)
                .setMissionListener(new UploadMission.MissionListener() {
                    @Override
                    public void onInit() {
                        Log.i(TAG, "onInit");
                    }

                    @Override
                    public void onStart() {
                        Log.i(TAG, "onStart");
                    }

                    @Override
                    public void onPause() {
                        Log.i(TAG, "onPause");
                    }

                    @Override
                    public void onWaiting() {
                        Log.i(TAG, "onWaiting");
                    }

                    @Override
                    public void onRetry() {
                        Log.i(TAG, "onRetry");
                    }

                    @Override
                    public void onProgress(UploadMission.UpdateInfo update) {
                        Log.i(TAG, "onProgress");
                    }

                    @Override
                    public void onFinish() {
                        Log.i(TAG, "success upload!");
                    }

                    @Override
                    public void onError(int errCode) {
                        Log.e(TAG, "failure upload!     errCode=" + errCode);
                    }
                })
                .start();
```
