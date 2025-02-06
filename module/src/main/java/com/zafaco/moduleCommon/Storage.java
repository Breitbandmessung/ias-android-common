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

import android.content.Context;
import android.os.Build;

import java.io.File;

public class Storage
{
    private static final String TAG = "Storage";


    public static long getTotalInternalStorageSize(Context ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            return new File(ctx.getFilesDir().getAbsoluteFile().toString()).getTotalSpace();
        } else
        {
            return getStorage(ctx, "internal", "total");
        }
    }


    public static long getAvailableInternalStorageSize(Context ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            return new File(ctx.getFilesDir().getAbsoluteFile().toString()).getFreeSpace();
        } else
        {
            return getStorage(ctx, "internal", "free");
        }
    }


    public static long getUsedInternalStorageSize(Context ctx)
    {
        long used = getTotalInternalStorageSize(ctx) - getAvailableInternalStorageSize(ctx);

        if (used > 0)
            return used;

        return 0;
    }


    public static long getTotalExternalStorageSize(Context ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            File[] files = ctx.getExternalFilesDirs(null);

            if (files.length > 1)
                try
                {
                    return new File(files[1].toString()).getTotalSpace();
                } catch (NullPointerException ex)
                {
                    Log.warning(TAG, "getTotalExternalStorageSize failed", ex);
                }
        } else
        {
            return getStorage(ctx, "external", "total");
        }
        return 0;
    }


    public static long getAvailableExternalStorageSize(Context ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            File[] files = ctx.getExternalFilesDirs(null);

            if (files.length > 1)
                try
                {
                    return new File(files[1].toString()).getFreeSpace();
                } catch (NullPointerException ex)
                {
                    Log.warning(TAG, "getAvailableExternalStorageSize failed", ex);
                }
        } else
        {
            return getStorage(ctx, "external", "free");
        }
        return 0;
    }


    public static long getUsedExternalStorageSize(Context ctx)
    {
        long used = getTotalExternalStorageSize(ctx) - getAvailableExternalStorageSize(ctx);

        if (used > 0)
            return used;

        return 0;
    }


    private static long getStorage(Context ctx, String sDevice, String sType)
    {
        Environment4.Device[] test;

        test = Environment4.getDevices(ctx);

        for (Environment4.Device d : test)
        {
            if (sDevice.equals("internal") && d.isPrimary())
            {
                switch (sType)
                {
                    case "total":
                        return d.getTotalSpace();
                    case "free":
                        return d.getFreeSpace();
                    case "used":
                        return (d.getTotalSpace() - d.getFreeSpace());
                }
            }
            if (sDevice.equals("external") && d.isRemovable())
            {
                switch (sType)
                {
                    case "total":
                        return d.getTotalSpace();
                    case "free":
                        return d.getFreeSpace();
                    case "used":
                        return (d.getTotalSpace() - d.getFreeSpace());
                }

            }
        }

        return 0;
    }


    public static String formatSize(long size)
    {
        String suffix = null;

        if (size >= 1024)
        {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024)
            {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0)
        {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null)
            resultBuffer.append(suffix);
        return resultBuffer.toString();
    }
}
