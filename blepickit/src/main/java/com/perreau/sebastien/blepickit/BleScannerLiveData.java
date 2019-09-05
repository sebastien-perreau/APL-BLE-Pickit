package com.perreau.sebastien.blepickit;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.ScanResult;


/**
 * This class keeps the current list of discovered Bluetooth LE devices matching filter.
 * If a new device has been found it is added to the list and the LiveData i observers are
 * notified. If a packet from a device that's already in the list is found, the RSSI and name
 * are updated and observers are also notified. Observer may check {@link #getUpdatedDeviceIndex()}
 * to find out the index of the updated device.
 */
@SuppressWarnings("unused")
public class BleScannerLiveData extends LiveData<BleScannerLiveData>
{
    private final List<ExtendedBluetoothDevice> mDevices = new ArrayList<>();
    private Integer mUpdatedDeviceIndex;
    private boolean mScanningStarted;
    private boolean mBluetoothEnabled;
    private boolean mLocationEnabled;

    /* package */ BleScannerLiveData(final boolean bluetoothEnabled, final boolean locationEnabled)
    {
        mScanningStarted = false;
        mBluetoothEnabled = bluetoothEnabled;
        mLocationEnabled = locationEnabled;
        postValue(this);
    }

    /* package */ void refresh()
    {
        postValue(this);
    }

    /* package */ void scanningStarted()
    {
        mScanningStarted = true;
        postValue(this);
    }

    /* package */ void scanningStopped()
    {
        mScanningStarted = false;
        postValue(this);
    }

    /* package */ void bluetoothEnabled()
    {
        mBluetoothEnabled = true;
        postValue(this);
    }

    /* package */ void bluetoothDisabled()
    {
        mBluetoothEnabled = false;
        mUpdatedDeviceIndex = null;
        mDevices.clear();
        postValue(this);
    }

    /* package */ void setLocationEnabled(final boolean enabled)
    {
        mLocationEnabled = enabled;
        postValue(this);
    }

    /* package */ void deviceDiscovered(final ScanResult result)
    {
        ExtendedBluetoothDevice device;

        final int index = indexOf(result);
        if (index == -1)
        {
            device = new ExtendedBluetoothDevice(result);
            mDevices.add(device);
            mUpdatedDeviceIndex = null;
        }
        else
        {
            device = mDevices.get(index);
            mUpdatedDeviceIndex = index;
        }
        // Update RSSI and name
        device.setRssi(result.getRssi());
        device.setName(result.getScanRecord().getDeviceName());

        postValue(this);
    }

    /**
     * Returns the list of devices.
     *
     * @return current list of devices discovered
     */
    @NonNull
    public List<ExtendedBluetoothDevice> getDevices()
    {
        return mDevices;
    }

    /**
     * Returns null if a new device was added, or an index of the updated device.
     */
    @Nullable
    public Integer getUpdatedDeviceIndex()
    {
        final Integer i = mUpdatedDeviceIndex;
        mUpdatedDeviceIndex = null;
        return i;
    }

    /**
     * Returns whether the list is empty.
     */
    public boolean isEmpty()
    {
        return mDevices.isEmpty();
    }

    /**
     * Returns whether scanning is in progress.
     */
    public boolean isScanning()
    {
        return mScanningStarted;
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    public boolean isBluetoothEnabled()
    {
        return mBluetoothEnabled;
    }

    /**
     * Returns whether Location is enabled.
     */
    public boolean isLocationEnabled()
    {
        return mLocationEnabled;
    }

    /**
     * Finds the index of existing devices on the scan results list.
     *
     * @param result scan result
     * @return index of -1 if not found
     */
    private int indexOf(final ScanResult result)
    {
        int i = 0;
        for (final ExtendedBluetoothDevice device : mDevices)
        {
            if (device.matches(result))
            {
                return i;
            }
            i++;
        }
        return -1;
    }
}
