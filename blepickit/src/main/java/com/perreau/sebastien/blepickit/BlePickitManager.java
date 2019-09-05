package com.perreau.sebastien.blepickit;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;

public class BlePickitManager extends BleManager<BlePickitManager.Callbacks>
{

    public interface Callbacks extends BleManagerCallbacks
    {
        void onChar1501DataReceived(final byte[] data);
        void onChar1502DataReceived(final byte[] data);
        void onChar1503DataReceived(final byte[] data);

        void onChar1501DataSent(final byte[] data);
        void onChar1502DataSent(final byte[] data);
        void onChar1503DataSent(final byte[] data);
    }

    public final static UUID BLE_UUID_SERVICE = UUID.fromString("a7bb1500-eef2-4a8e-80d4-13a83c8cf46f");
    private final static UUID BLE_UUID_CHAR_1501 = UUID.fromString("a7bb1501-eef2-4a8e-80d4-13a83c8cf46f");
    private final static UUID BLE_UUID_CHAR_1502 = UUID.fromString("a7bb1502-eef2-4a8e-80d4-13a83c8cf46f");
    private final static UUID BLE_UUID_CHAR_1503 = UUID.fromString("a7bb1503-eef2-4a8e-80d4-13a83c8cf46f");

    private BluetoothGattCharacteristic mChar1501;
    private BluetoothGattCharacteristic mChar1502;
    private BluetoothGattCharacteristic mChar1503;

    BlePickitManager(final Context context)
    {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback()
    {
        return mGattCallback;
    }

    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback()
    {

        @Override
        protected boolean isRequiredServiceSupported(BluetoothGatt gatt)
        {
            final BluetoothGattService service = gatt.getService(BLE_UUID_SERVICE);

            if (service != null)
            {
                mChar1501 = service.getCharacteristic(BLE_UUID_CHAR_1501);
                mChar1502 = service.getCharacteristic(BLE_UUID_CHAR_1502);
                mChar1503 = service.getCharacteristic(BLE_UUID_CHAR_1503);
            }

            boolean char1501WriteRequest = false;
            if (mChar1501 != null)
            {
                char1501WriteRequest = (mChar1501.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
            }

            boolean char1502WriteRequest = false;
            if (mChar1502 != null)
            {
                char1502WriteRequest = (mChar1502.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
            }

            boolean char1503WriteRequest = false;
            if (mChar1503 != null)
            {
                char1503WriteRequest = (mChar1503.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
            }

            return char1501WriteRequest && char1502WriteRequest && char1503WriteRequest;
        }

        @Override
        protected void initialize()
        {
            super.initialize();

            setNotificationCallback(mChar1501).with((device, data) ->
                    mCallbacks.onChar1501DataReceived(data.getValue()));
            enableNotifications(mChar1501).enqueue();

            setNotificationCallback(mChar1502).with((device, data) ->
                    mCallbacks.onChar1502DataReceived(data.getValue()));
            enableNotifications(mChar1502).enqueue();

            setNotificationCallback(mChar1503).with((device, data) ->
                    mCallbacks.onChar1503DataReceived(data.getValue()));
            enableNotifications(mChar1503).enqueue();
        }

        @Override
        protected void onDeviceDisconnected()
        {
            mChar1501 = null;
            mChar1502 = null;
            mChar1503 = null;
        }
    };

    void writeCharacteristic1502(byte[] data)
    {
        writeCharacteristic(mChar1502, data).with((device, data1) ->
                mCallbacks.onChar1502DataSent(data1.getValue())).enqueue();
    }

    void writeCharacteristic1501(byte[] data)
    {
        writeCharacteristic(mChar1501, data).with((device, data1) ->
                mCallbacks.onChar1501DataSent(data1.getValue())).enqueue();
    }

    void writeCharacteristic1503(byte[] data)
    {
        writeCharacteristic(mChar1503, data).with((device, data1) ->
                mCallbacks.onChar1503DataSent(data1.getValue())).enqueue();
    }

}
