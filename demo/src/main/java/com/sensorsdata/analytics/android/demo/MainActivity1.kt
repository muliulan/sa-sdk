/*
 * Created by wangzhuohou on 2019/04/17.
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

import android.app.Activity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.sensorsdata.analytics.android.demo.databinding.ActivityMainBinding
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter

class MainActivity1 : Activity() {

    var instance: DbAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        val url = "http://baidu.com"
        binding.button1.setOnClickListener {
            instance = DbAdapter.getInstance()
        }

        binding.button2.setOnClickListener {
            instance?.addCache(url, "a:1,b:${System.currentTimeMillis()}")
        }
        binding.button3.setOnClickListener {
            binding.textView.text = instance?.queryCache(url)
        }
        binding.button4.setOnClickListener {
            instance?.updateCache(url, System.currentTimeMillis().toString())
        }

    }


}