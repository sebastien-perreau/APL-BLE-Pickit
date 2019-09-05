package com.perreau.sebastien.blepickit;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.perreau.sebastien.blepickit.MyAsyncTask;

public class BlePickitViewModel extends AndroidViewModel implements BlePickitManager.Callbacks
{

    private final BlePickitManager mBlePickitManager;
    private BluetoothDevice mDevice;

    private final MutableLiveData<String> mBlePickitConnectionState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mBlePickitIsConnected = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mBlePickitIsReady = new MutableLiveData<>();

    private final MutableLiveData<byte[]> mBlePickit_1503_GET_BLE_PARAMETERS = new MutableLiveData<>();
    private final MutableLiveData<byte[]> mBlePickit_1503_GET_SPC_PARAMETERS = new MutableLiveData<>();

    private long testTickStart, testTickStop;
    private int testLengthBytes;

    public BlePickitViewModel(@NonNull Application application)
    {
        super(application);
        mBlePickitManager = new BlePickitManager(getApplication());
        mBlePickitManager.setGattCallbacks(this);
    }

    public MutableLiveData<String> getBlePickitConnectionState()
    {
        return mBlePickitConnectionState;
    }

    public MutableLiveData<Boolean> getBlePickitIsConnected()
    {
        return mBlePickitIsConnected;
    }

    public MutableLiveData<Boolean> getBlePickitIsReady()
    {
        return mBlePickitIsReady;
    }

    MutableLiveData<byte[]> mBlePickit_1503_GetBleParameters()
    {
        return mBlePickit_1503_GET_BLE_PARAMETERS;
    }

    MutableLiveData<byte[]> mBlePickit_1503_GetSpcParameters()
    {
        return mBlePickit_1503_GET_SPC_PARAMETERS;
    }

    public void connect(@NonNull final ExtendedBluetoothDevice device)
    {
        // Prevent from calling again when called again (screen orientation changed)
        if (mDevice == null)
        {
            mDevice = device.getDevice();
            reconnect();
        }
    }

    private void reconnect()
    {
        if (mDevice != null)
        {
            mBlePickitManager.connect(mDevice).retry(3, 100).useAutoConnect(false).enqueue();
        }
    }

    public void disconnect()
    {
        if (mBlePickitManager.isConnected())
        {
            mBlePickitManager.disconnect();
        }
    }

    public void requestGetBleParams()
    {
        final byte[] data = new byte[1];
        data[0] = 0x00; // ID_GET_BLE_PARAMS
        mBlePickitManager.writeCharacteristic1503(data);
    }

    public void requestSetLedStatus(boolean enable)
    {
        final byte[] data = new byte[3];
        data[0] = 0x04; // ID_LED_STATUS
        data[1] = 0x01; // Length
        data[2] = (byte) (enable ? 0x01 : 0x00);
        mBlePickitManager.writeCharacteristic1503(data);
    }

    public void requestPaLnaStatus(boolean enable)
    {
        final byte[] data = new byte[3];
        data[0] = 0x01; // ID_PA_LNA
        data[1] = 0x01; // Length
        data[2] = (byte) (enable ? 0x01 : 0x00);
        mBlePickitManager.writeCharacteristic1503(data);
    }

    public void requestGetSpcParams(byte periodicity)
    {
        final byte[] data = new byte[4];
        data[0] = 0x09; // ID_GET_SPC_PARAMS
        data[1] = 0x02; // Length
        data[2] = (byte) 0x00;
        data[3] = (byte) periodicity;
        mBlePickitManager.writeCharacteristic1503(data);
    }

    public boolean requestSendBuffer(byte[] userData)
    {
        if (userData.length <= 242)
        {
            final byte[] data = new byte[userData.length + 2];

            data[0] = (byte) 0x30;  // ID_BUFFER
            data[1] = (byte) userData.length;
            System.arraycopy(userData, 0, data, 2, userData.length);
            //noinspection StatementWithEmptyBody
            mBlePickitManager.writeCharacteristic1501(data);

            return true;
        }
        return false;
    }

