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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.S)
public class ListenerTelephony extends TelephonyCallback implements TelephonyCallback.DisplayInfoListener, TelephonyCallback.ServiceStateListener, TelephonyCallback.SignalStrengthsListener, TelephonyCallback.DataConnectionStateListener, TelephonyCallback.DataActivityListener
{

    private final Context ctx;


    private final Tool             mTool;
    private final ModulesInterface interfaceCallback;
    private       ServiceState     serviceState;
    private       CellInfo         cellInfo;
    private final TelephonyManager tm;
    private       Thread           pThread;
    private       boolean          withIntervall         = false;
    private       int              dataNetworkId         = 0;
    private       int              overrideDataNetworkId = -1;
    private       int              dataNetworkState      = -1;

    private static final String TAG = "ListenerTelephony";


    public ListenerTelephony(Context ctx, ModulesInterface intCall)
    {
        this.ctx               = ctx;
        this.interfaceCallback = intCall;

        mTool = new Tool();

        TelephonyManager tmTemp = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        tm = tmTemp.createForSubscriptionId(dataSubId);
    }


    public void startUpdates()
    {
        tm.registerTelephonyCallback(ctx.getMainExecutor(), this);

        getData();
    }

    public ServiceState getServiceState()
    {
        return serviceState;
    }

    public void withIntervall()
    {
        pThread = new Thread(new ListenerTelephony.WorkerThread());
        pThread.start();

        withIntervall = true;
    }


    public void stopUpdates()
    {
        tm.unregisterTelephonyCallback(this);

        if (withIntervall)
            pThread.interrupt();

        withIntervall = false;
    }

    @Override
    public void onDataActivity(int i)
    {
        getData();
    }

    @Override
    public void onDataConnectionStateChanged(int networkState, int networkType)
    {
        dataNetworkId    = networkType;
        dataNetworkState = networkState;

        getData();
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo)
    {
        dataNetworkId         = telephonyDisplayInfo.getNetworkType();
        overrideDataNetworkId = telephonyDisplayInfo.getOverrideNetworkType();

        getData();
    }

    @Override
    public void onServiceStateChanged(@NonNull ServiceState serviceState)
    {
        this.serviceState = serviceState;

        getData();
    }

