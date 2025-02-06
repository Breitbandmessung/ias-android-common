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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthNr;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

public class ListenerNetwork extends PhoneStateListener
{


    private final Context ctx;


    private final Tool             mTool;
    private final ModulesInterface interfaceCallback;
    private       ServiceState     serviceState;
    private       CellInfo         cellInfo;
    private final TelephonyManager tm;
    private       Thread           pThread;
    private       boolean          withIntervall    = false;
    private       int              dataNetworkId    = 0;
    private       int              dataNetworkState = -1;

    private static final String TAG = "ListenerNetwork";


    public ListenerNetwork(Context ctx, ModulesInterface intCall)
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

        int flags = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_DATA_ACTIVITY | PhoneStateListener.LISTEN_SERVICE_STATE;

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            flags = flags | PhoneStateListener.LISTEN_CELL_LOCATION;
        }

        tm.listen(this, flags);

        getData();
    }

    public ServiceState getServiceState()
    {
        return serviceState;
    }

    public void withIntervall()
    {
        pThread = new Thread(new WorkerThread());
        pThread.start();

        withIntervall = true;
    }


    public void stopUpdates()
    {
        tm.listen(this, PhoneStateListener.LISTEN_NONE);

        if (withIntervall)
            pThread.interrupt();

        withIntervall = false;
    }


    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength)
    {
        super.onSignalStrengthsChanged(signalStrength);
        getData();
    }


    @Override
    public void onDataConnectionStateChanged(int networkState, int networkType)
    {
        dataNetworkId    = networkType;
        dataNetworkState = networkState;

        super.onDataConnectionStateChanged(networkState, networkType);

        getData();
    }


    @Override
    public void onDataActivity(int directon)
    {
        super.onDataActivity(directon);

        getData();
    }


    @Override
    public void onCellLocationChanged(CellLocation location)
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        super.onCellLocationChanged(location);

        getData();
    }


    @Override
    public void onServiceStateChanged(ServiceState serviceState)
    {
        super.onServiceStateChanged(serviceState);

        this.serviceState = serviceState;

        getData();
    }


    private void triggerCellInfoUpdate()
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            long startTime = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                tm.requestCellInfoUpdate(ctx.getMainExecutor(), new TelephonyManager.CellInfoCallback()
                {
                    @Override
                    public void onCellInfo(@NonNull List<CellInfo> cellInfos)
                    {
                        Log.debug(TAG, "CellInfoUpdate took " + (System.currentTimeMillis() - startTime) + "ms");
                        for (CellInfo currentCellInfo : cellInfos)
                        {
                            if (currentCellInfo.isRegistered())
                            {
                                cellInfo = currentCellInfo;
                                return;
                            }
                        }
                        cellInfo = null;
                    }
                });
            }
        }
    }

    private void getData()
    {
        triggerCellInfoUpdate();
        JSONObject jData = new JSONObject();

        String app_operator_netcode = tm.getNetworkOperator();
        String app_operator_simcode = tm.getSimOperator();

        String app_carrier_sim    = "";
        int    app_carrier_sim_id = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
        {
            app_carrier_sim    = String.valueOf(tm.getSimCarrierIdName());
            app_carrier_sim_id = tm.getSimCarrierId();
        }

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
            jData.put("app_access_id_debug", dataNetworkId);

            ServiceState serviceState = getServiceState();

            int    adjustedId = -1;
            String sInfo      = "";

            if (dataNetworkId == 13 && serviceState != null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                {
                    for (NetworkRegistrationInfo info : serviceState.getNetworkRegistrationInfoList())
                    {
                        if (info.getAvailableServices().contains(NetworkRegistrationInfo.SERVICE_TYPE_DATA))
                        {
                            sInfo      = info.toString();
                            adjustedId = mTool.parseNetworkRegistrationInfo(sInfo);
                        }
                    }
                } else
                {
                    sInfo      = serviceState.toString();
                    adjustedId = mTool.parseNetworkRegistrationInfo(sInfo);
                }
            }

            if (adjustedId != -1)
            {
                jData.put("app_access_id_debug", adjustedId);


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
                CellInfo currentCellInfo = cellInfo;

                jData.put("app_rssi", getRssi(currentCellInfo));
                jData.put("app_rsrp", getRsrp(currentCellInfo));
                jData.put("app_rsrq", getRsrq(currentCellInfo));
                jData.put("app_arfcn", getArfcn(currentCellInfo));
                jData.put("app_cellid", getCid(currentCellInfo));
                jData.put("app_celllac", getLac(currentCellInfo));
                jData.put("app_cellpci", getPci(currentCellInfo));
            }


            jData.put("app_call_state", mTool.isCalling(ctx) ? "1" : "0");


            interfaceCallback.receiveData(jData);
        } catch (Exception ex)
        {
            Log.warning(TAG, "getData: error", jData, ex);
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
            if (currentCellInfo.isRegistered() && (cellInfo == null || cellInfo.getTimeStamp() < currentCellInfo.getTimeStamp()))
            {
                cellInfo = currentCellInfo;
            }
        }
    }

    public int getRssi(CellInfo currentCellInfo)
    {
        int rssi = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoGsm)
            {
                rssi = ((CellInfoGsm) currentCellInfo).getCellSignalStrength().getDbm();
            } else if (currentCellInfo instanceof CellInfoCdma)
            {
                rssi = ((CellInfoCdma) currentCellInfo).getCellSignalStrength().getDbm();
            } else if (currentCellInfo instanceof CellInfoWcdma)
            {
                rssi = ((CellInfoWcdma) currentCellInfo).getCellSignalStrength().getDbm();
            } else if (currentCellInfo instanceof CellInfoLte)
            {
                rssi = ((CellInfoLte) currentCellInfo).getCellSignalStrength().getDbm();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    rssi = currentCellInfo.getCellSignalStrength().getDbm();
                }
            }
        }

        if (rssi == CellInfo.UNAVAILABLE || rssi == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rssi;
    }


    public int getRsrp(CellInfo currentCellInfo)
    {
        int rsrp = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoLte)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    rsrp = ((CellInfoLte) currentCellInfo).getCellSignalStrength().getRsrp();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    CellSignalStrengthNr ss = (CellSignalStrengthNr) currentCellInfo.getCellSignalStrength();
                    if (ss.getSsRsrp() != Integer.MAX_VALUE)
                        rsrp = ss.getSsRsrp();
                    else
                        rsrp = ss.getCsiRsrp();
                }
            }
        }

        if (rsrp == CellInfo.UNAVAILABLE || rsrp == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rsrp;
    }

    public int getRsrq(CellInfo currentCellInfo)
    {
        int rsrq = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoLte)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    rsrq = ((CellInfoLte) currentCellInfo).getCellSignalStrength().getRsrq();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    CellSignalStrengthNr ss = (CellSignalStrengthNr) currentCellInfo.getCellSignalStrength();
                    if (ss.getSsRsrq() != Integer.MAX_VALUE)
                        rsrq = ss.getSsRsrq();
                    else
                        rsrq = ss.getCsiRsrq();
                }
            }
        }

        if (rsrq == CellInfo.UNAVAILABLE || rsrq == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return rsrq;
    }

    private int getArfcn(CellInfo currentCellInfo)
    {
        int arfcn = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoGsm)
            {
                arfcn = ((CellInfoGsm) currentCellInfo).getCellIdentity().getArfcn();
            } else if (currentCellInfo instanceof CellInfoWcdma)
            {
                arfcn = ((CellInfoWcdma) currentCellInfo).getCellIdentity().getUarfcn();
            } else if (currentCellInfo instanceof CellInfoLte)
            {
                arfcn = ((CellInfoLte) currentCellInfo).getCellIdentity().getEarfcn();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    arfcn = ((CellIdentityNr) currentCellInfo.getCellIdentity()).getNrarfcn();
                }
            }
        }

        if (arfcn == CellInfo.UNAVAILABLE || arfcn == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return arfcn;
    }

    private long getCid(CellInfo currentCellInfo)
    {
        long cellid = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoGsm)
            {
                cellid = ((CellInfoGsm) currentCellInfo).getCellIdentity().getCid();
            } else if (currentCellInfo instanceof CellInfoWcdma)
            {
                cellid = ((CellInfoWcdma) currentCellInfo).getCellIdentity().getCid();
            } else if (currentCellInfo instanceof CellInfoLte)
            {
                cellid = ((CellInfoLte) currentCellInfo).getCellIdentity().getCi();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    cellid = ((CellIdentityNr) currentCellInfo.getCellIdentity()).getNci();
                }
            }
        }


        if (cellid == CellInfo.UNAVAILABLE || cellid == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return cellid;
    }

    private int getLac(CellInfo currentCellInfo)
    {
        int lac = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoGsm)
            {
                lac = ((CellInfoGsm) currentCellInfo).getCellIdentity().getLac();
            } else if (currentCellInfo instanceof CellInfoWcdma)
            {
                lac = ((CellInfoWcdma) currentCellInfo).getCellIdentity().getLac();
            } else if (currentCellInfo instanceof CellInfoLte)
            {
                lac = ((CellInfoLte) currentCellInfo).getCellIdentity().getTac();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    lac = ((CellIdentityNr) currentCellInfo.getCellIdentity()).getTac();
                }
            }
        }

        if (lac == CellInfo.UNAVAILABLE || lac == CellInfo.UNAVAILABLE_LONG)
            return 0;

        return lac;
    }

    private int getPci(CellInfo currentCellInfo)
    {
        int pci = 0;

        if (currentCellInfo != null && currentCellInfo.isRegistered())
        {
            if (currentCellInfo instanceof CellInfoLte)
            {
                pci = ((CellInfoLte) currentCellInfo).getCellIdentity().getPci();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                if (currentCellInfo instanceof CellInfoNr)
                {
                    pci = ((CellIdentityNr) currentCellInfo.getCellIdentity()).getPci();
                }
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


            Class<?> c  = Class.forName(serviceState.getClass().getName());
            Method   mI = c.getDeclaredMethod("getRilVoiceRadioTechnology");

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
