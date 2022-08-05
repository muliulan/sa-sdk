/*
 * Created by wangzhuozhou on 2016/11/12.
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
package com.sensorsdata.analytics.android.demo

import android.app.Application
import com.sensorsdata.analytics.android.sdk.SAConfigOptions
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI
import com.sensorsdata.analytics.android.sdk.pop.HttpDataBean

class MyApplication : Application() {

    /**
     * Sensors Analytics 采集数据的地址
     */
    private val SA_SERVER_URL = ""

    override fun onCreate() {
        super.onCreate()
        initSensorsDataAPI()
    }


    /**
     * 初始化 Sensors Analytics SDK
     */
    private fun initSensorsDataAPI() {
        val configOptions = SAConfigOptions(SA_SERVER_URL)
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(
            SensorsAnalyticsAutoTrackEventType.APP_START or
                    SensorsAnalyticsAutoTrackEventType.APP_END or
                    SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN or
                    SensorsAnalyticsAutoTrackEventType.APP_CLICK
        )
        // 打开 crash 信息采集
        configOptions.enableTrackAppCrash()
        configOptions.setCustomNetWorkListener(arrayListOf(object : SAConfigOptions.NetWork {
            override fun getUrl(): String = "http://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b"

            override fun getNewData(data: String?): HttpDataBean =
                HttpDataBean().apply {
                    json = data
                    isSa=true
                }

        }))


        //传入 SAConfigOptions 对象，初始化神策 SDK
        SensorsDataAPI.startWithConfigOptions(this, configOptions)
    }

}