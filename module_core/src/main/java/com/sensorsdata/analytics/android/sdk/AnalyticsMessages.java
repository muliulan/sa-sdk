/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.DebugModeException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.exceptions.ResponseErrorException;
import com.sensorsdata.analytics.android.sdk.pop.FileUtils;
import com.sensorsdata.analytics.android.sdk.pop.HttpDataBean;
import com.sensorsdata.analytics.android.sdk.pop.HttpNetWork;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Manage communication of events with the internal database and the SensorsData servers.
 * This class straddles the thread boundary between user threads and
 * a logical SensorsData thread.
 */
class AnalyticsMessages {
    private static final String TAG = "SA.AnalyticsMessages";
    private static final int FLUSH_QUEUE = 3;
    private static final int DELETE_ALL = 4;
    private static final int FLUSH_SCHEDULE = 5;
    private static final Map<Context, AnalyticsMessages> S_INSTANCES = new HashMap<>();
    private final Worker mWorker;
    private final Context mContext;
    private final DbAdapter mDbAdapter;
    private SensorsDataAPI mSensorsDataAPI;
    private HttpNetWork mHttpNetWork;

    /**
     * 不要直接调用，通过 getInstance 方法获取实例
     */
    private AnalyticsMessages(final Context context, SensorsDataAPI sensorsDataAPI) {
        mContext = context;
        mDbAdapter = DbAdapter.getInstance();
        mWorker = new Worker();
        mSensorsDataAPI = sensorsDataAPI;
        mHttpNetWork = new HttpNetWork(context, mSensorsDataAPI);
    }

