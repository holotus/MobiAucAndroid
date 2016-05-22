package io.hengky.common;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import java.util.ArrayList;

/**
 * Created by yip on 21/5/16.
 */
public class BaseMainActivity extends AppCompatActivity {

    public final ObservableBoolean isDiscoverable = new ObservableBoolean();

    static final String LOG_TAG = BaseMainActivity.class.getSimpleName();
    static final int NULL_INT = -1;
    static final int FOREVER_DISCOVERABLE = 0;
    static final int REQUEST_ENABLE_DISCOVERABLE = 1;
    static final int REQUEST_ACCESS_FINE_LOCATION = 3;

    ProgressDialog mProgressDialog;
    AlertDialog mDiscoverableStoppedAlert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Updating all device.");
        }

        {

            if (mDiscoverableStoppedAlert != null) {
                mDiscoverableStoppedAlert.dismiss();
            }
            mDiscoverableStoppedAlert = new AlertDialog.Builder(this)
                    .setMessage("Discoverable Stopped")
                    .setPositiveButton("OK", null)
                    .setCancelable(true)
                    .create();
        }

        // Helper for Discoverable and Discovery
        {
            IntentFilter filter;

            // Register for broadcasts when discoverable status chance
            filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            this.registerReceiver(mReceiver, filter);

            // Register for broadcasts when discovery has started
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            this.registerReceiver(mReceiver, filter);

            // Register for broadcasts when a device is discovered
            filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.registerReceiver(mReceiver, filter);

            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            this.registerReceiver(mReceiver, filter);

        }

    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /////
    // Helper for Discoverable and Discovery
    /////
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {

                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED: {

                    Log.i(LOG_TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED");

                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, NULL_INT);
//                    int previousScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, NULL_INT);

                    boolean ok = scanMode != NULL_INT && scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
//                    boolean previousOk = previousScanMode != NULL_INT && previousScanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;

                    onEnsureDiscoverable(ok);

                    break;
                }

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {

                    Log.i(LOG_TAG, "BluetoothAdapter.ACTION_DISCOVERY_STARTED");
                    onDiscoveringChanged(true);
                    break;
                }

                case BluetoothDevice.ACTION_FOUND: {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(LOG_TAG, "BluetoothDevice.ACTION_FOUND ***** FOUND DEVICE *****: " + device.getName());
                    BluetoothSocketManager.getInstance().startConnect(device);
                    break;

                }

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {

                    Log.i(LOG_TAG, "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
                    onDiscoveringChanged(false);
                    break;
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ENABLE_DISCOVERABLE: {
                if (Activity.RESULT_CANCELED == resultCode) {
                    onEnsureDiscoverable(false);
                }
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION: {
                onEnsureLocationPermission(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            }
        }

    }

    /////
    // Location Permission (Android 6.0)
    /////
    private void ensureLocationPremission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
        } else {
            // OK
            onEnsureLocationPermission(true);
        }
    }

    private void onEnsureLocationPermission(boolean isOk) {
        if (isOk) {
            ensureDiscoverable();
        } else {
            onDiscoverableChanged(false);
        }
    }

    /////
    // Discoverable Permission
    /////
    private void ensureDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, FOREVER_DISCOVERABLE);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERABLE);
    }

    private void onEnsureDiscoverable(boolean isOK) {

        if (isOK) {
            BluetoothSocketManager.getInstance().startAccept();
            onDiscoverableChanged(true);

        } else {
            BluetoothSocketManager.getInstance().stopAll();
            onDiscoverableChanged(false);
        }
    }

    protected void onDiscoverableChanged(boolean isDiscoverable) {
        if (isDiscoverable) {
            startDiscovery();
            mDiscoverableStoppedAlert.dismiss();
        } else {
            mDiscoverableStoppedAlert.show();
        }
        this.isDiscoverable.set(isDiscoverable);
    }

    protected void onDiscoveringChanged(boolean isDiscovering) {
        if (isDiscovering) {
            mProgressDialog.show();
        } else {
            mProgressDialog.dismiss();
        }
    }

    protected void setDiscoverable(boolean isDiscoverable) {
        if (isDiscoverable) {
            ensureLocationPremission();
        } else {
            // TODO: stop discoverable, stop accept, stop all connected device
            onDiscoverableChanged(false);
            BluetoothSocketManager.getInstance().stopAll();
        }
    }

    protected void startDiscovery() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, 0, 0, "Update Device List");
//        return true;
//    }
}
