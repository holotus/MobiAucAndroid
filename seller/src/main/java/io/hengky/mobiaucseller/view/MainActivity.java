package io.hengky.mobiaucseller.view;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.Date;

import io.hengky.common.BluetoothSocketManager;


/**
 * Created by yip on 16/5/16.
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    final static int FOREVER = 0;
    final static int REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startDiscoverable();

        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, new Date().toString());
                BluetoothSocketManager.getInstance().write(new Date().toString().getBytes());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                startDiscoverable();
            } else {
                BluetoothSocketManager.getInstance().startAccept();
            }
        }
    }

    private void startDiscoverable() {

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, FOREVER);
        startActivityForResult(discoverableIntent, REQUEST_CODE);

    }
}
