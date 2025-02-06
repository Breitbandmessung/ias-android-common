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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.Tool;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;

import org.json.JSONObject;

import java.util.ConcurrentModificationException;
import java.util.Vector;

public class ListenerGeoLocation
{


    private final Context ctx;


    private final ModulesInterface interfaceCallback;
    private       JSONObject       jData;

    private final boolean                     useFused;
    private       FusedLocationProviderClient mFusedLocationClient = null;

    private boolean mtGeoService  = false;
    private boolean withIntervall = false;

    private       Location      lastLocation;
    private       Thread        pThread;
    private final Vector<Float> app_velocity_vector = new Vector<>();

    private double app_altitude_max = 0;
    private float app_velocity_max = 0;
    private float app_velocity_avg = 0;

    private static final String TAG = "ListenerGeoLocation";


    public ListenerGeoLocation(Context ctx, ModulesInterface intCall, boolean fused)
    {
        this.ctx = ctx;

        this.useFused = fused;

        this.interfaceCallback = intCall;

        jData = new JSONObject();
    }

    public void setGeoService()
    {
        mtGeoService = true;
    }


    public void startUpdates(int min_time, int min_distance, int min_prio)
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        if (useFused)
        {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
            LocationRequest mLocationRequest = new LocationRequest.Builder(min_prio, min_time).setMinUpdateIntervalMillis(min_time).setMinUpdateDistanceMeters(min_distance).build();

            getLastLocation(mFusedLocationClient);

            try
            {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
            } catch (Exception ex)
            {
                Log.warning(TAG, "requesting fused location updates failed", ex);
            }
        } else
        {
            try
            {
                LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, min_time, min_distance, mLocationGPSListener, Looper.getMainLooper());
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, min_time, min_distance, mLocationNetworkListener, Looper.getMainLooper());
            } catch (Exception ex)
            {
                Log.warning(TAG, "requesting location updates failed", ex);
            }
        }
    }

    public void withIntervall()
    {
        pThread = new Thread(new ListenerGeoLocation.WorkerThread());
        pThread.start();

        withIntervall = true;
    }


    public void stopUpdates()
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        if (useFused && mFusedLocationClient != null)
        {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        } else
        {
            LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationNetworkListener);
            locationManager.removeUpdates(mLocationGPSListener);
        }

        if (withIntervall)
        {
            pThread.interrupt();
        }
    }

    private final LocationListener mLocationNetworkListener = this::getLocation;
    private final LocationListener mLocationGPSListener     = this::getLocation;

    private final LocationCallback mLocationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            getLocation(locationResult.getLastLocation());
        }
    };

    private void getLastLocation(FusedLocationProviderClient mFusedLocationClient)
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        mFusedLocationClient.getLastLocation().addOnSuccessListener(lastKnownLocation ->
        {


            if (lastKnownLocation == null)
                return;

            getLocation(lastKnownLocation);
        }).addOnFailureListener(e -> Log.warning(TAG, "onFailure: Location", e));
    }

    private void getLocation(Location location)
    {
        if (new Tool().isMockLocationOn(location))
            return;

        double newAccuracy = location.getAccuracy();

        if ((

                newAccuracy != 0.0 && newAccuracy <= (lastLocation == null ? Float.MAX_VALUE : lastLocation.getAccuracy())) || (

                mtGeoService))
        {
            try
            {
                jData = new JSONObject();

                jData.put("app_latitude", location.getLatitude());
                jData.put("app_longitude", location.getLongitude());
                jData.put("app_accuracy", newAccuracy);

                double app_altitude = location.getAltitude();
                float app_velocity = location.getSpeed();

                jData.put("app_altitude", app_altitude);
                jData.put("app_velocity", app_velocity);

                jData.put("app_location_age_ns", SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos());

                jData.put("app_precise_location_permission", ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);


                if (withIntervall)
                {

                    if (app_altitude > app_altitude_max)
                    {
                        app_altitude_max = app_altitude;
                    }
                    jData.put("app_altitude_max", app_altitude_max);


                    if (app_velocity > app_velocity_max)
                    {
                        app_velocity_max = app_velocity;
                    }
                    jData.put("app_velocity_max", app_velocity_max);


                    if (!app_velocity_vector.isEmpty())
                    {
                        double velocitySum = 0;
                        for (float obj : app_velocity_vector)
                        {
                            velocitySum += obj;
                        }
                        app_velocity_avg = (float) (velocitySum / app_velocity_vector.size());
                    }
                    jData.put("app_velocity_avg", app_velocity_avg);
                }


                interfaceCallback.receiveData(jData);
            } catch (Exception ex)
            {
                if (!(ex instanceof ConcurrentModificationException))
                    Log.warning(TAG, "getLocation: error", jData, ex);
            }


            lastLocation = location;
        }
    }

    class WorkerThread extends Thread
    {
        int index = 0;

        public void run()
        {
            while (true)
            {
                try
                {
                    if (index > 10)
                    {
                        if (lastLocation != null)
                            app_velocity_vector.add(lastLocation.getSpeed());
                    }

                    Thread.sleep(1000);

                    index++;

                } catch (Exception ex)
                {
                    Log.info(TAG, "ListenerGeoLocation-Thread interrupted", ex);
                    break;
                }
            }
        }
    }
}
