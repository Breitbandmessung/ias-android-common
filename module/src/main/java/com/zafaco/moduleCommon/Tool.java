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

import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static android.telephony.TelephonyManager.SIM_STATE_CARD_IO_ERROR;
import static android.telephony.TelephonyManager.SIM_STATE_CARD_RESTRICTED;
import static android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED;
import static android.telephony.TelephonyManager.SIM_STATE_NOT_READY;
import static android.telephony.TelephonyManager.SIM_STATE_PERM_DISABLED;
import static android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED;
import static android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

public class Tool
{

    private static final String TAG = "Tool";


    public JSONObject mergeJSON(JSONObject object1, JSONObject object2)
    {
        try
        {
            for (Iterator<String> iter = object2.keys(); iter.hasNext(); )
            {
                String key = iter.next();
                object1.put(key, object2.get(key));
            }
        } catch (Exception ex)
        {
        }

        return object1;
    }

    public static JSONObject readJSON(Context ctx, String path)
    {
        String json = null;
        try
        {
            InputStream is = ctx.getAssets().open(path);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, StandardCharsets.UTF_8);

            return new JSONObject(json);
        } catch (IOException | JSONException ex)
        {
            Log.warning(TAG, "readJSON", ex);
            return null;
        }
    }

    public static JSONObject mapJSON(JSONObject original, JSONObject mappingJSON)
    {

        JSONObject mapped = new JSONObject();

        try
        {
            if (original != null)
            {
                for (Iterator<String> iter = mappingJSON.keys(); iter.hasNext(); )
                {
                    String     key               = iter.next();
                    JSONObject mappingDetail     = mappingJSON.getJSONObject(key);
                    String     mappingType       = mappingDetail.getString("type");
                    JSONArray  parameterMappings = mappingDetail.getJSONArray("mappings");
                    for (int i = 0; i < parameterMappings.length(); i++)
                    {
                        JSONObject parameter = parameterMappings.optJSONObject(i);

                        String new_key           = parameter.optString("new_key");
                        String old_key           = parameter.optString("old_key");
                        String old_key_divider   = parameter.optString("old_key_divider");
                        String parameterType     = parameter.optString("type");
                        String type              = parameter.optString("type");
                        double convert           = parameter.optDouble("convert");
                        double convertMultiplier = parameter.optDouble("convert_multiplier");
                        String format            = parameter.optString("format");

                        switch (mappingType)
                        {
                            case "array":
                                JSONArray originalArray = original.optJSONArray(key);
                                if (originalArray != null)
                                {
                                    switch (parameterType)
                                    {
                                        case "last":
                                            mapped.put(new_key, originalArray.optJSONObject(originalArray.length() - 1).opt(old_key));
                                            break;
                                        case "max":
                                            long max = Long.MIN_VALUE;
                                            for (int j = 0; j < originalArray.length(); j++)
                                            {
                                                if (originalArray.getJSONObject(j).getDouble(old_key_divider) == 0)
                                                    continue;
                                                double throughput = (originalArray.getJSONObject(j).getLong(old_key) * convertMultiplier) / (originalArray.getJSONObject(j).getDouble(old_key_divider) / convert);
                                                if (throughput > max)
                                                    max = (long) throughput;
                                            }
                                            if (max != Long.MIN_VALUE && originalArray.length() > 0)
                                                mapped.put(new_key, max);
                                            convert = Double.NaN;
                                            break;
                                        case "min":
                                            long min = Long.MAX_VALUE;
                                            for (int j = 0; j < originalArray.length(); j++)
                                            {
                                                if (originalArray.getJSONObject(j).getDouble(old_key_divider) == 0)
                                                    continue;
                                                double throughput = (originalArray.getJSONObject(j).getLong(old_key) * convertMultiplier) / (originalArray.getJSONObject(j).getDouble(old_key_divider) / convert);
                                                if (throughput < min)
                                                    min = (long) throughput;
                                            }
                                            if (min != Long.MAX_VALUE && originalArray.length() > 0)
                                                mapped.put(new_key, min);
                                            convert = Double.NaN;
                                            break;
                                        case "all":
                                            mapped.put(new_key, originalArray.toString());
                                            break;
                                        case "array":
                                            JSONArray tempArray = new JSONArray();
                                            JSONArray tempMappings = parameter.getJSONArray("mappings");
                                            for (int j = 0; j < originalArray.length(); j++)
                                            {
                                                JSONObject tempObject = new JSONObject();
                                                for (int k = 0; k < tempMappings.length(); k++)
                                                {
                                                    double tempDivide = tempMappings.optJSONObject(k).optDouble("convert");
                                                    if (!Double.isNaN(tempDivide))
                                                        tempObject.put(tempMappings.optJSONObject(k).optString("new_key"), originalArray.getJSONObject(j).optLong(tempMappings.optJSONObject(k).optString("old_key")) / tempDivide);
                                                    else if (tempMappings.optJSONObject(k).optString("type").equals("index"))
                                                        tempObject.put(tempMappings.optJSONObject(k).optString("new_key"), (j + 1));
                                                    else
                                                        tempObject.put(tempMappings.optJSONObject(k).optString("new_key"), originalArray.getJSONObject(j).get(tempMappings.optJSONObject(k).optString("old_key")));

                                                }
                                                tempArray.put(tempObject);

                                            }

                                            mapped.put(new_key, tempArray.toString());
                                            break;
                                        case "throughput":

                                            break;
                                        default:
                                            Log.warning(TAG, "incorrect parameter type in mapping");

                                    }
                                    if (!Double.isNaN(convert))
                                    {
                                        mapped.put(new_key, mapped.optLong(new_key) / convert);
                                    }
                                }
                                break;
                            case "object":

                                JSONObject originalObject = (key.equals("general")) ? original : original.optJSONObject(key);

                                if (originalObject != null)
                                {
                                    mapped.put(new_key, originalObject.opt(old_key));
                                    if (!Double.isNaN(convert))
                                    {
                                        if (type.equals("int"))
                                            mapped.put(new_key, mapped.optLong(new_key) / (long) convert);
                                        else
                                            mapped.put(new_key, mapped.optLong(new_key) / convert);

                                        if (!format.isEmpty())
                                        {
                                            mapped.put(new_key, getDateFromTimestamp(mapped.optLong(new_key), format));
                                        }
                                    }
                                }
                                break;

                            default:
                                Log.warning(TAG, "incorrect mapping type in mapping");

                        }
                    }
                }
            }
        } catch (JSONException ex)
        {
            Log.warning(TAG, "mapJSON failed", ex);
        }


        return mapped;
    }

    private static Object convertValue(Object oldValue, double convertValue, String convertArithmetic)
    {
        if (!Double.isNaN(convertValue))
        {
            switch (convertArithmetic)
            {
                case "/":
                    return (long) oldValue / convertValue;
                case "*":
                    return (long) oldValue * convertValue;
                case "+":
                    return (long) oldValue + convertValue;
                case "-":
                    return (long) oldValue - convertValue;
            }
        }
        return oldValue;
    }

    public static long getTimestampFromDate(String date)
    {
        return getTimestampFromDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static long getTimestampFromDate(String date, String format)
    {
        DateFormat f = new SimpleDateFormat(format, Locale.getDefault());
        try
        {
            return f.parse(date).getTime();
        } catch (ParseException e)
        {
            return -1;
        }
    }

    public static String getDateFromTimestamp(long timestamp)
    {
        return getDateFromTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    public static String getDateFromTimestamp(long timestamp, String format)
    {
        Date       date = new Date(timestamp);
        DateFormat f    = new SimpleDateFormat(format, Locale.getDefault());
        return f.format(date);
    }

    public String getCursorValue(Cursor cCursor, String sColumn)
    {
        return cCursor.getString(cCursor.getColumnIndexOrThrow(sColumn));
    }


    public String getNetType(int netType)
    {
        switch (netType)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO revision 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO revision A";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "iDen";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO revision B";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "TD_SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "IWLAN";
            case 19:
                return "NRNSA";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "NR";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "unknown";
            default:
                return "unknown";
        }
    }

    public String getOverrideNetType(int netType)
    {
        switch (netType)
        {
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE:
                return "none";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA:
                return "LTE CA";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO:
                return "LTE Advanced Pro";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA:
                return "NR NSA";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED:
                return "NR SA or MMWAVE";
            default:
                return "unknown";
        }
    }

    public String getStateString(int state)
    {
        switch (state)
        {
            case ServiceState.STATE_OUT_OF_SERVICE:
                return "STATE_OUT_OF_SERVICE";
            case ServiceState.STATE_EMERGENCY_ONLY:
                return "STATE_EMERGENCY_ONLY";
            case ServiceState.STATE_IN_SERVICE:
                return "STATE_IN_SERVICE";
            case ServiceState.STATE_POWER_OFF:
                return "STATE_POWER_OFF";
            default:
                return "STATE_UNKNOWN";
        }
    }

    public String getPhoneType(int phoneType)
    {
        switch (phoneType)
        {
            case TelephonyManager.PHONE_TYPE_NONE:
                return "None";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
            default:
                return "Unknown";
        }
    }

    public String setCategory(int id)
    {
        switch (id)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case 19:
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "WLAN";
            default:
                return "unknown";
        }
    }

    public String mappingProvider(String app_operator_sim_mcc, String app_operator_sim_mnc)
    {
        if (app_operator_sim_mcc == null || !app_operator_sim_mcc.equals("262") || app_operator_sim_mnc == null)
            return "-";

        switch (app_operator_sim_mnc)
        {
            case "01":
            case "06":
                return "Telekom.de";
            case "02":
            case "04":
            case "09":
                return "Vodafone.de";
            case "03":
            case "05":
            case "07":
            case "08":
            case "11":
            case "77":
                return "o2 - de";
            case "23":
                return "1&1";
            default:
                return "-";
        }
    }


    public boolean isVPN(Context ctx)
    {
        ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                NetworkCapabilities networkCapabilities = connMan.getNetworkCapabilities(connMan.getActiveNetwork());
                return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            } else
            {
                return connMan.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
            }
        } catch (Exception ex)
        {
            Log.warning(TAG, "isVPN failed", ex);
            return false;
        }
    }


    public boolean isPowerSafeMode(Context ctx)
    {
        PowerManager powMan = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            return powMan.isPowerSaveMode();
        }
        return false;
    }


    public boolean isWifi(Context ctx)
    {
        ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network             network             = Objects.requireNonNull(connMan).getActiveNetwork();
        NetworkCapabilities networkCapabilities = Objects.requireNonNull(connMan).getNetworkCapabilities(network);

        if (networkCapabilities != null)
        {
            return  networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else
        {
            return false;
        }
    }

    public boolean isMobile(Context ctx)
    {
        ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = Objects.requireNonNull(connMan).getActiveNetwork();
        NetworkCapabilities networkCapabilities = Objects.requireNonNull(connMan).getNetworkCapabilities(network);

        if (networkCapabilities != null)
        {
            return  networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else
        {
            return false;
        }
    }

    public boolean isEthernet(Context ctx)
    {
        ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = Objects.requireNonNull(connMan).getActiveNetwork();
        NetworkCapabilities networkCapabilities = Objects.requireNonNull(connMan).getNetworkCapabilities(network);

        if (networkCapabilities != null)
        {
            return  networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else
        {
            return false;
        }

    }

    public String getConnectionType(Context ctx)
    {
        if (isEthernet(ctx))
            return "ETHERNET";
        if (isWifi(ctx))
            return "WIFI";
        if (isMobile(ctx))
            return "WWAN";

        return "UNKNOWN";
    }


    public boolean isRoaming(Context ctx)
    {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isNetworkRoaming();
    }


    public boolean isOnline(Context ctx)
    {
        ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = Objects.requireNonNull(connMan).getActiveNetworkInfo();

        if (networkInfo != null)
            return networkInfo.isConnected();

        return false;
    }


    public boolean isWifiTethering(Context ctx)
    {
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try
        {
            Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
            try
            {
                return (boolean) method.invoke(wm);
            } catch (NullPointerException | IllegalArgumentException | IllegalAccessException |
                     InvocationTargetException e)
            {
                Log.warning(TAG, "Reflection failed for wifi ap state", e);
            }
        } catch (NoSuchMethodException | SecurityException ex)
        {
            Log.warning(TAG, "Reflection failed for wifi ap state", ex);
        }
        return false;
    }


    public int getThermalStatus(Context ctx)
    {
        PowerManager powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
        {
            return powerManager.getCurrentThermalStatus();
        }
        return -1;
    }


    public String getSimStateString(int simStateId)
    {
        String simState = "SIM_STATE_UNDEFINED";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            switch (simStateId)
            {
                case SIM_STATE_NOT_READY:
                    simState = "SIM_STATE_NOT_READY";
                    break;
                case SIM_STATE_PERM_DISABLED:
                    simState = "SIM_STATE_PERM_DISABLED";
                    break;
                case SIM_STATE_CARD_IO_ERROR:
                    simState = "SIM_STATE_CARD_IO_ERROR";
                    break;
                case SIM_STATE_CARD_RESTRICTED:
                    simState = "SIM_STATE_CARD_RESTRICTED";
                    break;
            }
        }

        switch (simStateId)
        {
            case SIM_STATE_ABSENT:
                simState = "SIM_STATE_ABSENT";
                break;
            case SIM_STATE_NETWORK_LOCKED:
                simState = "SIM_STATE_NETWORK_LOCKED";
                break;
            case SIM_STATE_PIN_REQUIRED:
                simState = "SIM_STATE_PIN_REQUIRED";
                break;
            case SIM_STATE_PUK_REQUIRED:
                simState = "SIM_STATE_PUK_REQUIRED";
                break;
            case SIM_STATE_READY:
                simState = "SIM_STATE_READY";
                break;
            case SIM_STATE_UNKNOWN:
                simState = "SIM_STATE_UNKNOWN";

        }
        return simState;
    }

    public int isSimReady(Context ctx)
    {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        int SIM_State = -1;

        int simState = tm.getSimState();

        switch (simState)
        {
            case SIM_STATE_ABSENT:
                SIM_State = SIM_STATE_ABSENT;
                break;
            case SIM_STATE_NETWORK_LOCKED:
                SIM_State = SIM_STATE_NETWORK_LOCKED;
                break;
            case SIM_STATE_PIN_REQUIRED:
                SIM_State = SIM_STATE_PIN_REQUIRED;
                break;
            case SIM_STATE_PUK_REQUIRED:
                SIM_State = SIM_STATE_PUK_REQUIRED;
                break;
            case SIM_STATE_READY:
                SIM_State = SIM_STATE_READY;
                break;
            case SIM_STATE_UNKNOWN:
                SIM_State = SIM_STATE_UNKNOWN;
                break;
        }

        return SIM_State;
    }

    public boolean isCalling(Context context)
    {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        assert manager != null;
        return manager.getMode() == AudioManager.MODE_IN_CALL;
    }

    public boolean isAirplane(Context context)
    {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public boolean isServiceRunning(Class<?> serviceClass, Context ctx)
    {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : Objects.requireNonNull(manager).getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public boolean existsFile(Context context, String dbName)
    {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }


    public String tokb(String value, String type)
    {
        try
        {
            switch (type)
            {
                case "kB":
                    return "" + (int) Double.parseDouble(value);
                case "MB":
                    return "" + (int) (Double.parseDouble(value) * 1000);
                case "GB":
                    return "" + (int) (Double.parseDouble(value) * 1000000);

                case "Mbit/s":
                    return "" + Integer.parseInt(value) * 1000;
            }
        } catch (NumberFormatException nfe)
        {
            return "-1";
        }

        return "-1";
    }


    public boolean getSimCardState(Context ctx)
    {
        boolean simReady = false;

        try
        {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

            assert tm != null;

            if (tm.getSimState() == SIM_STATE_READY)
            {
                simReady = true;
            }
        } catch (Exception ex)
        {
            simReady = false;
        }

        return simReady;
    }

    private String bytesToHex(byte[] hash)
    {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++)
        {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public void copyFiles(File src, File dst) throws IOException
    {
        try (InputStream in = new FileInputStream(src))
        {
            try (OutputStream out = new FileOutputStream(dst))
            {

                byte[] buf = new byte[1024];
                int    len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public String formatStringToGUI(String value, int factor)
    {
        DecimalFormat df = new DecimalFormat("#0.00");

        Double d = Double.parseDouble(value) / factor;

        return df.format(d);
    }

    public String formatStringToGUI(String value, int factor, String unit)
    {
        DecimalFormat df = new DecimalFormat("#0.00");

        Double d = Double.parseDouble(value) / factor;

        return df.format(d) + " " + unit;
    }

    public String formatStringToGUI(Double value, int factor, String unit)
    {
        DecimalFormat df = new DecimalFormat("#0.00");

        Double d = value / factor;

        return df.format(d) + " " + unit;
    }

    public Object getObject(String s, Class<?> cls)
    {
        Gson gson = new Gson();

        return gson.fromJson(s, cls);
    }

    public String saveObject(Object o)
    {
        Gson gson = new Gson();

        return gson.toJson(o);
    }

    public int getNetworkType(final Context ctx)
    {
        return getNetworkType(ctx, 500);
    }

    public int getNetworkType(final Context ctx, int timeout)
    {
        final int[]     networkTypeId    = new int[1];
        final String[]  sServiceState    = new String[1];
        final boolean[] networkTypeDone  = {false};
        final boolean[] serviceStateDone = {false};

        Thread networkThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                final TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                Looper.prepare();
                tm.listen(new PhoneStateListener()
                {
                    @Override
                    public void onDataConnectionStateChanged(int networkState, int networkType)
                    {
                        networkTypeDone[0] = true;
                        networkTypeId[0]   = networkType;
                        super.onDataConnectionStateChanged(networkState, networkType);
                        if (networkTypeDone[0] && serviceStateDone[0])
                        {
                            tm.listen(this, PhoneStateListener.LISTEN_NONE);
                            Looper.myLooper().quit();
                        }
                    }

                    @Override
                    public void onServiceStateChanged(ServiceState serviceState)
                    {
                        serviceStateDone[0] = true;
                        sServiceState[0]    = serviceState.toString();
                        super.onServiceStateChanged(serviceState);
                        if (networkTypeDone[0] && serviceStateDone[0])
                        {
                            tm.listen(this, PhoneStateListener.LISTEN_NONE);
                            Looper.myLooper().quit();
                        }
                    }
                }, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
                Looper.loop();
            }
        });
        networkThread.start();
        try
        {
            networkThread.join(timeout);
        } catch (InterruptedException ignored)
        {
        }

        if (sServiceState[0] != null)
        {
            int adjustedId = parseNetworkRegistrationInfo(sServiceState[0]);

            if (adjustedId != -1)
            {
                networkTypeId[0] = adjustedId;
            }
        }
        return networkTypeId[0];
    }


    public int parseNetworkRegistrationInfo(String sInfo)
    {


        if (sInfo.contains("nrState=CONNECTED") || sInfo.contains("nrState=NOT_RESTRICTED") || sInfo.contains("isNrAvailable = true") || sInfo.contains("nsaState=5") || sInfo.contains("EnDc=true") || sInfo.contains("5G Allocated=true"))
        {
            return 19;
        }

        return -1;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public int getActiveSimCount(final Context ctx)
    {

        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        int simsActive = 0;
        for (int i = 0; i < 10; i++)
        {
            if (tm.getSimState(i) == SIM_STATE_READY)
                simsActive++;
        }
        return simsActive;
    }

    public boolean isMockLocationOn(Location location)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            return location.isMock();
        } else
        {
            return location.isFromMockProvider();
        }
    }
}
