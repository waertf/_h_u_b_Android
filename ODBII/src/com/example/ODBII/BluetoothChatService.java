package com.example.ODBII;

/**
 * Created by wavegisAAA on 10/23/2014.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    Context mContext=null;
    String GetBT6000sBTName=null;
    final String FuelLevelInputCmd="012F\r";
    Thread sendFuelLevelInputCmd=null;
    private final String _httpRequestUrl="http://192.168.1.13/new_tms/work/carInfo.json";
    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mContext=context;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MyActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MyActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MyActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.interrupt();
            mConnectedThread.cancel();
            //mConnectedThread.interrupt();
            try {
                mConnectedThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.interrupt();
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.interrupt();
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        if(sendFuelLevelInputCmd!=null)
        {
            sendFuelLevelInputCmd.interrupt();
            sendFuelLevelInputCmd=null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MyActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MyActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED && !Thread.interrupted()) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            GetBT6000sBTName=device.getName();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private double ODB2SendDelayTime=0;
        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            sendFuelLevelInputCmd = new SendFuelLevelInputCmd(mmOutStream);
            sendFuelLevelInputCmd.start();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            //byte[] buffer = new byte[1024];
            //int bytes;
            byte[] buffer = null;  // buffer store for the stream
            int bytes; // bytes returned from read()
            StringBuilder stringBuilderHttpPost=new StringBuilder();
            int Fstatus, EngineTemperature = 0,FuelPressure = 0,IntakeManifoldPressure = 0,Rpm = 0,Speed = 0,IntakeAirTemperature = 0,AirFlowRate = 0;
            StringBuffer FstatusSB = null;
            double EngineLoading = 0,ThrottlePosition = 0,BatteryVoltag = 0,FuelLevelInput=-1;
            String DTC = null,LFT= null,RFT= null,LRT= null,RRT= null;
            // Keep listening to the InputStream until an exception occurs
            double ODB2startTime=0;
            // Keep listening to the InputStream while connected
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    // Read from the InputStream
                    //bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    //mHandler.obtainMessage(MyActivity.MESSAGE_READ, bytes, -1, buffer)
                    //.sendToTarget();
                    //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // Read from the InputStream
                    int[] firstThree = new int[3];
                    for (int i = 0; i < firstThree.length; i++) {
                        if ((firstThree[i] = mmInStream.read()) != -1) {

                        }
                    }
                    byte[] head = new byte[3];
                    if ((firstThree[0] == 64 && firstThree[1] == 78) ||
                            (firstThree[0] == 64 && firstThree[1] == 77)) {
                        buffer = new byte[17 - firstThree.length];
                        head[0] = (byte) firstThree[0];
                        head[1] = (byte) firstThree[1];
                        head[2] = (byte) firstThree[2];
                    } else {
                        if (firstThree[0] == 84 && firstThree[1] == 80 && firstThree[2] == 86) {
                            buffer = new byte[13 - firstThree.length];
                            head[0] = (byte) firstThree[0];
                            head[1] = (byte) firstThree[1];
                            head[2] = (byte) firstThree[2];
                        } else {
                            if (firstThree[0] == 0x34 && firstThree[1] == 0x31) {
                                buffer = new byte[11 - firstThree.length];
                                head[0] = (byte) firstThree[0];
                                head[1] = (byte) firstThree[1];
                                head[2] = (byte) firstThree[2];
                            } else
                                continue;
                        }
                    }
                    if (buffer.length > 0)
                        if ((bytes = mmInStream.read(buffer)) != -1) {
                            //baos.write(buffer, 0, bytes);
                        } else
                            continue;
                    //byte data [] = baos.toByteArray();
                    //baos.close();
                    byte data[] = new byte[head.length + buffer.length];
                    System.arraycopy(head, 0, data, 0, head.length);
                    System.arraycopy(buffer, 0, data, head.length, buffer.length);
                    int[] intArray = new int[data.length];
                    String[] hexString = new String[data.length];
                    //StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < data.length; i++) {
                        hexString[i] = (String.format("%02X", data[i]));
                        intArray[i] = (int) data[i] & 0xff;
                    }
                    //StringBuilder stringBuilderHttpPost=new StringBuilder();
                    if (ODB2startTime == 0)
                        ODB2startTime = System.currentTimeMillis();

                    switch (data.length) {
                        case 17://OBDII
                            switch (data[0]) {
                                case 64:
                                    switch (data[1]) {
                                        case 78://Normal
                                            /*
                                            if (System.currentTimeMillis() - ODB2startTime > ODB2SendDelayTime) {
                                                ODB2startTime = 0;
                                            } else
                                                break;
                                                */
                                            Fstatus = (intArray[2]);
                                            FstatusSB = new StringBuffer();
                                            if ((1 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to insufficient engine temperature#");
                                            } else if ((2 & Fstatus) != 0) {
                                                FstatusSB.append("Closed loop and using oxygen sensor feedback to determine fuel " +
                                                        "mix#");
                                            } else if ((4 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to engine load OR fuel cut due to deacceleration#");
                                            } else if ((8 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to system failure#");
                                            } else if ((16 & Fstatus) != 0) {
                                                FstatusSB.append("Closed loop and using at least one oxygen sensor but there is a fault" +
                                                        " in the feedback system#");
                                            }
                                            EngineLoading = (double) (100 * (intArray[3]) / 255);
                                            EngineTemperature = -40 + (intArray[4]);
                                            FuelPressure = 3 * (intArray[5]);
                                            IntakeManifoldPressure = (intArray[6]);
                                            Rpm = 256 * (intArray[7]) + (intArray[8]);
                                            Speed = (intArray[9]);
                                            IntakeAirTemperature = -40 + (intArray[10]);
                                            AirFlowRate = (intArray[11]);
                                            ThrottlePosition = (double) (100 * (intArray[12]) / 255);
                                            BatteryVoltag = (double) (intArray[13]) / 10;
                                            /*
                                            stringBuilderHttpPost.append("ID:"+GetBT6000sBTName+",");
                                            stringBuilderHttpPost.append("FuelSystemStatus:" + FstatusSB.toString() + ",");//燃油系統狀態 純文字用"#"分隔
                                            stringBuilderHttpPost.append("EngineLoading:" + EngineLoading + ",");//引擎負荷 單位：%
                                            stringBuilderHttpPost.append("EngineTemperature:" + EngineTemperature + ",");//引擎溫度 單位：°C
                                            stringBuilderHttpPost.append("FuelPressure:" + FuelPressure + ",");//燃油壓力 單位：kPa
                                            stringBuilderHttpPost.append("IntakeManifoldPressure:" + IntakeManifoldPressure + ",");//進氣歧管壓力 單位：kPa
                                            stringBuilderHttpPost.append("Rpm:" + Rpm + ",");//引擎轉速 單位：rpm
                                            stringBuilderHttpPost.append("Speed:" + Speed + ",");//車輛速度 單位：km/h
                                            stringBuilderHttpPost.append("IntakeAirTemperature:" + IntakeAirTemperature + ",");//進氣溫度 單位：°C
                                            stringBuilderHttpPost.append("AirFlowRate:" + AirFlowRate + ",");//空氣流量 單位：g/s
                                            stringBuilderHttpPost.append("ThrottlePosition:" + ThrottlePosition + ",");//油門位置 單位：%
                                            stringBuilderHttpPost.append("BatteryVoltag:" + BatteryVoltag + ",");//電池電壓 單位：V
                                            */
                                            break;
                                        case 77://Malfunction
                                            byte[] error = new byte[10];
                                            for (int i = 0; i < 10; i++)
                                                error[i] = data[i + 2];
                                            StringBuffer stringBuffer = new StringBuffer();
                                            for (int i = 0; i < 10; i += 2) {
                                                byte first, second;
                                                String header;
                                                first = error[i];
                                                second = error[i + 1];
                                                /*if(first+second!=0)*/
                                                {
                                                    if (first >> 6 == 0)
                                                        header = "DTC" + (i / 2 + 1) + ":P";
                                                    else if (first >> 6 == 1)
                                                        header = "DTC" + (i / 2 + 1) + ":C";
                                                    else if (first >> 6 == -1)
                                                        header = "DTC" + (i / 2 + 1) + ":U";
                                                    else
                                                        header = "DTC" + (i / 2 + 1) + ":B";
                                                    stringBuffer.append(header +
                                                            toHexChar(15 & (first & 63) >> 4) + toHexChar(first & 15) + toHexChar(15 & second >> 4) + toHexChar(second & 15) + ",");
                                                    //stringBuilderHttpPost.append(DTC);
                                                }
                                            }
                                            if (DTC != null && stringBuffer.length() > 0)
                                                DTC = stringBuffer.toString();
                                            else
                                                DTC = "DTC1:P0000,DTC2:P0000,DTC3:P0000,DTC4:P0000,DTC5:P0000,";
                                            //stringBuilderHttpPost.append("ID:"+GetBT6000sBTName+",");
                                            break;
                                    }
                                    break;
                            }
                            break;

                        case 13://TPMS
                            switch (data[0]) {
                                case 84://T
                                    switch (data[1]) {
                                        case 80://P
                                            switch (data[2]) {
                                                case 86://V
                                                    int Temperature, Pressure;
                                                    double BatteryVoltage;
                                                    Temperature = intArray[8] - 50;
                                                    Pressure = intArray[9];
                                                    BatteryVoltage = intArray[10] / 50;
                                                    switch (data[3]) {
                                                        case 1://the sensor is assigned to Left Front tire.
                                                            LFT = "LFT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(LFT);//胎溫 單位：°C 胎壓 單位：Psi 電池電壓 單位：V
                                                            break;
                                                        case 2://the sensor is assigned to Right Front tire.
                                                            RFT = "RFT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(RFT);
                                                            break;
                                                        case 3://the sensor is assigned to Right Rear tire.
                                                            RRT = "RRT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(RRT);
                                                            break;
                                                        case 4://the sensor is assigned to Left Rear tire.
                                                            LRT = "LRT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(LRT);
                                                            break;
                                                    }
                                                    //stringBuilderHttpPost.append("ID:"+GetBT6000sBTName+",");
                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                            }
                            break;
                        case 11://Fuel Level Input
                            if (data[0] == 0x34 && data[1] == 0x31 && data[3] == 0x32 && data[4] == 0x46) {
                                //Log.d("data[6]-'0'", String.valueOf((data[6]-'0')));
                                //Log.d("data[7]-'0'", String.valueOf((data[7]-'0')));
                                FuelLevelInput = ((data[6] - '0') * 16 + (data[7] - '0')) * 100 / 255;
                            }
                            break;
                    }
                    if(FstatusSB==null)
                        continue;
                    if (System.currentTimeMillis() - ODB2startTime > ODB2SendDelayTime && data.length>0) {
                        ODB2SendDelayTime = 15 * 1000;
                        ODB2startTime = 0;
                        stringBuilderHttpPost.append("ID:" + GetBT6000sBTName + ",");
                        stringBuilderHttpPost.append("FuelSystemStatus:" + FstatusSB.toString() + ",");//燃油系統狀態 純文字用"#"分隔
                        stringBuilderHttpPost.append("EngineLoading:" + EngineLoading + ",");//引擎負荷 單位：%
                        stringBuilderHttpPost.append("EngineTemperature:" + EngineTemperature + ",");//引擎溫度 單位：°C
                        stringBuilderHttpPost.append("FuelPressure:" + FuelPressure + ",");//燃油壓力 單位：kPa
                        stringBuilderHttpPost.append("IntakeManifoldPressure:" + IntakeManifoldPressure + ",");//進氣歧管壓力 單位：kPa
                        stringBuilderHttpPost.append("Rpm:" + Rpm + ",");//引擎轉速 單位：rpm
                        stringBuilderHttpPost.append("Speed:" + Speed + ",");//車輛速度 單位：km/h
                        stringBuilderHttpPost.append("IntakeAirTemperature:" + IntakeAirTemperature + ",");//進氣溫度 單位：°C
                        stringBuilderHttpPost.append("AirFlowRate:" + AirFlowRate + ",");//空氣流量 單位：g/s
                        stringBuilderHttpPost.append("ThrottlePosition:" + ThrottlePosition + ",");//油門位置 單位：%
                        stringBuilderHttpPost.append("BatteryVoltag:" + BatteryVoltag + ",");//電池電壓 單位：V
                        if (DTC != null)
                            stringBuilderHttpPost.append(DTC);
                        //DTC=null;
                        if (LFT != null)
                            stringBuilderHttpPost.append(LFT);//胎溫 單位：°C 胎壓 單位：Psi 電池電壓 單位：V
                        if (RFT != null)
                            stringBuilderHttpPost.append(RFT);
                        if (RRT != null)
                            stringBuilderHttpPost.append(RRT);
                        if (LRT != null)
                            stringBuilderHttpPost.append(LRT);
                        stringBuilderHttpPost.append("FuelLevelInput:" + FuelLevelInput + ",");
                        FuelLevelInput=-1;
                        FstatusSB=null;
                        DTC=LFT=RFT=LRT=RRT=null;
                    }

                    if (stringBuilderHttpPost.length() > 0) {
                        Log.d("alonso3", stringBuilderHttpPost.toString());
                        Log.d(this.toString(),SendHttpPost(stringBuilderHttpPost.toString()));
                        //stringBuilderHttpPost.setLength(0);
                        /*
                        for (byte b : data) {
                            sb.append(String.format("%02X ", b));
                        }
                        */
                        //String[] hexString=sb.toString().split(" ");
                        //baos.close();
                        //bytes = mmInStream.read(buffer);
                        // Send the obtained bytes to the UI activity
                        //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        //.sendToTarget();
                        final String receive = new String(stringBuilderHttpPost.toString()).replace(",", "\n");
                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(MyActivity.MESSAGE_READ, receive.getBytes().length, -1, receive.getBytes())
                        .sendToTarget();
                        /*
                        mContext.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                TextView myTextView = (TextView).findViewById(R.id.mytextview);
                                myTextView.setText(receive);
                            }
                        });
                        */
                        stringBuilderHttpPost.setLength(0);
                    }
                }catch(IOException e){
                    if(Thread.interrupted())
                    {
                        try {
                            throw new InterruptedException();
                        } catch (InterruptedException e1) {
                            Log.e(TAG, "disconnected1", e1);
                            Message msg = mHandler.obtainMessage(MyActivity.MESSAGE_TOAST);
                            Bundle bundle = new Bundle();
                            bundle.putString(MyActivity.TOAST, "Device connection was lost");
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                            break;
                        }
                    }
                    else {
                        Log.e(TAG, "disconnected2", e);
                        connectionLost();
                        // Start the service over to restart listening mode
                        //BluetoothChatService.this.start();
                        break;
                    }
                    }
                }
            }


        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MyActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        public  char toHexChar(int input) {
            return input >= 0 && input <= 9?(char)(input + 48):(char)(65 + (input - 10));
        }
        private String SendHttpPost(String message) {
            final HttpPostRequest httpPost = new HttpPostRequest(_httpRequestUrl,message);
            Thread SendHttpPost = new Thread(httpPost);
            SendHttpPost.start();
            try {
                SendHttpPost.join(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final String messageWithNewLineAndResult = httpPost.getResult()+"\n"+message.replaceAll(",","\n");
            return messageWithNewLineAndResult;
        }
    }
    public class SendFuelLevelInputCmd extends Thread{
        private final OutputStream mmOutStream;

        SendFuelLevelInputCmd(OutputStream outStrem){
            mmOutStream=outStrem;
        }
        public void run() {
            while (!Thread.currentThread().isInterrupted())
            {
                write(FuelLevelInputCmd.getBytes());
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                    break;
                }

                write("ats\r".getBytes());
                try
                {
                    Thread.sleep(10*1000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                    break;
                }

            }
        }
        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }
    }
}

