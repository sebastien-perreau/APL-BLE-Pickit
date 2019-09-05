package com.perreau.sebastien.blepickit;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ExtendedBluetoothDevice implements Parcelable
{
    private final BluetoothDevice device;
    private String name;
    private int rssi;

    public ExtendedBluetoothDevice(final ScanResult scanResult)
    {
        this.device = scanResult.getDevice();
        this.name = scanResult.getScanRecord().getDeviceName();
        this.rssi = scanResult.getRssi();
    }

    public BluetoothDevice getDevice()
    {
        return device;
    }

    public String getAddress()
    {
        return device.getAddress();
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public int getRssi()
    {
        return rssi;
    }

    public void setRssi(final int rssi)
    {
        this.rssi = rssi;
    }

    public boolean matches(final ScanResult scanResult)
    {
        return device.getAddress().equals(scanResult.getDevice().getAddress());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o instanceof ExtendedBluetoothDevice)
        {
            final ExtendedBluetoothDevice that = (ExtendedBluetoothDevice) o;
            return device.getAddress().equals(that.device.getAddress());
        }
        return super.equals(o);
    }

    // Parcelable implementation

    private ExtendedBluetoothDevice(final Parcel in)
    {
        this.device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        this.name = in.readString();
        this.rssi = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags)
    {
        parcel.writeParcelable(device, flags);
        parcel.writeString(name);
        parcel.writeInt(rssi);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<ExtendedBluetoothDevice> CREATOR = new Creator<ExtendedBluetoothDevice>()
    {
        @Override
        public ExtendedBluetoothDevice createFromParcel(final Parcel source)
        {
            return new ExtendedBluetoothDevice(source);
        }

        @Override
        public ExtendedBluetoothDevice[] newArray(final int size)
        {
            return new ExtendedBluetoothDevice[size];
        }
    };
}