    /**
     * 获取 AnalyticsMessages 对象
     *
     * @param messageContext Context
     */
    public static AnalyticsMessages getInstance(final Context messageContext, final SensorsDataAPI sensorsDataAPI) {
        synchronized (S_INSTANCES) {
            final Context appContext = messageContext.getApplicationContext();
            final AnalyticsMessages ret;
            if (!S_INSTANCES.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext, sensorsDataAPI);
                S_INSTANCES.put(appContext, ret);
            } else {
                ret = S_INSTANCES.get(appContext);
            }
            return ret;
        }
    }


    void enqueueEventMessage(final String type, final JSONObject eventJson) {
        try {
            synchronized (mDbAdapter) {
                int ret = mDbAdapter.addJSON(eventJson);
                if (ret < 0) {
                    String error = "Failed to enqueue the event: " + eventJson;
                    if (mSensorsDataAPI.isDebugMode()) {
                        throw new DebugModeException(error);
                    } else {
                        SALog.i(TAG, error);
                    }
                }

                final Message m = Message.obtain();
                m.what = FLUSH_QUEUE;

                if (mSensorsDataAPI.isDebugMode() || ret ==
                        DbParams.DB_OUT_OF_MEMORY_ERROR) {
                    SALog.i("mll-sa", "handler 立即发消息");
                    mWorker.runMessage(m);
                } else {
                    // track_signup 立即发送
                    if (type.equals("track_signup") || ret > mSensorsDataAPI
                            .getFlushBulkSize()) {
                        SALog.i("mll-sa", "handler 立即发消息");
                        mWorker.runMessage(m);
                    } else {
                        final int interval = mSensorsDataAPI.getFlushInterval();
                        SALog.i("mll-sa", "handler 延迟" + interval / 1000 + " 发消息");
                        mWorker.runMessageOnce(m, interval);
                    }
                }
            }
        } catch (Exception e) {
            SALog.i(TAG, "enqueueEventMessage error:" + e);
        }
    }

    void flush() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_QUEUE;

            mWorker.runMessage(m);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void flushScheduled() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_SCHEDULE;

            mWorker.runMessageOnce(m, mSensorsDataAPI.getFlushInterval());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void deleteAll() {
        try {
            final Message m = Message.obtain();
            m.what = DELETE_ALL;

            mWorker.runMessage(m);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void sendData() {
        try {
            if (!mSensorsDataAPI.isNetworkRequestEnable()) {
                SALog.i(TAG, "NetworkRequest 已关闭，不发送数据！");
                return;
            }

//            if (TextUtils.isEmpty(mSensorsDataAPI.getServerUrl())) {
//                SALog.i(TAG, "Server url is null or empty.");
//                return;
//            }

            //无网络
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return;
            }

            //不符合同步数据的网络策略
            String networkType = NetworkUtils.networkType(mContext);
            if (!NetworkUtils.isShouldFlush(networkType, mSensorsDataAPI.getFlushNetworkPolicy())) {
                SALog.i(TAG, String.format("您当前网络为 %s，无法发送数据，请确认您的网络发送策略！", networkType));
                return;
            }

            // 如果开启多进程上报
            if (mSensorsDataAPI.getConfigOptions().isMultiProcessFlush()) {
                // 已经有进程在上报
                if (DbAdapter.getInstance().isSubProcessFlushing()) {
                    return;
                }
                DbAdapter.getInstance().commitSubProcessFlushState(true);
            } else if (!SensorsDataAPI.mIsMainProcess) {//不是主进程
                return;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return;
        }
        int count = 100;
        while (count > 0) {
            String[] eventsData;
            synchronized (mDbAdapter) {
                if (mSensorsDataAPI.isDebugMode()) {
                    /* debug 模式下服务器只允许接收 1 条数据 */
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 1);
                } else {
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 50);
                }
            }

            if (eventsData == null) {
                DbAdapter.getInstance().commitSubProcessFlushState(false);
                return;
            }

            final String lastId = eventsData[0];
            final String rawMessage = eventsData[1];
            final String gzip = eventsData[2];
            String errorMessage = null;

            try {
                SALog.i("mll-sa", "开始请求");
                sendHttp(gzip, rawMessage);

            } catch (Exception e) {
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                boolean isDebugMode = mSensorsDataAPI.isDebugMode();
                if (!TextUtils.isEmpty(errorMessage)) {
                    if (isDebugMode || SALog.isLogEnabled()) {
                        SALog.i(TAG, errorMessage);
                        if (isDebugMode && SensorsDataAPI.SHOW_DEBUG_INFO_VIEW) {
                            SensorsDataDialogUtils.showHttpErrorDialog(AppStateManager.getInstance().getForegroundActivity(), errorMessage);
                        }
                    }
                }
                count = mDbAdapter.cleanupEvents(lastId);
                SALog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
            }
        }
        if (mSensorsDataAPI.getConfigOptions().isMultiProcessFlush()) {
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }
    }

    /**
     * 数据动态分发
     */
    private void sendHttp(String gzip, String rawMessage) throws ConnectErrorException, ResponseErrorException, InvalidDataException {
        SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
        ArrayList<SAConfigOptions.NetWork> customNetWork = configOptions.getCustomNetWork();

        for (SAConfigOptions.NetWork netWork : customNetWork) {
            if (netWork == null) {
                return;
            }
            String url = netWork.getUrl();
            HttpDataBean httpDataBean = netWork.getNewData(mergeData(url, rawMessage));
            httpDataBean.setUrl(url);

            if (mHttpNetWork != null) {
                mHttpNetWork.sendHttpRequest(httpDataBean, gzip, false);
            }
        }
    }

    private String mergeData(String url, String rawMessage) {
        String filePath = FileUtils.getCachePath(mContext, url);
        String cacheData = FileUtils.getText(filePath);
        if (TextUtils.isEmpty(cacheData)) {
            FileUtils.sendText(rawMessage, filePath);
            return rawMessage;
        }

        try {
            JSONArray cache = new JSONArray(cacheData);
            JSONArray raw = new JSONArray(rawMessage);
            for (int index = 0; index < raw.length(); index++) {
                cache.put(raw.get(index));
            }
            String mergeData = cache.toString();
            FileUtils.sendText(mergeData, filePath);
            return mergeData;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rawMessage;
    }

    /**
     * 在服务器正常返回状态码的情况下，目前只有 (>= 500 && < 600) || 404 || 403 才不删数据
     *
     * @param httpCode 状态码
     * @return true: 删除数据，false: 不删数据
     */
    private boolean isDeleteEventsByCode(int httpCode) {
        boolean shouldDelete = true;
        if (httpCode == HttpURLConnection.HTTP_NOT_FOUND ||
                httpCode == HttpURLConnection.HTTP_FORBIDDEN ||
                (httpCode >= HttpURLConnection.HTTP_INTERNAL_ERROR && httpCode < 600)) {
            shouldDelete = false;
        }
        return shouldDelete;
    }


    // Worker will manage the (at most single) IO thread associated with
    // this AnalyticsMessages instance.
    // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
    private class Worker {

        private final Object mHandlerLock = new Object();
        private Handler mHandler;

        Worker() {
            final HandlerThread thread =
                    new HandlerThread("com.sensorsdata.analytics.android.sdk.AnalyticsMessages.Worker",
                            Thread.MIN_PRIORITY);
            thread.start();
            mHandler = new AnalyticsMessageHandler(thread.getLooper());
        }

        void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    SALog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    SALog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
                        mHandler.sendMessageDelayed(msg, delay);
                    }
                }
            }
        }

        private class AnalyticsMessageHandler extends Handler {

            AnalyticsMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    SALog.i("mll-sa", "handler接受消息 " + msg.what);
                    if (msg.what == FLUSH_QUEUE) {
                        sendData();
                    } else if (msg.what == DELETE_ALL) {
                        try {
                            mDbAdapter.deleteAllEvents();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    } else if (msg.what == FLUSH_SCHEDULE) {
                        flushScheduled();
                        sendData();
                    } else {
                        SALog.i(TAG, "Unexpected message received by SensorsData worker: " + msg);
                    }
                } catch (final RuntimeException e) {
                    SALog.i(TAG, "Worker threw an unhandled exception", e);
                }
            }
        }
    }
}