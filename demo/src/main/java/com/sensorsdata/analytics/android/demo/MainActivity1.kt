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
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.sensorsdata.analytics.android.demo.databinding.ActivityMainBinding
import com.sensorsdata.analytics.android.sdk.pop.FileUtils

class MainActivity1 : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        val aaa = applicationContext.externalCacheDir.toString() + "/maidian/aaa.txt"
        binding.button.setOnClickListener {
            Log.e("mll", aaa)
            FileUtils.sendText("1111111111111111", aaa)
        }

        binding.lambdaButton.setOnClickListener {

            FileUtils.deleteFiles(aaa)

        }

    }


}