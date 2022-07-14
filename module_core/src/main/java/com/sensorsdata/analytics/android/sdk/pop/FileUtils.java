package com.sensorsdata.analytics.android.sdk.pop;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author : zhaoCS
 * date    : 2022/7/14 2:21 下午
 * desc    :
 */
public class FileUtils {


    public static String getCachePath(Context context, String url) {
        return context.getApplicationContext().getExternalCacheDir().toString() + "/maiDian/" + Uri.parse(url).getHost() + ".txt";
    }

    /**
     * 读文件
     */
    public static String getText(String filePath) {

        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
//                System.lineSeparator()
                sb.append(s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * 取文件
     */
    public static void sendText(String content, String filePath) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(filePath);
            if (!createOrExistsFile(file)) {
                return;
            }
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes("utf-8"));
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件
     * */
    public static void deleteFiles(String filePath){
        File file = new File(filePath);
        if (!file.exists()){
            return;
        }
        if (file.isFile()){
            file.delete();
        }


    }

    private static boolean createOrExistsFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.isFile();
        }
        if (!createOrExistsDir(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }
}
