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

package com.zafaco.moduleCommon.models.location;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;
import com.zafaco.moduleCommon.Log;
import com.zafaco.moduleCommon.interfaces.ModulesInterface;
import com.zafaco.moduleCommon.interfaces.ResultInfo;
import com.zafaco.moduleCommon.listener.ListenerGeoLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LocationInfo implements ResultInfo
{
    private double              latitude;
    private double              longitude;
    private double              altitude;
    private double              altitudeMax;
    private double              accuracy;
    private double              velocity;
    private double              velocityAvg;
    private double              velocityMax;
    private boolean             precisePermission;
    private long                timestampFix;
    private ListenerGeoLocation listenerGeoLocation;

    private static final String TAG = "LOCATION_INFO";

    public LocationInfo()
    {
    }

    public LocationInfo(Context ctx)
    {
        getSingleLocation(ctx);
    }

    private void getSingleLocation(Context ctx)
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        this.precisePermission = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;


        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx);
        Location                    location                    = null;
        try
        {
            location = Tasks.await(fusedLocationProviderClient.getLastLocation(), 1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e)
        {
            Log.error("TAG", "getLastLocation", e);
        }
        if (location != null)
        {
            addAllData(location);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY).setMaxUpdates(1).build();

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback()
        {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult)
            {
                addAllData(locationResult.getLastLocation());
            }
        }, Looper.getMainLooper());
    }


    public LocationInfo(Location location)
    {
        addAllData(location);
    }

    private void addAllData(Location location)
    {
        if (location != null)
        {
            this.latitude     = location.getLatitude();
            this.longitude    = location.getLongitude();
            this.altitude     = location.getAltitude();
            this.accuracy     = location.getAccuracy();
            this.velocity     = location.getSpeed();
            this.timestampFix = location.getTime();
        }
    }

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }

    public double getAltitude()
    {
        return altitude;
    }

    public void setAltitude(double altitude)
    {
        this.altitude = altitude;
    }

    public double getAltitudeMax()
    {
        return altitudeMax;
    }

    public void setAltitudeMax(double altitudeMax)
    {
        this.altitudeMax = altitudeMax;
    }

    public double getAccuracy()
    {
        return accuracy;
    }

    public void setAccuracy(double accuracy)
    {
        this.accuracy = accuracy;
    }

    public double getVelocity()
    {
        return velocity;
    }

    public void setVelocity(double velocity)
    {
        this.velocity = velocity;
    }

    public long getTimestampFix()
    {
        return timestampFix;
    }

    public void setTimestampFix(long timestampFix)
    {
        this.timestampFix = timestampFix;
    }

    public double getVelocityAvg()
    {
        return velocityAvg;
    }

    public void setVelocityAvg(double velocityAvg)
    {
        if (!Double.isNaN(velocityAvg))
            this.velocityAvg = velocityAvg;
    }

    public double getVelocityMax()
    {
        return velocityMax;
    }

    public void setVelocityMax(double velocityMax)
    {
        if (velocityMax >= this.velocityMax)
            this.velocityMax = velocityMax;
    }

    public boolean isPrecisePermission()
    {
        return precisePermission;
    }

    public void setPrecisePermission(boolean precisePermission)
    {
        this.precisePermission = precisePermission;
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jData = new JSONObject();

        try
        {
            if (getLatitude() != 0.0 && getLongitude() != 0.0)
            {
                jData.put("latitude", getLatitude());
                jData.put("longitude", getLongitude());
                jData.put("altitude", getAltitude());
                jData.put("accuracy", getAccuracy());
                jData.put("altitude_max", getAltitudeMax());
                jData.put("velocity_max", getVelocityMax());
                jData.put("velocity_avg", getVelocityAvg());
                jData.put("velocity", getVelocity());
                jData.put("precise_permission", isPrecisePermission());
                jData.put("timestamp_fix", getTimestampFix());
            } else
            {
                jData.put("latitude", null);
                jData.put("longitude", null);
                jData.put("altitude", null);
                jData.put("accuracy", null);
                jData.put("altitude_max", null);
                jData.put("velocity_max", null);
                jData.put("velocity_avg", null);
                jData.put("velocity", null);
                jData.put("precise_permission", null);
                jData.put("timestamp_fix", null);
            }
        } catch (JSONException ignored)
        {
        }

        return jData;
    }


    public void stopLocationListener()
    {
        if (listenerGeoLocation != null)
            listenerGeoLocation.stopUpdates();
    }

    public void startLocationListener(Context ctx)
    {
        listenerGeoLocation = new ListenerGeoLocation(ctx, new ModulesInterface()
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
                    setVelocityAvg(message.optDouble("app_velocity_avg"));
                    setPrecisePermission(message.optBoolean("app_precise_location_permission"));

                    if (message.optDouble("app_accuracy") <= getAccuracy())
                    {
                        setAccuracy(message.optDouble("app_accuracy"));
                        setLatitude(message.optDouble("app_latitude"));
                        setLongitude(message.optDouble("app_longitude"));
                        setAltitude(message.optDouble("app_altitude"));
                        setAltitudeMax(message.optDouble("app_altitude_max"));
                        setVelocity(message.optDouble("app_velocity"));
                        setVelocityMax(message.optDouble("app_velocity_max"));
                    }
                } catch (Exception ignore)
                {
                    Log.warning(TAG, "geo callback", ignore);
                }
            }
        },false);

        listenerGeoLocation.setGeoService();
        listenerGeoLocation.startUpdates(1, 1, Priority.PRIORITY_HIGH_ACCURACY);
        listenerGeoLocation.withIntervall();
    }

}
