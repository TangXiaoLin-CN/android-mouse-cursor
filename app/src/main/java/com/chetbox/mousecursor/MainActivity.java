package com.chetbox.mousecursor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity {

    // A reference to the service used to get location updates.
    private MouseAccessibilityService mService = null;
    // Tracks the bound state of the service.
    private boolean mBound = false;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MouseAccessibilityService.LocalBinder binder = (MouseAccessibilityService.LocalBinder) service;
                mService = binder.getServerInstance();
                mBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mService = null;
                mBound = false;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();

        //add for android O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mBound) {
                // Unbind from the service. This signals to the service that this activity is no longer
                // in the foreground, and the service can respond by promoting itself to a foreground
                // service.
                unbindService(mServiceConnection);
                mBound = false;
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        //add for android O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bindService(new Intent(this, MouseAccessibilityService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void onResume() {
        super.onResume();
        finish();
    }
}
