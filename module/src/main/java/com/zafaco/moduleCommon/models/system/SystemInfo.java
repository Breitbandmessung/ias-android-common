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

package com.zafaco.moduleCommon.models.system;

import static android.content.Context.BATTERY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;

import com.zafaco.moduleCommon.Storage;
import com.zafaco.moduleCommon.interfaces.ResultInfo;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfo implements ResultInfo
{

    private Context ctx;
    private String  osName                   = "Android";
    private int     sdkVersion               = Build.VERSION.SDK_INT;
    private String  osVersion                = Build.VERSION.RELEASE;
    private long    clientUptime             = SystemClock.elapsedRealtime();
    private long    internalStorageUsed      = -1;
    private long    internalStorageAvailable = -1;
    private long    externalStorageUsed      = -1;
    private long    externalStorageAvailable = -1;
    private long    ramUsed                  = -1;
    private long    ramAvailable             = -1;
    private int     batteryPercent           = -1;

    public SystemInfo()
    {
    }

    public SystemInfo(Context ctx)
    {
        this.ctx = ctx;

        internalStorageUsed      = Storage.getUsedInternalStorageSize(ctx);
        internalStorageAvailable = Storage.getAvailableInternalStorageSize(ctx);
        externalStorageUsed      = Storage.getUsedExternalStorageSize(ctx);
        externalStorageAvailable = Storage.getAvailableExternalStorageSize(ctx);

        getRamData();

        batteryPercent = getBatteryPercentage();
    }

    public String getOsName()
    {
        return osName;
    }

    public void setOsName(String osName)
    {
        this.osName = osName;
    }

    public String getOsVersion()
    {
        return osVersion;
    }

    public void setOsVersion(String osVersion)
    {
        this.osVersion = osVersion;
    }

    public long getClientUptime()
    {
        return clientUptime;
    }

    public void setClientUptime(long clientUptime)
    {
        this.clientUptime = clientUptime;
    }

    public long getInternalStorageUsed()
    {
        return internalStorageUsed;
    }

    public void setInternalStorageUsed(long internalStorageUsed)
    {
        this.internalStorageUsed = internalStorageUsed;
    }

    public long getInternalStorageAvailable()
    {
        return internalStorageAvailable;
    }

    public void setInternalStorageAvailable(long internalStorageAvailable)
    {
        this.internalStorageAvailable = internalStorageAvailable;
    }

    public long getExternalStorageUsed()
    {
        return externalStorageUsed;
    }

    public void setExternalStorageUsed(long externalStorageUsed)
    {
        this.externalStorageUsed = externalStorageUsed;
    }

    public long getExternalStorageAvailable()
    {
        return externalStorageAvailable;
    }

    public void setExternalStorageAvailable(long externalStorageAvailable)
    {
        this.externalStorageAvailable = externalStorageAvailable;
    }

    public long getRamUsed()
    {
        return ramUsed;
    }

    public void setRamUsed(long ramUsed)
    {
        this.ramUsed = ramUsed;
    }

    public long getRamAvailable()
    {
        return ramAvailable;
    }

    public void setRamAvailable(long ramAvailable)
    {
        this.ramAvailable = ramAvailable;
    }

    public void getRamData()
    {
        final ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

        am.getMemoryInfo(memoryInfo);

        ramAvailable = memoryInfo.availMem;
        ramUsed      = memoryInfo.totalMem - memoryInfo.availMem;
    }


    @Override
    public JSONObject toJson()
    {
        JSONObject jData = new JSONObject();

        try
        {
            jData.put("os_name", getOsName());
            jData.put("os_version", getOsVersion());
            jData.put("sdk_version", getSdkVersion());
            jData.put("storage_internal_available", getInternalStorageAvailable());
            jData.put("storage_internal_used", getInternalStorageUsed());
            jData.put("storage_external_available", getExternalStorageAvailable());
            jData.put("storage_external_used", getExternalStorageUsed());
            jData.put("ram_used", getRamUsed());
            jData.put("ram_available", getRamAvailable());
            jData.put("uptime", getClientUptime());
            jData.put("battery_percentage", getBatteryPercent());

        } catch (JSONException ignored)
        {
        }
        return jData;
    }


    private int getBatteryPercentage()
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {

            BatteryManager bm = (BatteryManager) ctx.getSystemService(BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        } else
        {

            IntentFilter iFilter       = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent       batteryStatus = ctx.registerReceiver(null, iFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            double batteryPct = level / (double) scale;

            return (int) (batteryPct * 100);
        }
    }

    public int getBatteryPercent()
    {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent)
    {
        this.batteryPercent = batteryPercent;
    }

    public int getSdkVersion()
    {
        return sdkVersion;
    }

    public void setSdkVersion(int sdkVersion)
    {
        this.sdkVersion = sdkVersion;
    }
}
