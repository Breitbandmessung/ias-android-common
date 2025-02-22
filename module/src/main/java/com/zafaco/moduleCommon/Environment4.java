

/*
 * Environment4.java
 * find SD-Card/USB on Android 4 using undocumented StorageManager.getVolumeList
 * https://github.com/jow-ct/Environment4
 */

package com.zafaco.moduleCommon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class Environment4
{
    private static final String TAG = "Environment4";

    public final static String TYPE_PRIMARY  = "primär";
    public final static String TYPE_INTERNAL = "intern";
    public final static String TYPE_SD       = "MicroSD";
    public final static String TYPE_USB      = "USB";
    public final static String TYPE_UNKNOWN  = "unbekannt";

    public final static String WRITE_NONE     = "none";
    public final static String WRITE_READONLY = "readonly";
    public final static String WRITE_APPONLY  = "apponly";
    public final static String WRITE_FULL     = "readwrite";

    private static Device[] devices, externalstorage, storage;
    private static BroadcastReceiver receiver;
    private static boolean           useReceiver = true;
    private static String            userDir;


    public static Device[] getDevices(Context context)
    {
        if (devices == null)
            initDevices(context);
        return devices;
    }


    public static Device[] getExternalStorage(Context context)
    {
        if (devices == null)
            initDevices(context);
        return externalstorage;
    }


    public static Device[] getStorage(Context context)
    {
        if (devices == null)
            initDevices(context);
        return storage;
    }


    public static IntentFilter getRescanIntentFilter()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);

        filter.addDataScheme("file");


        return filter;
    }


    public static void setUseReceiver(Context context, boolean use)
    {
        if (use && receiver == null)
        {

            receiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    Log.i(TAG, "Storage " + intent.getAction() + "-" + intent.getData());
                    initDevices(context);
                }
            };
            context.registerReceiver(receiver, getRescanIntentFilter());
        } else if (!use && receiver != null)
        {

            context.unregisterReceiver(receiver);
            receiver = null;
        }
        useReceiver = use;
    }


    public static void initDevices(Context context)
    {

        if (userDir == null)
            userDir = "/Android/data/" + context.getPackageName();


        setUseReceiver(context, useReceiver);


        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class          c  = sm.getClass();
        Object[]       vols;
        try
        {


            Method m = c.getMethod("getVolumeList");

            vols = (Object[]) m.invoke(sm);
            Device[] temp = new Device[vols.length];
            for (int i = 0; i < vols.length; i++)
                 temp[i] = new Device(vols[i]);


            Device primary = null;
            for (Device d : temp)
                if (d.mPrimary)
                    primary = d;
            if (primary == null)
                for (Device d : temp)
                    if (!d.mRemovable)
                    {
                        d.mPrimary = true;
                        primary    = d;
                        break;
                    }
            if (primary == null)
            {
                primary          = temp[0];
                primary.mPrimary = true;
            }


            File[] files  = ContextCompat.getExternalFilesDirs(context, null);
            File[] caches = ContextCompat.getExternalCacheDirs(context);
            for (Device d : temp)
            {
                if (files != null)
                    for (File f : files)
                        if (f != null && f.getAbsolutePath().startsWith(d.getAbsolutePath()))
                            d.mFiles = f;
                if (caches != null)
                    for (File f : caches)
                        if (f != null && f.getAbsolutePath().startsWith(d.getAbsolutePath()))
                            d.mCache = f;
            }


            ArrayList<Device> tempDev  = new ArrayList<Device>(10);
            ArrayList<Device> tempStor = new ArrayList<Device>(10);
            ArrayList<Device> tempExt  = new ArrayList<Device>(10);
            for (Device d : temp)
            {
                tempDev.add(d);
                if (d.isAvailable())
                {
                    tempExt.add(d);
                    tempStor.add(d);
                }
            }


            Device internal = new Device(context);
            tempStor.add(0, internal);
            if (!primary.mEmulated)
                tempDev.add(0, internal);


            devices         = tempDev.toArray(new Device[tempDev.size()]);
            storage         = tempStor.toArray(new Device[tempStor.size()]);
            externalstorage = tempExt.toArray(new Device[tempExt.size()]);

            setUseReceiver(context, false);
        } catch (Exception e)
        {

            Log.e(TAG, "getVolumeList not found, fallback");

        }

    }


    public static class Device extends File
    {
        String mUserLabel, mUuid, mState, mWriteState, mType;
        boolean mPrimary, mRemovable, mEmulated, mAllowMassStorage;
        long mMaxFileSize;
        File mFiles, mCache;


        Device(Context context)
        {
            super(Environment.getDataDirectory().getAbsolutePath());
            mState      = Environment.MEDIA_MOUNTED;
            mFiles      = context.getFilesDir();
            mCache      = context.getCacheDir();
            mType       = TYPE_INTERNAL;
            mWriteState = WRITE_APPONLY;
        }


        @SuppressWarnings("NullArgumentToVariableArgMethod")
        Device(Object storage) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
        {
            super((String) storage.getClass().getMethod("getPath", null).invoke(storage, null));
            for (Method m : storage.getClass().getMethods())
            {
                if (m.getName().equals("getUserLabel") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mUserLabel = (String) m.invoke(storage, null);
                if (m.getName().equals("getUuid") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mUuid = (String) m.invoke(storage, null);
                if (m.getName().equals("getState") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mState = (String) m.invoke(storage, null);
                if (m.getName().equals("isRemovable") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mRemovable = (Boolean) m.invoke(storage, null);
                if (m.getName().equals("isPrimary") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mPrimary = (Boolean) m.invoke(storage, null);
                if (m.getName().equals("isEmulated") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mEmulated = (Boolean) m.invoke(storage, null);
                if (m.getName().equals("allowMassStorage") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mAllowMassStorage = (Boolean) m.invoke(storage, null);
                if (m.getName().equals("getMaxFileSize") && m.getParameterTypes().length == 0 && m.getReturnType() == long.class)
                    mMaxFileSize = (Long) m.invoke(storage, null);


            }
            if (mState == null)
                mState = getState();

            if (mPrimary)
                mType = TYPE_PRIMARY;
            else
            {
                String n = getAbsolutePath().toLowerCase();
                if (n.indexOf("sd") > 0)
                    mType = TYPE_SD;
                else if (n.indexOf("usb") > 0)
                    mType = TYPE_USB;
                else
                    mType = TYPE_UNKNOWN + " " + getAbsolutePath();
            }
        }


        public String getType()
        {
            return mType;
        }


        public String getAccess()
        {
            if (mWriteState == null)
            {
                try
                {
                    mWriteState = WRITE_NONE;
                    File[] root = listFiles();
                    if (root == null || root.length == 0)
                        throw new IOException("root empty/unreadable");
                    mWriteState = WRITE_READONLY;
                    File t = File.createTempFile("jow", null, getFilesDir());

                    t.delete();
                    mWriteState = WRITE_APPONLY;
                    t           = File.createTempFile("jow", null, this);

                    t.delete();
                    mWriteState = WRITE_FULL;
                } catch (IOException ignore)
                {
                    Log.v(TAG, "test " + getAbsolutePath() + " ->" + mWriteState + "<- " + ignore.getMessage());
                }
            }
            return mWriteState;
        }


        public boolean isAvailable()
        {
            String s = getState();
            return (Environment.MEDIA_MOUNTED.equals(s) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(s));

        }


        public String getState()
        {
            if (mRemovable || mState == null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)

                    mState = Environment.getExternalStorageState(this);
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)

                    mState = Environment.getStorageState(this);
                else if (canRead() && getTotalSpace() > 0)

                    mState = Environment.MEDIA_MOUNTED;
                else if (mState == null || Environment.MEDIA_MOUNTED.equals(mState))

                    mState = EnvironmentCompat.MEDIA_UNKNOWN;
            }
            return mState;
        }

        public File getFilesDir()
        {
            if (mFiles == null)
            {
                mFiles = new File(this, userDir + "/files");
                if (!mFiles.isDirectory())

                    mFiles.mkdirs();
            }
            return mFiles;
        }

        public File getCacheDir()
        {
            if (mCache == null)
            {
                mCache = new File(this, userDir + "/cache");
                if (!mCache.isDirectory())

                    mCache.mkdirs();
            }
            return mCache;
        }


        public boolean isPrimary()
        {
            return mPrimary;
        }


        public boolean isRemovable()
        {
            return mRemovable;
        }


        public boolean isEmulated()
        {
            return mEmulated;
        }

        public boolean isAllowMassStorage()
        {
            return mAllowMassStorage;
        }

        public long getMaxFileSize()
        {
            return mMaxFileSize;
        }

        public String getUserLabel()
        {
            return mUserLabel;
        }

        public String getUuid()
        {
            return mUuid;
        }
    }
}
 
