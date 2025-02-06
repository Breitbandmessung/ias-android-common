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

package com.zafaco.moduleCommon.models.network;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;
import com.zafaco.moduleCommon.interfaces.ResultInfo;
import com.zafaco.moduleCommon.listener.ListenerNetwork;
import com.zafaco.moduleCommon.listener.ListenerTelephony;
import com.zafaco.moduleCommon.models.measurement.AbstractMeasurementResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class NetworkInfo implements ResultInfo
{

    private static final String TAG = "NETWORK_INFO";

    private int rssi;
    private int rsrp;
    private int rsrq;
    private int sinr;
    private int arfcn;

    private String  operatorNetId = "-1";
    private String  operatorNet;
    private int     operatorNetMcc;
    private int     operatorNetMnc;
    private boolean operatorNetChanged;
    private int     operatorSimMcc;
    private int     operatorSimMnc;
    private String  operatorSimId = "-1";
    private String  operatorSim;
    private int     carrierSimId  = -1;
    private String  carrierSim;

    private int simState;
    private int simsActive;

    private int callState;
    private int dataNetworkState;

    private String simCountryIso;
    private String networkCountryIso;

    private int     phoneType;
    private String  connectionType;
    private String  connectionTypeDownloadStart;
    private boolean connectionTypeDownloadChanged = false;
    private String  connectionTypeUploadStart;
    private boolean connectionTypeUploadChanged   = false;
    private String  connectionTypeRttStart;
    private boolean connectionTypeRttChanged      = false;
    private int     dataNetworkId                 = 0;
    private String  dataNetwork                   = "unknown";

    private int     dataNetworkDownloadStart   = -1;
    private boolean dataNetworkDownloadChanged = false;
    private int     dataNetworkUploadStart     = -1;
    private boolean dataNetworkUploadChanged   = false;
    private int     dataNetworkRttStart        = -1;
    private boolean dataNetworkRttChanged      = false;
    private long    cellId;
    private int     cellLac;
    private int     cellPci;

    private int tetheringState;
    private int roamingState;

    private ListenerTelephony                  listenerTelephony;
    private ListenerNetwork                    listenerNetwork;
    private AbstractMeasurementResult.TestCase testCase;

    public NetworkInfo()
    {
    }

    public NetworkInfo(Context ctx)
    {
        TelephonyManager tmTemp    = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        int              dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        TelephonyManager tm        = tmTemp.createForSubscriptionId(dataSubId);

        Tool tool = new Tool();
        setOperatorNet(tm.getNetworkOperatorName());
        setOperatorNetId(tm.getNetworkOperator());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            setCarrierSimId(tm.getSimCarrierId());
            if (tm.getSimCarrierIdName() != null)
                setCarrierSim(tm.getSimCarrierIdName().toString());
        }

        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY)
        {
            setOperatorSimId(tm.getSimOperator());
            setOperatorSim(tm.getSimOperatorName());
        }


        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                setDataNetworkId(tm.getDataNetworkType());
            } else
                setDataNetworkId(tm.getNetworkType());

            setDataNetwork(tool.getNetType(getDataNetworkId()));
        } else
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        MyTelephonyCallback telephonyCallback = new MyTelephonyCallback(tm);
                        tm.registerTelephonyCallback(ctx.getMainExecutor(), telephonyCallback);
                    } else
                    {
                        Looper.prepare();
                        PhoneStateListener listener = new PhoneStateListener()
                        {
                            @Override
                            public void onDataConnectionStateChanged(int networkState, int networkType)
                            {
                                setDataNetworkId(networkType);
                                setDataNetwork(tool.getNetType(getDataNetworkId()));
                                super.onDataConnectionStateChanged(networkState, networkType);
                                Looper.myLooper().quit();
                            }
                        };

                        tm.listen(listener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                        Looper.loop();
                        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
                    }

                }
            }).start();
        }

        setSimState(tm.getSimState());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        {
            setCallState(tm.getCallState());
        } else if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
        {
            setCallState(tm.getCallStateForSubscription());
        }
        setDataNetworkState(tm.getDataState());

        setPhoneType(tm.getPhoneType());
        setSimCountryIso(tm.getSimCountryIso());
        setNetworkCountryIso(tm.getNetworkCountryIso());


        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            List<CellInfo> cellInfoList = tm.getAllCellInfo();
            getCellInfoData(cellInfoList);


        }

        simsActive = 0;
        for (int i = 0; i < 10; i++)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                if (tm.getSimState(i) == TelephonyManager.SIM_STATE_READY)
                    simsActive++;
            }
        }

        setConnectionType(tool.getConnectionType(ctx));
    }

    public int getRssi()
    {
        return rssi;
    }

    public void setRssi(int rssi)
    {
        this.rssi = rssi;
    }

    public int getRsrp()
    {
        return rsrp;
    }

    public void setRsrp(int rsrp)
    {
        this.rsrp = rsrp;
    }

    public int getRsrq()
    {
        return rsrq;
    }

    public void setRsrq(int rsrq)
    {
        this.rsrq = rsrq;
    }

    public int getSinr()
    {
        return sinr;
    }

    public void setSinr(int sinr)
    {
        this.sinr = sinr;
    }

    public int getArfcn()
    {
        return arfcn;
    }

    public void setArfcn(int arfcn)
    {
        this.arfcn = arfcn;
    }

    public int getSimsActive()
    {
        return simsActive;
    }

    public void setSimsActive(int simsActive)
    {
        this.simsActive = simsActive;
    }

    public int getCellPci()
    {
        return cellPci;
    }

    public void setCellPci(int cellPci)
    {
        this.cellPci = cellPci;
    }

    public int getTetheringState()
    {
        return tetheringState;
    }

    public void setTetheringState(int tetheringState)
    {
        this.tetheringState = tetheringState;
    }

    public int getRoamingState()
    {
        return roamingState;
    }

    public void setRoamingState(int roamingState)
    {
        this.roamingState = roamingState;
    }

    public String getOperatorNetId()
    {
        return operatorNetId;
    }

    public void setOperatorNetId(String operatorNetId)
    {
        this.operatorNetId = operatorNetId;
        if (operatorNetId != null && !operatorNetId.isEmpty())
        {
            setOperatorNetMcc(Integer.parseInt(operatorNetId.substring(0, 3)));
            setOperatorNetMnc(Integer.parseInt(operatorNetId.substring(3, 5)));
        }
    }

    public String getOperatorNet()
    {
        return operatorNet;
    }

    public void setOperatorNet(String operatorNet)
    {
        this.operatorNet = operatorNet;
    }

    public void setOperatorNetAll(String operatorNet, int operatorNetMcc, int operatorNetMnc)
    {
        if (!operatorNet.equals(this.operatorNet) || operatorNetMcc != this.operatorNetMcc || operatorNetMnc != this.operatorNetMnc)
        {
            operatorNetChanged = true;
        }
        setOperatorNet(operatorNet);
        setOperatorNetMnc(operatorNetMnc);
        setOperatorNetMcc(operatorNetMcc);
    }

    public String getOperatorSimId()
    {
        return operatorSimId;
    }

    public void setOperatorSimId(String operatorSimId)
    {
        this.operatorSimId = operatorSimId;
        try
        {
            setOperatorSimMcc(Integer.parseInt(operatorSimId.substring(0, 3)));
            setOperatorSimMnc(Integer.parseInt(operatorSimId.substring(3, 5)));
        } catch (Exception ignored)
        {
        }
    }

    public String getOperatorSim()
    {
        return operatorSim;
    }

    public void setOperatorSim(String operatorSim)
    {
        this.operatorSim = operatorSim;
    }

    public int getOperatorNetMcc()
    {
        return operatorNetMcc;
    }

    public void setOperatorNetMcc(int operatorNetMcc)
    {
        this.operatorNetMcc = operatorNetMcc;
    }

    public int getOperatorNetMnc()
    {
        return operatorNetMnc;
    }

    public void setOperatorNetMnc(int operatorNetMnc)
    {
        this.operatorNetMnc = operatorNetMnc;
    }

    public int getOperatorSimMcc()
    {
        return operatorSimMcc;
    }

    public void setOperatorSimMcc(int operatorSimMcc)
    {
        this.operatorSimMcc = operatorSimMcc;
    }

    public int getOperatorSimMnc()
    {
        return operatorSimMnc;
    }

    public void setOperatorSimMnc(int operatorSimMnc)
    {
        this.operatorSimMnc = operatorSimMnc;
    }

    public int getCarrierSimId()
    {
        return carrierSimId;
    }

    public void setCarrierSimId(int carrierSimId)
    {
        this.carrierSimId = carrierSimId;
    }

    public String getCarrierSim()
    {
        return carrierSim;
    }

    public void setCarrierSim(String carrierSim)
    {
        this.carrierSim = carrierSim;
    }

    public int getDataNetworkId()
    {
        return dataNetworkId;
    }

    public void setDataNetworkId(int dataNetworkId)
    {
        this.dataNetworkId = dataNetworkId;
        setNetworkIdChanged(dataNetworkId, testCase);
    }

    public String getDataNetwork()
    {
        return dataNetwork;
    }

    public void setDataNetwork(String dataNetwork)
    {
        this.dataNetwork = dataNetwork;
    }

    public int getDataNetworkDownloadStart()
    {
        return dataNetworkDownloadStart;
    }

    public void setDataNetworkDownloadStart(int dataNetworkDownloadStart)
    {
        this.dataNetworkDownloadStart = dataNetworkDownloadStart;
    }

    public boolean isDataNetworkDownloadChanged()
    {
        return dataNetworkDownloadChanged;
    }

    public void setDataNetworkDownloadChanged(boolean dataNetworkDownloadChanged)
    {
        this.dataNetworkDownloadChanged = dataNetworkDownloadChanged;
    }

    public int getDataNetworkUploadStart()
    {
        return dataNetworkUploadStart;
    }

    public void setDataNetworkUploadStart(int dataNetworkUploadStart)
    {
        this.dataNetworkUploadStart = dataNetworkUploadStart;
    }

    public boolean isDataNetworkUploadChanged()
    {
        return dataNetworkUploadChanged;
    }

    public void setDataNetworkUploadChanged(boolean dataNetworkUploadChanged)
    {
        this.dataNetworkUploadChanged = dataNetworkUploadChanged;
    }

    public void setOperatorNetChanged(boolean operatorNetChanged)
    {
        this.operatorNetChanged = operatorNetChanged;
    }

    public long getCellId()
    {
        return cellId;
    }

    public void setCellId(long cellId)
    {
        this.cellId = cellId;
    }

    public int getCellLac()
    {
        return cellLac;
    }

    public void setCellLac(int cellLac)
    {
        this.cellLac = cellLac;
    }

    public boolean isOperatorNetChanged()
    {
        return operatorNetChanged;
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jData = new JSONObject();

        try
        {
            jData.put("operator_net_id", getOperatorNetId());
            jData.put("operator_net_mcc", getOperatorNetMcc());
            jData.put("operator_net_mnc", getOperatorNetMnc());
            jData.put("operator_net", getOperatorNet());
            if (isOperatorNetChanged())
                jData.put("operator_net_changed", isOperatorNetChanged());
            jData.put("operator_sim_id", getOperatorSimId());
            jData.put("operator_sim_mcc", getOperatorSimMcc());
            jData.put("operator_sim_mnc", getOperatorSimMnc());
            jData.put("operator_sim", getOperatorSim());
            jData.put("carrier_sim_id", getCarrierSimId());
            jData.put("carrier_sim", getCarrierSim());

            jData.put("cell_lac", getCellLac());
            jData.put("cell_id", getCellId());
            jData.put("cell_rssi", getRssi());
            jData.put("cell_rsrp", getRsrp());
            jData.put("cell_rsrq", getRsrq());
            jData.put("cell_pci", getCellPci());
            jData.put("cell_arfcn", getArfcn());


            jData.put("call_state", getCallState());
            jData.put("sim_state", getSimState());
            jData.put("sims_active", getSimsActive());
            jData.put("data_state", getDataNetworkState());
            jData.put("roaming_state", getRoamingState());
            jData.put("tethering_state", getTetheringState());

            jData.put("sim_country_iso", getSimCountryIso());
            jData.put("network_country_iso", getNetworkCountryIso());
            jData.put("phone_type", getPhoneType());

            jData.put("connection_type", getConnectionType());
            jData.put("connection_type_download_start", getConnectionTypeDownloadStart());
            if (isConnectionTypeDownloadChanged())
                jData.put("connection_type_download_changed", isConnectionTypeDownloadChanged());
            jData.put("connection_type_upload_start", getConnectionTypeUploadStart());
            if (isConnectionTypeUploadChanged())
                jData.put("connection_type_upload_changed", isConnectionTypeUploadChanged());
            jData.put("connection_type_rtt_start", getConnectionTypeRttStart());
            if (isConnectionTypeRttChanged())
                jData.put("connection_type_rtt_changed", isConnectionTypeRttChanged());

            jData.put("network", getDataNetwork());
            if (isDataNetworkDownloadChanged())
                jData.put("network_id_download_changed", isDataNetworkDownloadChanged());
            if (getDataNetworkDownloadStart() != -1)
                jData.put("network_id_download_start", getDataNetworkDownloadStart());
            if (isDataNetworkUploadChanged())
                jData.put("network_id_upload_changed", isDataNetworkUploadChanged());
            if (getDataNetworkUploadStart() != -1)
                jData.put("network_id_upload_start", getDataNetworkUploadStart());
            if (isDataNetworkRttChanged())
                jData.put("network_id_rtt_changed", isDataNetworkRttChanged());
            if (getDataNetworkRttStart() != -1)
                jData.put("network_id_rtt_start", getDataNetworkRttStart());
            jData.put("network_id", getDataNetworkId());

        } catch (JSONException ignored)
        {
        }

        return jData;
    }


    private void getCellInfoData(List<CellInfo> cellInfoList)
    {
        if (cellInfoList != null)
        {
            for (final CellInfo info : cellInfoList)
            {
                if (info instanceof CellInfoGsm)
                {
                    if (info.isRegistered())
                        getGsmData((CellInfoGsm) info);
                } else if (info instanceof CellInfoLte)
                {
                    if (info.isRegistered())
                        getLteData((CellInfoLte) info);
                } else if (info instanceof CellInfoCdma)
                {
                    if (info.isRegistered())
                        getCdmaData((CellInfoCdma) info);
                } else if (info instanceof CellInfoWcdma)
                {
                    if (info.isRegistered())
                        getWcdmaData((CellInfoWcdma) info);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    if (info instanceof CellInfoTdscdma)
                    {
                        if (info.isRegistered())
                            getTdscdmaData((CellInfoTdscdma) info);
                    } else if (info instanceof CellInfoNr)
                    {
                        if (info.isRegistered())
                            getNrData((CellInfoNr) info);
                    }
                } else
                {
                    Log.warning(TAG, "getCellInfoData: unsupported CellInfo");
                }
            }

        }

    }

    private void getLteData(@NonNull CellInfoLte cellInfoLte)
    {
        final CellSignalStrengthLte signalStrengthLte = cellInfoLte.getCellSignalStrength();
        final CellIdentityLte       identityLte       = cellInfoLte.getCellIdentity();

        setCellId(identityLte.getCi());
        setCellLac(identityLte.getTac());
        setRssi(signalStrengthLte.getDbm());
        setCellPci(identityLte.getPci());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            setArfcn(identityLte.getEarfcn());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            setRsrp(signalStrengthLte.getRsrp());
            setRsrq(signalStrengthLte.getRsrq());
            setSinr(signalStrengthLte.getRssnr());
        }

    }


    private void getGsmData(@NonNull CellInfoGsm cellInfoGsm)
    {
        final CellSignalStrengthGsm signalStrengthGsm = cellInfoGsm.getCellSignalStrength();
        final CellIdentityGsm       identityGsm       = cellInfoGsm.getCellIdentity();

        setCellId(identityGsm.getCid());
        setCellLac(identityGsm.getLac());
        setRssi(signalStrengthGsm.getDbm());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            setArfcn(identityGsm.getArfcn());
        }

    }


    private void getCdmaData(@NonNull CellInfoCdma cellInfoCdma)
    {
        final CellSignalStrengthCdma signalStrengthCdma = cellInfoCdma.getCellSignalStrength();
        final CellIdentityCdma       identityCdma       = cellInfoCdma.getCellIdentity();

        setCellId(identityCdma.getBasestationId());
        setRssi(signalStrengthCdma.getDbm());

    }


    private void getWcdmaData(@NonNull CellInfoWcdma cellInfoWcdma)
    {
        final CellSignalStrengthWcdma signalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
        final CellIdentityWcdma       identityWcdma       = cellInfoWcdma.getCellIdentity();


        setCellId(identityWcdma.getCid());
        setCellLac(identityWcdma.getLac());
        setRssi(signalStrengthWcdma.getDbm());

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getTdscdmaData(@NonNull CellInfoTdscdma cellInfoTdscdma)
    {
        final CellSignalStrengthTdscdma signalStrengthTdscdma = cellInfoTdscdma.getCellSignalStrength();
        final CellIdentityTdscdma       identityTdscdma       = cellInfoTdscdma.getCellIdentity();

        setCellId(identityTdscdma.getCid());
        setCellLac(identityTdscdma.getLac());
        setRssi(signalStrengthTdscdma.getDbm());

    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getNrData(@NonNull CellInfoNr cellInfoNr)
    {

        final CellSignalStrengthNr signalStrengthNr = (CellSignalStrengthNr) cellInfoNr.getCellSignalStrength();
        final CellIdentityNr       identityNr       = (CellIdentityNr) cellInfoNr.getCellIdentity();

        setCellId(identityNr.getNci());
        setCellLac(identityNr.getTac());
        setRssi(signalStrengthNr.getDbm());
        setRsrp(signalStrengthNr.getCsiRsrp());
        setCellPci(identityNr.getPci());
    }

    public void setNetworkIdChanged(int networkId, AbstractMeasurementResult.TestCase testcase)
    {
        if (testcase != null)
        {
            switch (testcase)
            {
                case DOWNLOAD:
                {
                    if (!dataNetworkDownloadChanged)
                        setDataNetworkDownloadChanged(dataNetworkDownloadStart != networkId);
                    break;
                }
                case UPLOAD:
                {
                    if (!dataNetworkUploadChanged)
                        setDataNetworkUploadChanged(dataNetworkUploadStart != networkId);
                    break;
                }
                case RTT_UDP:
                {
                    if (!dataNetworkRttChanged)
                        setDataNetworkRttChanged(dataNetworkRttStart != networkId);
                    break;
                }
            }
        }
    }

    public void setConnectionTypeChanged(String connectionType, AbstractMeasurementResult.TestCase testcase)
    {
        if (testcase != null)
        {
            switch (testcase)
            {
                case DOWNLOAD:
                {
                    if (!connectionTypeDownloadChanged)
                        setConnectionTypeDownloadChanged(!connectionTypeDownloadStart.equals(connectionType));
                    break;
                }
                case UPLOAD:
                {
                    if (!connectionTypeUploadChanged)
                        setConnectionTypeUploadChanged(!connectionTypeUploadStart.equals(connectionType));
                    break;
                }
                case RTT_UDP:
                {
                    if (!connectionTypeRttChanged)
                        setConnectionTypeRttChanged(!connectionTypeRttStart.equals(connectionType));
                    break;
                }
            }
        }
    }

    public int getSimState()
    {
        return simState;
    }

    public void setSimState(int simState)
    {
        this.simState = simState;
    }

    public String getSimCountryIso()
    {
        return simCountryIso;
    }

    public void setSimCountryIso(String simCountryIso)
    {
        this.simCountryIso = simCountryIso;
    }

    public int getCallState()
    {
        return callState;
    }

    public void setCallState(int callState)
    {
        this.callState = callState;
    }

    public int getDataNetworkState()
    {
        return dataNetworkState;
    }

    public void setDataNetworkState(int dataNetworkState)
    {
        this.dataNetworkState = dataNetworkState;
    }

    public int getPhoneType()
    {
        return phoneType;
    }

    public void setPhoneType(int phoneType)
    {
        this.phoneType = phoneType;
    }

    public String getNetworkCountryIso()
    {
        return networkCountryIso;
    }

    public void setNetworkCountryIso(String networkCountryIso)
    {
        this.networkCountryIso = networkCountryIso;
    }

    public String getConnectionType()
    {
        return connectionType;
    }

    public void setConnectionType(String connectionType)
    {
        if (connectionType != null)
            this.connectionType = connectionType;

        setConnectionTypeChanged(connectionType, testCase);
    }

    public String getConnectionTypeDownloadStart()
    {
        return connectionTypeDownloadStart;
    }

    public void setConnectionTypeDownloadStart(String connectionTypeDownloadStart)
    {
        this.connectionTypeDownloadStart = connectionTypeDownloadStart;
    }

    public boolean isConnectionTypeDownloadChanged()
    {
        return connectionTypeDownloadChanged;
    }

    public void setConnectionTypeDownloadChanged(boolean connectionTypeDownloadChanged)
    {
        this.connectionTypeDownloadChanged = connectionTypeDownloadChanged;
    }

    public String getConnectionTypeUploadStart()
    {
        return connectionTypeUploadStart;
    }

    public void setConnectionTypeUploadStart(String connectionTypeUploadStart)
    {
        this.connectionTypeUploadStart = connectionTypeUploadStart;
    }

    public boolean isConnectionTypeUploadChanged()
    {
        return connectionTypeUploadChanged;
    }

    public void setConnectionTypeUploadChanged(boolean connectionTypeUploadChanged)
    {
        this.connectionTypeUploadChanged = connectionTypeUploadChanged;
    }

    public int getDataNetworkRttStart()
    {
        return dataNetworkRttStart;
    }

    public void setDataNetworkRttStart(int dataNetworkRttStart)
    {
        this.dataNetworkRttStart = dataNetworkRttStart;
    }

    public boolean isDataNetworkRttChanged()
    {
        return dataNetworkRttChanged;
    }

    public void setDataNetworkRttChanged(boolean dataNetworkRttChanged)
    {
        this.dataNetworkRttChanged = dataNetworkRttChanged;
    }

    public String getConnectionTypeRttStart()
    {
        return connectionTypeRttStart;
    }

    public void setConnectionTypeRttStart(String connectionTypeRttStart)
    {
        this.connectionTypeRttStart = connectionTypeRttStart;
    }

    public boolean isConnectionTypeRttChanged()
    {
        return connectionTypeRttChanged;
    }

    public void setConnectionTypeRttChanged(boolean connectionTypeRttChanged)
    {
        this.connectionTypeRttChanged = connectionTypeRttChanged;
    }

    public void setCurrentTestCase(AbstractMeasurementResult.TestCase testCase)
    {
        this.testCase = testCase;

        switch (testCase)
        {
            case DOWNLOAD:
            {
                setDataNetworkDownloadStart(getDataNetworkId());
                setConnectionTypeDownloadStart(getConnectionType());
                break;
            }

            case UPLOAD:
            {
                setDataNetworkUploadStart(getDataNetworkId());
                setConnectionTypeUploadStart(getConnectionType());
                break;
            }

            case RTT_UDP:
            {
                setDataNetworkRttStart(getDataNetworkId());
                setConnectionTypeRttStart(getConnectionType());
                break;
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.DataConnectionStateListener
    {
        private final TelephonyManager tm;

        public MyTelephonyCallback(TelephonyManager tm)
        {
            this.tm = tm;
        }

        @Override
        public void onDataConnectionStateChanged(int networkState, int networkType)
        {
            Log.debug(TAG, "network triggered ");
            setDataNetworkId(networkType);
            setDataNetwork(new Tool().getNetType(getDataNetworkId()));
            tm.unregisterTelephonyCallback(this);
        }
    }

    public void startNetworkListener(Context ctx)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            listenerTelephony = new ListenerTelephony(ctx, new ModulesInterface()
            {
                @Override
                public void receiveString(String message)
                {

                }

                @Override
                public void receiveData(JSONObject message)
                {
                    try
                    {

                        setDataNetworkId(message.getInt("app_access_id"));
                        setDataNetwork(message.getString("app_access"));

                        setOperatorNetMcc(message.optInt("app_operator_net_mcc"));
                        setOperatorNetMnc(message.optInt("app_operator_net_mnc"));
                        setOperatorNet(message.optString("app_operator_net"));

                        setOperatorSimMcc(message.optInt("app_operator_sim_mcc"));
                        setOperatorSimMnc(message.optInt("app_operator_sim_mnc"));
                        setOperatorSim(message.optString("app_operator_sim"));

                        setCallState(message.optInt("app_call_state", -1));
                        setConnectionType(message.optString("app_mode"));


                        if (getTetheringState() != 1)
                        {
                            setTetheringState(new Tool().isWifiTethering(ctx) ? 1 : 0);
                        }


                        if (getRoamingState() != 1)
                        {
                            setRoamingState(new Tool().isRoaming(ctx) ? 1 : 0);
                        }

                        setArfcn(message.optInt("app_arfcn"));
                        setRssi(message.optInt("app_rssi"));
                        setCellId(message.optLong("app_cellid"));
                        setCellLac(message.optInt("app_celllac"));
                        setCellPci(message.optInt("app_cellpci"));
                        setRsrp(message.optInt("app_rsrp"));
                        setRsrq(message.optInt("app_rsrq"));

                    } catch (Exception ex)
                    {
                        Log.warning(TAG, "network listener failed", ex);
                    }
                }
            });

            listenerTelephony.startUpdates();
        } else
        {
            new Thread(() ->
            {
                Looper.prepare();
                listenerNetwork = new ListenerNetwork(ctx, new ModulesInterface()
                {
                    @Override
                    public void receiveString(String message)
                    {

                    }

                    @Override
                    public void receiveData(JSONObject message)
                    {
                        try
                        {

                            setDataNetworkId(message.optInt("app_access_id"));
                            setDataNetwork(message.optString("app_access"));

                            setOperatorNetAll(message.optString("app_operator_net"), message.optInt("app_operator_net_mcc"), message.optInt("app_operator_net_mnc"));

                            setOperatorSimMcc(message.optInt("app_operator_sim_mcc"));
                            setOperatorSimMnc(message.optInt("app_operator_sim_mnc"));
                            setOperatorSim(message.optString("app_operator_sim"));

                            setCallState(message.optInt("app_call_state"));
                            setConnectionType(message.optString("app_mode"));

                            setArfcn(message.optInt("app_arfcn"));
                            setCellId(message.optLong("app_cellid"));
                            setCellLac(message.optInt("app_celllac"));
                            setRssi(message.optInt("app_rssi"));
                            setRsrp(message.optInt("app_rsrp"));
                            setRsrq(message.optInt("app_rsrq"));


                            if (getTetheringState() != 1)
                            {
                                setTetheringState(new Tool().isWifiTethering(ctx) ? 1 : 0);
                            }


                            if (getRoamingState() != 1)
                            {
                                setRoamingState(new Tool().isRoaming(ctx) ? 1 : 0);
                            }

                        } catch (Exception ex)
                        {
                            Log.warning(TAG, "network listener failed", ex);
                        }
                    }
                });

                listenerNetwork.startUpdates();

                Looper.loop();
            }).start();
        }
    }

    public void stopNetworkListener()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (listenerTelephony != null)
                listenerTelephony.stopUpdates();
        } else
        {
            if (listenerNetwork != null)
                listenerNetwork.stopUpdates();
        }
    }
}
