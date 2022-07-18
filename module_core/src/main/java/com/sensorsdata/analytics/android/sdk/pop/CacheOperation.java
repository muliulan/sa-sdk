package com.sensorsdata.analytics.android.sdk.pop;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

/**
 * @author : zhaoCS
 * date    : 2022/7/18 11:43 上午
 * desc    :
 */
public class CacheOperation {

    private final ContentResolver contentResolver;

    public CacheOperation(Context context) {
        contentResolver = context.getContentResolver();
    }

    public void insertData(Uri uri, String url, String json) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(json)) {
            return;
        }
        String query = query(uri, url);
        if (!TextUtils.isEmpty(query)) {
            update(uri, url, json);
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.POP_KEY_URL, url);
        contentValues.put(DbParams.POP_KEY_JSON, json);
        contentResolver.insert(uri, contentValues);
    }

    public void delete(Uri uri, String url) {
        contentResolver.delete(uri, DbParams.POP_KEY_URL + "=?", new String[]{url});
    }

    public String query(Uri uri, String url) {
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String urlCursor = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                if (TextUtils.equals(urlCursor, url)) {
                    return cursor.getString(cursor.getColumnIndexOrThrow("json"));
                }
            }
            cursor.close();
        }
        return "";
    }

    public void update(Uri uri, String url, String json) {
        ContentValues values = new ContentValues();
        values.put(DbParams.POP_KEY_URL, url);
        values.put(DbParams.POP_KEY_JSON, json);
        contentResolver.update(uri, values, DbParams.POP_KEY_URL + "=?", new String[]{url});
    }
}