    @Override
    public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength)
    {
        getData();
    }

    private void triggerCellInfoUpdate()
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            tm.requestCellInfoUpdate(ctx.getMainExecutor(), new TelephonyManager.CellInfoCallback()
            {
                @Override
                public void onCellInfo(@NonNull List<CellInfo> cellInfos)
                {
                    for (CellInfo currentCellInfo : cellInfos)
                    {
                        if (currentCellInfo.isRegistered() && (cellInfo == null || cellInfo.getTimeStamp() < currentCellInfo.getTimeStamp()))
                        {
                            cellInfo = currentCellInfo;
                            getData();
                            return;
                        }
                    }
                    cellInfo = null;
                }
            });
        }
    }

    private void fetchCellInfo()
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        List<CellInfo> cellInfos = tm.getAllCellInfo();


        if (cellInfos == null || cellInfos.isEmpty())
            return;

        for (CellInfo currentCellInfo : cellInfos)
        {
            if (currentCellInfo.isRegistered() && (cellInfo == null || cellInfo.getTimestampMillis() < currentCellInfo.getTimestampMillis()))
            {
                cellInfo = currentCellInfo;
            }
        }
    }

    private void getData()
    {
        triggerCellInfoUpdate();

        JSONObject jData = new JSONObject();

        String app_operator_netcode = tm.getNetworkOperator();
        String app_operator_simcode = tm.getSimOperator();

        String app_carrier_sim    = String.valueOf(tm.getSimCarrierIdName());
        int    app_carrier_sim_id = tm.getSimCarrierId();

        String app_operator_sim = "";
        String app_operator_net = tm.getNetworkOperatorName();

        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY)
        {
            app_operator_sim = tm.getSimOperatorName();
        }

        try
        {
            jData.put("app_mode", mTool.getConnectionType(ctx));


            jData.put("app_data_state", dataNetworkState);
            jData.put("app_access_id", dataNetworkId);

            if (dataNetworkId == 13 && overrideDataNetworkId != -1 && overrideDataNetworkId != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE)
            {
                if (overrideDataNetworkId == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA)
                {
                    jData.put("app_access_id", 19);
                }

                if (overrideDataNetworkId == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED)
                {
                    jData.put("app_access_id", 20);
                }


            }

            jData.put("app_access", mTool.getNetType(jData.getInt("app_access_id")));


            if (serviceState != null)
            {
                jData.put("app_voice", mTool.getStateString(serviceState.getState()));
                jData.put("app_voice_id", getVoiceNetworkType(serviceState));

                jData.put("app_emergency_only", getEmergencyOnly(serviceState));
                jData.put("app_roaming", serviceState.getRoaming());
            }


            if (!app_operator_netcode.equals(""))
            {
                jData.put("app_operator_net_mcc", app_operator_netcode.substring(0, 3));
                jData.put("app_operator_net_mnc", app_operator_netcode.substring(3));
            }

            if (!app_operator_simcode.equals(""))
            {
                jData.put("app_operator_sim_mcc", app_operator_simcode.substring(0, 3));
                jData.put("app_operator_sim_mnc", app_operator_simcode.substring(3));
            }

            if (!app_operator_net.equals("") && !app_operator_net.equals("null"))
                jData.put("app_operator_net", app_operator_net);
            if (!app_operator_sim.equals("") && !app_operator_sim.equals("null"))
                jData.put("app_operator_sim", app_operator_sim);
            if (!app_carrier_sim.equals("") && !app_carrier_sim.equals("null"))
                jData.put("app_carrier_sim", app_carrier_sim);
            jData.put("app_carrier_sim_id", app_carrier_sim_id);


            fetchCellInfo();
            if (cellInfo != null)
            {
                CellIdentity       currentCellIdentity        = cellInfo.getCellIdentity();
                CellSignalStrength currenctCellSignalStrength = cellInfo.getCellSignalStrength();

                jData.put("app_rssi", getRssi(currenctCellSignalStrength));
                jData.put("app_rsrp", getRsrp(currenctCellSignalStrength));
                jData.put("app_rsrq", getRsrq(currenctCellSignalStrength));
                jData.put("app_arfcn", getArfcn(currentCellIdentity));
                jData.put("app_cellid", getCid(currentCellIdentity));
                jData.put("app_celllac", getLac(currentCellIdentity));
                jData.put("app_celltac", getTac(currentCellIdentity));
                jData.put("app_cellpci", getPci(currentCellIdentity));
            }


            jData.put("app_call_state", mTool.isCalling(ctx) ? "1" : "0");


            interfaceCallback.receiveData(jData);
        } catch (Exception ex)
        {
            Log.warning(TAG, "getData: error", jData, ex);
        }
    }

    public int getRssi(CellSignalStrength currenctCellSignalStrength)
    {
        int rssi = 0;
        if (currenctCellSignalStrength == null)
            return rssi;

        rssi = currenctCellSignalStrength.getDbm();
        if (rssi == CellInfo.UNAVAILABLE || rssi == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rssi;
    }

    public int getRsrp(CellSignalStrength currenctCellSignalStrength)
    {
        int rsrp = 0;
        if (currenctCellSignalStrength == null)
            return rsrp;

        if (currenctCellSignalStrength instanceof CellSignalStrengthLte)
        {
            rsrp = ((CellSignalStrengthLte) currenctCellSignalStrength).getRsrp();
        } else if (currenctCellSignalStrength instanceof CellSignalStrengthNr)
        {
            if (((CellSignalStrengthNr) currenctCellSignalStrength).getSsRsrp() != Integer.MAX_VALUE)
                rsrp = ((CellSignalStrengthNr) currenctCellSignalStrength).getSsRsrp();
            else
                rsrp = ((CellSignalStrengthNr) currenctCellSignalStrength).getCsiRsrp();
        }
        if (rsrp == CellInfo.UNAVAILABLE || rsrp == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rsrp;

    }

    public int getRsrq(CellSignalStrength currenctCellSignalStrength)
    {
        int rsrq = 0;
        if (currenctCellSignalStrength == null)
            return rsrq;

        if (currenctCellSignalStrength instanceof CellSignalStrengthLte)
        {
            rsrq = ((CellSignalStrengthLte) currenctCellSignalStrength).getRsrq();
        } else if (currenctCellSignalStrength instanceof CellSignalStrengthNr)
        {
            if (((CellSignalStrengthNr) currenctCellSignalStrength).getSsRsrq() != Integer.MAX_VALUE)
                rsrq = ((CellSignalStrengthNr) currenctCellSignalStrength).getSsRsrq();
            else
                rsrq = ((CellSignalStrengthNr) currenctCellSignalStrength).getCsiRsrq();

        }
        if (rsrq == CellInfo.UNAVAILABLE || rsrq == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rsrq;

    }

    private int getArfcn(CellIdentity currentCellIdentity)
    {
        int arfcn = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return arfcn;
        }

        if (currentCellIdentity == null)
            return arfcn;


        if (currentCellIdentity instanceof CellIdentityGsm)
        {
            arfcn = ((CellIdentityGsm) currentCellIdentity).getArfcn();
        } else if (currentCellIdentity instanceof CellIdentityWcdma)
        {
            arfcn = ((CellIdentityWcdma) currentCellIdentity).getUarfcn();
        } else if (currentCellIdentity instanceof CellIdentityLte)
        {
            arfcn = ((CellIdentityLte) currentCellIdentity).getEarfcn();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (currentCellIdentity instanceof CellIdentityNr)
            {
                arfcn = ((CellIdentityNr) currentCellIdentity).getNrarfcn();
            }
        }

        if (arfcn == CellInfo.UNAVAILABLE || arfcn == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return arfcn;
    }

    private long getCid(CellIdentity currentCellIdentity)
    {
        long cellid = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return cellid;
        }

        if (currentCellIdentity == null)
            return cellid;

        if (currentCellIdentity instanceof CellIdentityGsm)
        {
            cellid = ((CellIdentityGsm) currentCellIdentity).getCid();
        } else if (currentCellIdentity instanceof CellIdentityWcdma)
        {
            cellid = ((CellIdentityWcdma) currentCellIdentity).getCid();
        } else if (currentCellIdentity instanceof CellIdentityLte)
        {
            cellid = ((CellIdentityLte) currentCellIdentity).getCi();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (currentCellIdentity instanceof CellIdentityNr)
            {
                cellid = ((CellIdentityNr) currentCellIdentity).getNci();
            }
        }

        if (cellid == CellInfo.UNAVAILABLE || cellid == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return cellid;
    }

    private int getLac(CellIdentity currentCellIdentity)
    {

        int lac = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return lac;
        }

        if (currentCellIdentity == null)
            return lac;

        if (currentCellIdentity instanceof CellIdentityGsm)
        {
            lac = ((CellIdentityGsm) currentCellIdentity).getLac();
        } else if (currentCellIdentity instanceof CellIdentityWcdma)
        {
            lac = ((CellIdentityWcdma) currentCellIdentity).getLac();
        } else if (currentCellIdentity instanceof CellIdentityLte)
        {
            lac = ((CellIdentityLte) currentCellIdentity).getTac();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (currentCellIdentity instanceof CellIdentityNr)
            {
                lac = ((CellIdentityNr) currentCellIdentity).getTac();
            }
        }

        if (lac == CellInfo.UNAVAILABLE || lac == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return lac;
    }

    private int getTac(CellIdentity currentCellIdentity)
    {
        int tac = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return tac;
        }

        if (currentCellIdentity == null)
            return tac;

        if (currentCellIdentity instanceof CellIdentityLte)
        {
            tac = ((CellIdentityLte) currentCellIdentity).getTac();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (currentCellIdentity instanceof CellIdentityNr)
            {
                tac = ((CellIdentityNr) currentCellIdentity).getTac();
            }
        }
        if (tac == CellInfo.UNAVAILABLE || tac == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return tac;

    }

    private int getPci(CellIdentity currentCellIdentity)
    {
        int pci = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return pci;
        }

        if (currentCellIdentity == null)
            return pci;

        if (currentCellIdentity instanceof CellIdentityLte)
        {
            pci = ((CellIdentityLte) currentCellIdentity).getPci();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (currentCellIdentity instanceof CellIdentityNr)
            {
                pci = ((CellIdentityNr) currentCellIdentity).getPci();
            }
        }
        if (pci == CellInfo.UNAVAILABLE || pci == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return pci;
    }

    private int getVoiceNetworkType(ServiceState serviceState)
    {
        try
        {


            Class  c  = Class.forName(serviceState.getClass().getName());
            Method mI = c.getDeclaredMethod("getRilVoiceRadioTechnology");

            mI.setAccessible(true);

            return (int) (Integer) mI.invoke(serviceState);
        } catch (Exception ignored)
        {
        }

        return -1;
    }

    private boolean getEmergencyOnly(ServiceState serviceState)
    {
        try
        {


            Class  c  = Class.forName(serviceState.getClass().getName());
            Method mI = c.getDeclaredMethod("isEmergencyOnly");

            mI.setAccessible(true);

            return (boolean) (Boolean) mI.invoke(serviceState);
        } catch (Exception ignored)
        {
        }

        return false;
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
                    Log.info(TAG, "ListenerNetwork-Thread interrupted", ex);
                    break;
                }
            }
        }
    }
}
