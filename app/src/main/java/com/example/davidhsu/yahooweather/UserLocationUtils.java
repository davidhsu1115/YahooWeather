package com.example.davidhsu.yahooweather;

import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

class UserLocationUtils {

    private Timer mTimer;
    private LocationManager mLocationManager;
    private LocationResult mLocationResult;
    private boolean mGpsEnables = false;
    private boolean mNetworkEnabled = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());   //Firgure it out

    /*
     * need permission
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
     */

    public boolean findUserLocation(Context context, LocationResult result)
    {
        mLocationResult=result;
        if(mLocationManager==null)
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try{mGpsEnables=mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
        try{mNetworkEnabled=mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

        //don't start listeners if no provider is enabled
        if(!mGpsEnables && !mNetworkEnabled)
            return false;
        
        if(mGpsEnables)
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        if(mNetworkEnabled)
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        //the code under this line is to prevent the Thread problem.
        mHandler.postDelayed(new GetLastLocation(),20000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (mTimer != null) mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            mLocationManager.removeUpdates(locationListenerNetwork);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (mTimer != null) mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            mLocationManager.removeUpdates(locationListenerGps);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run() {
            YahooWeatherLog.d("20 secs timeout for GPS. GetLocation is executed.");
             mLocationManager.removeUpdates(locationListenerGps);
             mLocationManager.removeUpdates(locationListenerNetwork);

             Location net_loc=null, gps_loc=null;
             if(mGpsEnables)
                 gps_loc=mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
             if(mNetworkEnabled)
                 net_loc=mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

             //if there are both values use the latest one
             if(gps_loc!=null && net_loc!=null){
                 if(gps_loc.getTime()>net_loc.getTime())
                     mLocationResult.gotLocation(gps_loc);
                 else
                     mLocationResult.gotLocation(net_loc);
                 return;
             }

             if(gps_loc!=null){
                 mLocationResult.gotLocation(gps_loc);
                 return;
             }
             if(net_loc!=null){
                 mLocationResult.gotLocation(net_loc);
                 return;
             }
             mLocationResult.gotLocation(null);
        }
    }

    public static interface LocationResult{
        public abstract void gotLocation(Location location);
    }
}