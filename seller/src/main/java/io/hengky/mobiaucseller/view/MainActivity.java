package io.hengky.mobiaucseller.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.hengky.common.BaseMainActivity;
import io.hengky.common.BluetoothSocketManager;
import io.hengky.common.Device;
import io.hengky.common.DeviceManager;
import io.hengky.mobiaucseller.R;
import io.hengky.mobiaucseller.databinding.ActivityMainBinding;
import io.hengky.mobiaucseller.databinding.ViewDeviceListItemBinding;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * Created by yip on 16/5/16.
 */
public class MainActivity extends BaseMainActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private ArrayAdapter<Device> arrayAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityMainBinding dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        dataBinding.setState(this);

        {
            dataBinding.updateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startDiscovery();
                }
            });
        }
        {
            dataBinding.price1Btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeviceManager.getInstance().broadcasePrice(5);
                }
            });
        }
        {
            dataBinding.price2Btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeviceManager.getInstance().broadcasePrice(99);
                }
            });
        }
        {
            dataBinding.powerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setDiscoverable(isChecked);
                }
            });
        }
        {
            arrayAdapter = new ArrayAdapter<Device>(this, R.layout.view_device_list_item, R.id.title_txt) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    ViewDeviceListItemBinding binding = ViewDeviceListItemBinding.inflate(getLayoutInflater(), parent, false);
                    binding.setDevice(arrayAdapter.getItem(position));
                    return binding.getRoot();
                }
            };

            dataBinding.deviceList.setAdapter(arrayAdapter);
            DeviceManager.getInstance().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Device>>() {
                @Override
                public void call(List<Device> devices) {
                    arrayAdapter.clear();
                    arrayAdapter.addAll(devices);
                }
            });
        }

    }
}
