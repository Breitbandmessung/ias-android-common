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

package com.zafaco.moduleCommon.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;

import org.json.JSONObject;

public class ListenerWireless extends BroadcastReceiver
{


    Context ctx;


    private final Tool             mTool;
    private final ModulesInterface interfaceCallback;
    private       WifiManager      wm;
    private       Thread           pThread;
    private       boolean          withIntervall = false;

    private static final String TAG = "ListenerWireless";


    private final String sState = "COMPLETED";


    public ListenerWireless(Context ctx, ModulesInterface intCall)
    {
        this.ctx               = ctx;
        this.interfaceCallback = intCall;

        mTool = new Tool();

        wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        ctx.registerReceiver(this, iFilter);

    }

    public void getState()
    {
        onReceive(ctx, new Intent());
    }

    public void withIntervall()
    {
        pThread = new Thread(new WorkerThread());
        pThread.start();

        withIntervall = true;
    }

    public void stopUpdates()
    {
        ctx.unregisterReceiver(this);

        if (withIntervall)
            pThread.interrupt();

        withIntervall = false;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        getData();
    }

    private void getData()
    {
        JSONObject jData = new JSONObject();

        try
        {
            wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);


            jData.put("app_mode", mTool.isWifi(ctx) ? "WIFI" : "WWAN");


            interfaceCallback.receiveData(jData);
        } catch (Exception ex)
        {
            Log.warning(TAG, "getData: error", jData, ex);
        }
    }

    class WorkerThread extends Thread
    {
        public void run()
        {
            while (true)
            {
                try
                {
                    getData();


                    Thread.sleep(10000);
                } catch (InterruptedException ex)
                {
                    Log.info(TAG, "ListenerWireless-Thread interrupted", ex);
                    break;
                }
            }
        }
    }
}
