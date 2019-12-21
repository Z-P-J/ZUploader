//package com.zpj.uploader.core;
//
//import android.text.TextUtils;
//
//import com.zpj.http.ZHttp;
//import com.zpj.http.core.Connection;
//import com.zpj.uploader.util.ssl.SSLContextUtil;
//
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Map;
//
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSocketFactory;
//
//class HttpUrlConnectionFactory {
//
//    private static final String COOKIE = "Cookie";
//    private static final String USER_AGENT = "User-Agent";
//    private static final String REFERER = "Referer";
//
//    static Connection getConnection(UploadMission mission) throws Exception {
//        URL url = new URL(mission.getUrl());
//        Connection conn = ZHttp.post(mission.getUrl());;
//        wrapConnection(conn, mission);
//        return conn;
//    }
//
//    private static void wrapConnection(Connection conn, UploadMission mission) {
//        if (conn instanceof HttpsURLConnection) {
////			HttpsURLConnection httpsURLConnection = (HttpsURLConnection) conn;
//            SSLContext sslContext =
//                    SSLContextUtil.getSSLContext(UploadManagerImpl.getInstance().getContext(), SSLContextUtil.CA_ALIAS, SSLContextUtil.CA_PATH);
//            if (sslContext == null) {
//                sslContext = SSLContextUtil.getDefaultSLLContext();
//            }
//            SSLSocketFactory ssf = sslContext.getSocketFactory();
//            ((HttpsURLConnection) conn).setSSLSocketFactory(ssf);
//            ((HttpsURLConnection) conn).setHostnameVerifier(SSLContextUtil.HOSTNAME_VERIFIER);
//        }
////        conn.setInstanceFollowRedirects(false);
////        conn.setConnectTimeout(mission.getConnectOutTime());
////        conn.setReadTimeout(mission.getReadOutTime());
//        conn.timeout(mission.getConnectOutTime());
//        if (!TextUtils.isEmpty(mission.getCookie().trim())) {
//            conn.header(COOKIE, mission.getCookie());
//        }
//        conn.header(USER_AGENT, mission.getUserAgent());
////        conn.setRequestProperty("Accept", "*/*");
//        conn.header(REFERER, mission.getUrl());
//        Map<String, String> headers = mission.getHeaders();
//        if (!headers.isEmpty()) {
//            for (String key : headers.keySet()) {
//                conn.header(key, headers.get(key));
//            }
//        }
//    }
//
//}
