package com.yuriy.openradio.model.storage.cache.api;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4.class)
public class PersistentApiCacheTest {

    private static final String DB_NAME = "ApiTestDb";
    private ApiCache mApiCache;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        mApiCache = new PersistentApiCache(context, DB_NAME);
        mApiCache.clear();
    }

    @After
    public void tearDown() {
        mApiCache.clear();
    }

    @Test
    public void putAndGetData() throws Exception {
        JSONArray in1 = getJsonArray(1);
        JSONArray in2 = getJsonArray(2);

        String key1 = "1234567890";
        String key2 = "0987654321";

        mApiCache.put(key1, in1);
        mApiCache.put(key2, in2);

        JSONArray out1 = mApiCache.get(key1);
        JSONArray out2 = mApiCache.get(key2);

        assertThat(out1.toString(), is(in1.toString()));
        assertThat(out2.toString(), is(in2.toString()));
    }

    @Test
    public void deleteRaw() throws Exception {
        JSONArray in1 = getJsonArray(1);
        JSONArray in2 = getJsonArray(2);

        String key1 = "1234567890";
        String key2 = "0987654321";

        mApiCache.put(key1, in1);
        mApiCache.put(key2, in2);

        mApiCache.remove(key1);
    }

    @Test
    public void newRecordOverridePrevious() throws Exception {
        JSONArray in1 = getJsonArray(1);
        JSONArray in2 = getJsonArray(2);
        JSONArray in3 = getJsonArray(3);
        String key = "1234567890";

        mApiCache.put(key, in1);
        mApiCache.remove(key);
        mApiCache.put(key, in2);
        mApiCache.remove(key);
        mApiCache.put(key, in3);

        assertThat(((PersistentApiCache)mApiCache).getCount(key), is(1));
        assertThat(mApiCache.get(key).toString().equals(in3.toString()), is(true));
    }

    private JSONArray getJsonArray(int factor) throws Exception {
        JSONObject student1 = new JSONObject();
        student1.put("id", "3" + factor);
        student1.put("name", "NAME OF STUDENT");
        student1.put("year", "3rd");
        student1.put("curriculum", "Arts" + factor);
        student1.put("birthday", "5/5/1993");

        JSONObject student2 = new JSONObject();
        student2.put("id", "2" + factor);
        student2.put("name", "NAME OF STUDENT2");
        student2.put("year", "4rd");
        student2.put("curriculum", "Science" + factor);
        student2.put("birthday", "5/5/1993");

        JSONArray jsonArray = new JSONArray();

        jsonArray.put(student1);
        jsonArray.put(student2);

        return jsonArray;
    }
}
