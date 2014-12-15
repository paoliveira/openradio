package com.yuriy.openradio.utils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.content.Context;

import com.yuriy.openradio.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * {@link com.yuriy.openradio.utils.ApiKeyLoader} is a helper class to provide
 * Dirble API key
 */
public class ApiKeyLoader {

    /**
     * Load API key from the resources.
     *
     * @param context Context of the application.
     * @return API key.
     */
    public static String getApiKey(final Context context) {
        final InputStream stream = context.getResources().openRawResource(R.raw.api_key);
        final Writer writer = new StringWriter();
        final char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                /* Ignore this exception */
            }
        }
        return writer.toString();
    }
}
