package io.hengky.mobiaucbuyer.view;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import io.hengky.common.BaseMainActivity;
import io.hengky.common.BluetoothSocketManager;
import io.hengky.common.Device;
import io.hengky.common.DeviceManager;
import io.hengky.mobiaucbuyer.R;
import io.hengky.mobiaucbuyer.databinding.ActivityMainBinding;
import io.hengky.mobiaucbuyer.databinding.ViewDeviceListItemBinding;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * Created by yip on 16/5/16.
 */
public class MainActivity extends BaseMainActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final int MIN_PRICE = 50;
    public static final int MAX_PRICE = 100;

    private ArrayAdapter<Device> arrayAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityMainBinding dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        dataBinding.setState(this);
        dataBinding.setDeviceManager(DeviceManager.getInstance());

        // set price to middle
        DeviceManager.getInstance().price.set((MAX_PRICE + MIN_PRICE) / 2);

//        {
//            dataBinding.updateBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    startDiscovery();
//                }
//            });
//        }
//        {
//            dataBinding.price1Btn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    DeviceManager.getInstance().broadcasePrice(5);
//                }
//            });
//        }
//        {
//            dataBinding.price2Btn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    DeviceManager.getInstance().broadcasePrice(99);
//                }
//            });
//        }

        {
            dataBinding.powerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setDiscoverable(isChecked);
                }
            });
        }

        {
            dataBinding.priceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    DeviceManager.getInstance().price.set(progress + MIN_PRICE);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // do nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // do nothing
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

    public void onResetBuyingClicked() {
        DeviceManager.getInstance().resetTrade();
    }
}