    public boolean requestSendExtendedBufferNoCrc(byte[] userData)
    {
        if (userData.length <= 4800)
        {
            new MyAsyncTask(new MyAsyncTask.Listener()
            {
                int currentPacket = 0, numberOfPacjets = 0;

                @Override
                public void onPreExecute()
                {
                    numberOfPacjets = (userData.length / 240)+1;
                    Log.d("My App", "START");
                }

                @Override
                public Long doInBackground(MyAsyncTask task)
                {
                    long ret = System.currentTimeMillis();

                    do
                    {
                        if (currentPacket < (numberOfPacjets - 1))
                        {
                            final byte[] data = new byte[244];

                            data[0] = (byte) 0x41;  // ID_EXTENDED_BUFFER_NO_CRC
                            data[1] = (byte) (240 + 2);
                            data[2] = (byte) numberOfPacjets;
                            data[3] = (byte) (currentPacket + 1);
                            System.arraycopy(userData, currentPacket * 240, data, 4, 240);
                            //noinspection StatementWithEmptyBody
                            mBlePickitManager.writeCharacteristic1501(data);
                        }
                        else
                        {
                            final byte[] data = new byte[(userData.length % 240) + 4];

                            data[0] = (byte) 0x41;  // ID_EXTENDED_BUFFER_NO_CRC
                            data[1] = (byte) ((userData.length % 240) + 2);
                            data[2] = (byte) numberOfPacjets;
                            data[3] = (byte) (currentPacket + 1);
                            System.arraycopy(userData, currentPacket * 240, data, 4, userData.length % 240);
                            //noinspection StatementWithEmptyBody
                            mBlePickitManager.writeCharacteristic1501(data);
                        }

                        task.executeOnProgressUpdate(numberOfPacjets, currentPacket+1);

                        currentPacket++;
                    }
                    while (currentPacket < numberOfPacjets);

                    return (System.currentTimeMillis() - ret);
                }

                @Override
                public void onPostExecute(Long result)
                {
                    Log.d("My App", "FINISH: "+result+" ms.");
                }

                @Override
                public void onProgressUpdate(Integer... values)
                {
                    Log.d("My App", "packet: "+values[1]+"/"+values[0]);
                }
            });

            return true;
        }
        return false;
    }

    public void requestTestStop()
    {
        final byte[] data = new byte[1];
        data[0] = (byte) 0x01;
        mBlePickitManager.writeCharacteristic1502(data);
    }

    public void requestTest1Ko()
    {
        final byte[] data = new byte[1];
        data[0] = (byte) 0x02;
        mBlePickitManager.writeCharacteristic1502(data);
        // Déclencher un compteur entre l'interruption sur l'envoi de la donnée et la réception des données.
        // Compter le nombre de données reçues + temps.
    }

    public void requestTest1Mo()
    {
        final byte[] data = new byte[1];
        data[0] = (byte) 0x03;
        mBlePickitManager.writeCharacteristic1502(data);
        // Déclencher un compteur entre l'interruption sur l'envoi de la donnée et la réception des données.
        // Compter le nombre de données reçues + temps.
    }

    public void requestTest60Seconds()
    {
        final byte[] data = new byte[1];
        data[0] = (byte) 0x04;
        mBlePickitManager.writeCharacteristic1502(data);
        // Déclencher un compteur entre l'interruption sur l'envoi de la donnée et la réception des données.
        // Compter le nombre de données reçues + temps.
    }

    @Override
    public void onChar1501DataReceived(byte[] data)
    {

    }

    @Override
    public void onChar1502DataReceived(byte[] data)
    {
        testTickStop = System.currentTimeMillis() - testTickStart;
        testLengthBytes += data.length;
        Log.d("My App", "Test reception - total data length: " +testLengthBytes+ " (+" +data.length+ " bytes) / time: " + testTickStop + " ms.");
    }

    @Override
    public void onChar1503DataReceived(byte[] data)
    {
        if ((data[0] == 0x00) && (data.length == 15))    // Get BLE PARAMS (data received from BLE PICKIT)
        {
            mBlePickit_1503_GET_BLE_PARAMETERS.postValue(data);
        }
        else if ((data[0] == 0x09) && (data.length == 11))    // Get parameters from Standard Prototype Controller
        {
            mBlePickit_1503_GET_SPC_PARAMETERS.postValue(data);
        }
    }

    @Override
    public void onChar1501DataSent(byte[] data)
    {

    }

    @Override
    public void onChar1502DataSent(byte[] data)
    {
        testTickStart = System.currentTimeMillis();
        testLengthBytes = 0;
        Log.d("My App", "Test Started - data length: " +data.length);
    }

    @Override
    public void onChar1503DataSent(byte[] data)
    {

    }

    @Override
    public void onDeviceConnecting(BluetoothDevice device)
    {
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Connecting...");
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device)
    {
        mBlePickitIsConnected.postValue(true);
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Connected - Services Discovering...");
    }

    @Override
    public void onDeviceDisconnecting(BluetoothDevice device)
    {
        mBlePickitIsConnected.postValue(false);
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Disconnecting...");
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device)
    {
        mBlePickitIsConnected.postValue(false);
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Disconnected");
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device)
    {
        mBlePickitIsConnected.postValue(false);
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Link Loss");
    }

    @Override
    public void onServicesDiscovered(BluetoothDevice device, boolean optionalServicesFound)
    {
        mBlePickitIsReady.postValue(false);
        mBlePickitConnectionState.postValue("Services Discovered");
    }

    @Override
    public void onDeviceReady(BluetoothDevice device)
    {
        mBlePickitConnectionState.postValue(device.getName() + " ready");
        mBlePickitIsReady.postValue(true);
    }

    @Override
    public void onBondingRequired(BluetoothDevice device)
    {

    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device)
    {

    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device)
    {

    }

    @Override
    public void onError(BluetoothDevice device, String message, int errorCode)
    {

    }

    @Override
    public void onDeviceNotSupported(BluetoothDevice device)
    {
        // Called when service discovery has finished but the main services were not found on the device.
        mBlePickitConnectionState.postValue("Services Discovering finised. Main services not found.");
    }
}
