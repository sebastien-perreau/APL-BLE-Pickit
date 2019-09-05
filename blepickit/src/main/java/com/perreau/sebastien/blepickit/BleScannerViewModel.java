package com.perreau.sebastien.blepickit;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.ParcelUuid;

import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class BleScannerViewModel extends AndroidViewModel
{

    private final BleScannerLiveData mBleScannerLiveData;

    public BleScannerLiveData getScannerState()
    {
        return mBleScannerLiveData;
    }

    public BleScannerViewModel(final Application application)
    {
        super(application);

        mBleScannerLiveData = new BleScannerLiveData(BleUtils.isBleEnabled(), BleUtils.isLocationEnabled(application));
        registerBroadcastReceivers(application);
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();

        getApplication().unregisterReceiver(mBluetoothStateBroadcastReceiver);
        getApplication().unregisterReceiver(mLocationProviderChangedReceiver);
    }

    public void refresh()
    {
        mBleScannerLiveData.refresh();
    }

    public void startScan(String filterName)
    {
        if (mBleScannerLiveData.isScanning())
        {
            return;
        }

        // Scanning settings
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                .build();

        // Let's use the filter to scan only for Blinky devices
        final ParcelUuid uuid = new ParcelUuid(BlePickitManager.BLE_UUID_SERVICE);
        final List<ScanFilter> filters = new ArrayList<>();
//		filters.add(new ScanFilter.Builder().setServiceUuid(uuid).build());

        if (filterName != null)
        {
            filters.add(new ScanFilter.Builder().setDeviceName(filterName).build());
        }

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(filters, settings, scanCallback);
        mBleScannerLiveData.scanningStarted();
    }

    /**
     * stop scanning for bluetooth devices.
     */
    public void stopScan()
    {
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(scanCallback);
        mBleScannerLiveData.scanningStopped();
    }

    private final ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result)
        {
            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (BleUtils.isLocationRequired(getApplication()) && !BleUtils.isLocationEnabled(getApplication()))
            {
                BleUtils.markLocationNotRequired(getApplication());
            }

            mBleScannerLiveData.deviceDiscovered(result);
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results)
        {
            // Batch scan is disabled (report delay = 0)
        }

        @Override
        public void onScanFailed(final int errorCode)
        {
            // TODO This should be handled
            mBleScannerLiveData.scanningStopped();
        }
    };

    /**
     * Register for required broadcast receivers.
     */
    private void registerBroadcastReceivers(final Application application)
    {
        application.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        application.registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider
     */
    private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(final Context context, final Intent intent)
        {
            final boolean enabled = BleUtils.isLocationEnabled(context);
            mBleScannerLiveData.setLocationEnabled(enabled);
        }
    };

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter
     */
    private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(final Context context, final Intent intent)
        {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            switch (state)
            {
                case BluetoothAdapter.STATE_ON:
                    mBleScannerLiveData.bluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF)
                    {
                        stopScan();
                        mBleScannerLiveData.bluetoothDisabled();
                    }
                    break;
            }
        }
    };
}
