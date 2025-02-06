/*
 *     Copyright (C) 2016-2025 zafaco GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License version 3
 *     as published by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zafaco.moduleCommon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Log
{

    private static final String TAG = "LOGGER";

    public static void error(@NonNull Throwable ex)
    {
        error(TAG, ex);
    }


    public static void error(@NonNull String tag, @NonNull Throwable ex)
    {
        error(tag, "", ex, null);
    }


    public static void error(@NonNull String tag, @NonNull String msg)
    {
        error(tag, msg, null, null);
    }


    public static void error(@NonNull String tag, String msg, Throwable ex)
    {
        error(tag, msg, ex, null);
    }


    public static void error(@NonNull String tag, String msg, HashMap<String, Object> extras)
    {
        error(tag, msg, null, extras);
    }


    public static void error(@NonNull String tag, String msg, JSONObject extras)
    {
        error(tag, msg, null, extras);
    }


    public static void error(@NonNull String tag, @NonNull String msg, Throwable ex, Object object)
    {
        error(tag, msg, ex, object, null);
    }


    public static void error(@NonNull String tag, @NonNull String msg, Throwable ex, Object object, HashMap<String, String> tags)
    {

        HashMap<String, Object> extras    = new HashMap<>();
        StringBuilder           mapString = null;
        if (object != null)
        {
            if (object instanceof JSONObject)
            {
                extras = new Gson().fromJson(object.toString(), HashMap.class);
            } else if (object instanceof HashMap)
            {
                extras = (HashMap) object;
            }

            mapString = getExtrasString(extras);
        }
        androidLogError(tag, getLogString(tag, msg, mapString, ex));
    }


    public static void warning(String msg)
    {
        warning(TAG, msg);
    }


    public static void warning(String tag, String msg)
    {
        warning(tag, msg, null, null);
    }


    public static void warning(String tag, Throwable ex)
    {
        warning(tag, "", null, ex);
    }


    public static void warning(String tag, String msg, Object object)
    {
        warning(tag, msg, object, null);
    }


    public static void warning(String tag, String msg, Throwable ex)
    {
        warning(tag, msg, null, ex);
    }


    public static void warning(String tag, String msg, Object object, Throwable ex)
    {
        warning(tag, msg, object, ex, null);
    }


    public static void warning(String tag, String msg, @Nullable Object object, @Nullable Throwable ex, @Nullable HashMap<String, String> tags)
    {
        HashMap<String, Object> extras    = new HashMap<>();
        StringBuilder           mapString = null;
        if (object != null)
        {
            if (object instanceof JSONObject)
            {
                extras = new Gson().fromJson(object.toString(), HashMap.class);
            } else if (object instanceof HashMap)
            {
                extras = (HashMap) object;
            }

            mapString = getExtrasString(extras);
        }
        androidLogWarning(tag, getLogString(tag, msg, mapString, ex));
    }


    public static void debug(String msg)
    {
        debug(TAG, msg);
    }


    public static void debug(String tag, String msg)
    {
        debug(tag, msg, null, null);
    }


    public static void debug(String tag, String msg, Object object)
    {
        debug(tag, msg, object, null);
    }


    public static void debug(String tag, String msg, @Nullable Object object, @Nullable HashMap<String, String> tags)
    {
        if (!BuildConfig.DEBUG)
            return;
        HashMap<String, Object> extras    = new HashMap<>();
        StringBuilder           mapString = null;
        if (object != null)
        {
            extras = new HashMap<>();
            if (object instanceof JSONObject)
            {
                extras = new Gson().fromJson(object.toString(), HashMap.class);
            } else if (object instanceof HashMap)
            {
                extras = (HashMap<String, Object>) object;
            }
            mapString = getExtrasString(extras);
        }

        androidLogDebug(tag, getLogString(tag, msg, mapString, null));
    }


    public static void info(String msg)
    {
        info(TAG, msg);
    }


    public static void info(String tag, String msg)
    {
        info(tag, msg, null);
    }


    public static void info(String tag, String msg, Object object)
    {
        info(tag, msg, object, null);
    }


    public static void info(String tag, String msg, Object object, Throwable ex)
    {
        info(tag, msg, object, ex, null);
    }


    public static void info(String tag, String msg, Object object, Throwable ex, HashMap<String, String> tags)
    {
        HashMap<String, Object> extras    = new HashMap<>();
        StringBuilder           mapString = null;
        if (object != null)
        {
            if (object instanceof JSONObject)
            {
                extras = new Gson().fromJson(object.toString(), HashMap.class);
            } else if (object instanceof HashMap)
            {
                extras = (HashMap<String, Object>) object;
            }
            mapString = getExtrasString(extras);
        }

        androidLogInfo(tag, getLogString(tag, msg, mapString, ex));
    }


    private static StringBuilder getExtrasString(HashMap<String, Object> extras)
    {
        StringBuilder mapString = new StringBuilder();
        for (Map.Entry<String, Object> entry : extras.entrySet())
        {
            mapString.append(entry.getKey()).append(" = ");

            if (entry.getValue() != null)
                mapString.append(entry.getValue().toString());

            mapString.append("\n");
        }

        return mapString;
    }

    private static String getLogString(@NonNull String tag, @NonNull String msg, @Nullable StringBuilder extras, @Nullable Throwable ex)
    {
        StringBuilder string = new StringBuilder();
        string.append("##### ").append(tag).append(" ##### \n");
        string.append(msg).append("\n");
        if (extras != null)
            string.append(extras).append("\n");

        if (ex != null)
        {
            if (BuildConfig.DEBUG)
                string.append(android.util.Log.getStackTraceString(ex));
            else
                string.append(ex.getMessage());
        }

        return string.toString();
    }

    private static void androidLogDebug(String tag, String msg)
    {
        android.util.Log.d(tag, msg);
    }

    private static void androidLogInfo(String tag, String msg)
    {
        android.util.Log.i(tag, msg);
    }

    private static void androidLogWarning(String tag, String msg)
    {
        android.util.Log.w(tag, msg);
    }

    private static void androidLogError(String tag, String msg)
    {
        android.util.Log.e(tag, msg);
    }
}
