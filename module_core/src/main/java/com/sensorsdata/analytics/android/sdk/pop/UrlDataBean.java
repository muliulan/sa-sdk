package com.sensorsdata.analytics.android.sdk.pop;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;

/**
 * @author : zhaoCS
 * date    : 2022/8/8 3:12 下午
 * desc    :
 */
public class UrlDataBean {

    private SAConfigOptions.NetWork netWork;
    private String rawMessage;
    private String gzip;


    public UrlDataBean(SAConfigOptions.NetWork netWork, String rawMessage, String gzip) {
        this.netWork = netWork;
        this.rawMessage = rawMessage;
        this.gzip = gzip;
    }

    public SAConfigOptions.NetWork getNetWork() {
        return netWork;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getGzip() {
        return gzip;
    }
}
