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

package com.zafaco.moduleCommon.models.client;

import android.content.Context;
import android.os.Build;

import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ResultInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

public class ClientInfo implements ResultInfo
{
    private int    timezone;
    private String manufacturer;
    private String model;
    private String libraryVersion;
    private String libraryVersionName;
    private String commonVersion;
    private String connectionType;
    private String appVersion;
    private String gitHash;
    private final Context ctx;

    public ClientInfo()
    {
        this(null, null, null);
    }

    public ClientInfo(Context context, String libraryVersion, String libraryVersionName)
    {
        this.ctx      = context;
        manufacturer  = Build.MANUFACTURER;
        model         = Build.MODEL;
        commonVersion = com.zafaco.moduleCommon.BuildConfig.VERSION_NAME;
        timezone      = (TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000;
        getConnectionTypeData();
        if (libraryVersion != null)
            this.libraryVersion = libraryVersion;
        if (libraryVersionName != null)
            this.libraryVersionName = libraryVersionName;
    }

    public int getTimezone()
    {
        return timezone;
    }

    public void setTimezone(int timezone)
    {
        this.timezone = timezone;
    }

    public String getManufacturer()
    {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer)
    {
        this.manufacturer = manufacturer;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getConnectionType()
    {
        return connectionType;
    }

    public void setConnectionType(String connectionType)
    {
        this.connectionType = connectionType;
    }

    public String getAppVersion()
    {
        return appVersion;
    }

    public void setAppVersion(String appVersion)
    {
        this.appVersion = appVersion;
    }

    public String getCommonVersion()
    {
        return commonVersion;
    }

    public void setCommonVersion(String commonVersion)
    {
        this.commonVersion = commonVersion;
    }

    public String getGitHash()
    {
        return gitHash;
    }

    public void setGitHash(String gitHash)
    {
        this.gitHash = gitHash;
    }

    public String getLibraryVersion()
    {
        return libraryVersion;
    }

    public void setLibraryVersion(String libraryVersion)
    {
        this.libraryVersion = libraryVersion;
    }

    public String getLibraryVersionName()
    {
        return libraryVersionName;
    }

    public void setLibraryVersionName(String libraryVersionName)
    {
        this.libraryVersionName = libraryVersionName;
    }

    private void getConnectionTypeData()
    {
        if (ctx == null)
            return;
        Tool tool = new Tool();

        if (tool.isWifi(ctx))
            connectionType = "WIFI";
        else if (tool.isMobile(ctx))
            connectionType = "CELLULAR";
        else if (tool.isEthernet(ctx))
            connectionType = "ETHERNET";
        else
            connectionType = "UNKNOWN";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jData = new JSONObject();
        try
        {
            jData.put("timezone", getTimezone());
            jData.put("manufacturer", getManufacturer());
            jData.put("model", getModel());
            if (libraryVersionName != null && libraryVersion != null)
                jData.put(getLibraryVersionName() + "_version", getLibraryVersion());
            jData.put("common_version", getCommonVersion());
            jData.put("connection_type", getConnectionType());
            jData.put("app_version", getAppVersion());
        } catch (JSONException ignored)
        {
        }

        return jData;
    }
}
