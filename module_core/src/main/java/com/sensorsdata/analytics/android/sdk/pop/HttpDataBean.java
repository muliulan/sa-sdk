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
    private RequestMethod mRequestMethod = HttpDataBean.RequestMethod.POST;
    /**
     * 是否是神策
     */
    private boolean mIsSa = false;
    /**
     * 请求头参数
     */
    private HashMap<String, Object> head;
    /**
     * 修改后的数据结构
     */
    private String json;

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public void setSa(boolean isSa) {
        this.mIsSa = isSa;
    }

    public boolean isSa() {
        return mIsSa;
    }

    public RequestMethod getRequestMethod() {
        return mRequestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.mRequestMethod = requestMethod;
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


