package com.sensorsdata.analytics.android.sdk.pop;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.exceptions.ResponseErrorException;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author : zhaoCS
 * date    : 2022/7/12 7:48 下午
 * desc    :
 */
public class HttpNetWork {

    private static final String TAG = "SA.AnalyticsMessages";
    private SensorsDataAPI mSensorsDataAPI;
    private Context mContext;
    private final DbAdapter mDbAdapter;
    private HttpState mHttpState;

    public HttpNetWork(Context context, SensorsDataAPI sensorsDataAPI, DbAdapter dbAdapter, HttpState httpState) {
        this.mSensorsDataAPI = sensorsDataAPI;
        this.mContext = context;
        this.mDbAdapter = dbAdapter;
        this.mHttpState = httpState;
    }

    public void sendHttpRequest(HttpDataBean httpDataBean, String gzip, boolean isRedirects) throws ConnectErrorException, ResponseErrorException, InvalidDataException {
        SALog.i("mll-sa", "网络请求参数 原始数据" + httpDataBean.getJson());

        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        PrintWriter bout = null;
        try {
            final URL url = new URL(httpDataBean.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                SALog.i(TAG, String.format("can not connect %s, it shouldn't happen", url), null);
                return;
            }
            setHead(httpDataBean, connection);

            String query = getQuery(httpDataBean, gzip, connection);
            connectionConfig(connection, query, httpDataBean);

            out = connection.getOutputStream();
            bout = new PrintWriter(out);
            bout.write(query);
            bout.flush();

            int responseCode = connection.getResponseCode();
            SALog.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200) {
                //请求成功删除 数据库数据
                mDbAdapter.deleteCache(httpDataBean.getUrl());
            }
            if (!isRedirects && NetworkUtils.needRedirects(responseCode)) {
                String location = NetworkUtils.getLocation(connection, httpDataBean.getUrl());

                if (!TextUtils.isEmpty(location)) {
                    closeStream(bout, out, null, connection);
                    sendHttpRequest(httpDataBean, gzip, true);
                    return;
                }
            }
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            in.close();
            in = null;

            String response = new String(responseBody, CHARSET_UTF8);
            if (SALog.isLogEnabled()) {
                String jsonMessage = JSONUtils.formatJson(httpDataBean.getJson());
                // 状态码 200 - 300 间都认为正确
                if (responseCode >= HttpURLConnection.HTTP_OK &&
                        responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {

                    SALog.i(TAG, "valid message: \n" + jsonMessage);
                } else {
                    SALog.i(TAG, "invalid message: \n" + jsonMessage);
                    SALog.i(TAG, String.format(Locale.CHINA, "ret_code: %d", responseCode));
                    SALog.i(TAG, String.format(Locale.CHINA, "ret_content: %s", response));
                }
            }
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误
                throw new ResponseErrorException(String.format("flush failure with response '%s', the response code is '%d'",
                        response, responseCode), responseCode);
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    private void connectionConfig(HttpURLConnection connection, String query, HttpDataBean httpDataBean) throws ProtocolException {
        SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
        if (configOptions.mSSLSocketFactory != null && connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(configOptions.mSSLSocketFactory);
        }
        connection.setInstanceFollowRedirects(false);
        connection.setFixedLengthStreamingMode(query.length());
        connection.setDoOutput(true);
        connection.setRequestMethod(httpDataBean.getRequestMethod().name());
        connection.setConnectTimeout(30 * 1000);
        connection.setReadTimeout(30 * 1000);
    }

    private String getQuery(HttpDataBean httpDataBean, String gzip, HttpURLConnection connection) throws UnsupportedEncodingException, InvalidDataException {
        String query;
        if (httpDataBean.isSa()) {
            query = saData(connection, httpDataBean.getJson(), gzip);
        } else {
            query = httpDataBean.getJson();
        }
        return query;
    }

    private void setHead(HttpDataBean httpDataBean, HttpURLConnection connection) {
        HashMap<String, Object> head = httpDataBean.getHead();
        if (head == null || head.size() == 0) {
            return;
        }
        for (Map.Entry<String, Object> item : head.entrySet()) {
            connection.setRequestProperty(item.getKey(), item.getValue().toString());
        }
    }

    /**
     * 神策的数据
     */
    private String saData(HttpURLConnection connection, String rawMessage, String gzip) throws UnsupportedEncodingException, InvalidDataException {
        if (mSensorsDataAPI.getDebugMode() == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
            connection.addRequestProperty("Dry-Run", "true");
        }

        String cookie = mSensorsDataAPI.getCookie(false);
        if (!TextUtils.isEmpty(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        }

        Uri.Builder builder = new Uri.Builder();

        String data = null;
        if (DbParams.GZIP_DATA_EVENT.equals(gzip)) {
            data = encodeData(rawMessage);
        }

        //先校验crc
        if (!TextUtils.isEmpty(data)) {
            builder.appendQueryParameter("crc", String.valueOf(data.hashCode()));
        }

        builder.appendQueryParameter("gzip", gzip);
        builder.appendQueryParameter("data_list", data);

        return builder.build().getEncodedQuery();
    }

    private void closeStream(PrintWriter bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }
    }

    private static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    private String encodeData(final String rawMessage) throws InvalidDataException {
        GZIPOutputStream gos = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes(CHARSET_UTF8).length);
            gos = new GZIPOutputStream(os);
            gos.write(rawMessage.getBytes(CHARSET_UTF8));
            gos.close();
            byte[] compressed = os.toByteArray();
            os.close();
            return new String(Base64Coder.encode(compressed));
        } catch (IOException exception) {
            // 格式错误，直接将数据删除
            throw new InvalidDataException(exception);
        } finally {
            if (gos != null) {
                try {
                    gos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public interface HttpState {
        void succeed();

        void error(Exception e);
    }

}
