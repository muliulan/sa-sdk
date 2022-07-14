package com.sensorsdata.analytics.android.sdk.pop;

import java.util.HashMap;

/**
 * @author : zhaoCS
 * date    : 2022/7/12 4:42 下午
 * desc    :
 */
public class HttpDataBean {
    private String mUrl;
    /**
     * 请求类型
     */
    private RequestMethod mRequestMethod;
    /**
     * 是否是神策
     */
    private boolean mIsSa;

    /**
     * 请求头参数(为空就没有)
     */
    private HashMap<String, Object> head;
    /**
     * 修改后的数据结构 (为空就使用原始数据结构)
     */
    private String json;

    public HttpDataBean(RequestMethod requestMethod, boolean isSa) {
        this.mRequestMethod = requestMethod;
        this.mIsSa = isSa;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public RequestMethod getRequestMethod() {
        return mRequestMethod;
    }


    public boolean isSa() {
        return mIsSa;
    }


    public HashMap<String, Object> getHead() {
        return head;
    }

    public void setHead(HashMap<String, Object> head) {
        this.head = head;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public enum RequestMethod {
        GET(),
        POST()
    }

}


