package com.example.ODBII;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.control.TroubleCodesObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineLoadObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import pt.lighthouselabs.obd.commands.engine.ThrottlePositionObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelConsumptionRateObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import pt.lighthouselabs.obd.commands.pressure.FuelPressureObdCommand;
import pt.lighthouselabs.obd.commands.pressure.IntakeManifoldPressureObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import pt.lighthouselabs.obd.commands.temperature.EngineCoolantTemperatureObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private final int _timeoutObdCommand = 1000;
    private final String _httpRequestUrl="http://192.168.1.13/new_tms/work/carInfo.json";
    private final long LOCATION_REFRESH_TIME=1;
    private final float LOCATION_REFRESH_DISTANCE=1;
    private Activity myActivity;
    private BluetoothAdapter mBluetoothAdapter = null;
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE =UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String ODBIIDeviceName="OBD2 TPMS";
    String GetBT6000sBTName=null;
    final String FuelLevelInputCmd="012F\r";
    private String ODBIIMacAddress="";
    Thread connectThread=null,connectedThread=null,sendFuelLevelInputCmd=null;
    BluetoothSocket BTSocket=null;
    Boolean normalClose=null;
    Boolean isConnected=null;
    Context context=null;
    private final BroadcastReceiver mReceiver=new BroadcastReceiver(){
        public void onReceive(Context context,Intent intent){
            String action=intent.getAction();
            //找到裝置後的處理
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //取得名稱
                /*
                if(device.getName()==null){mNewDevicesArryAdapter.add("unKnow");}
                else
                {mNewDevicesArryAdapter.add(device.getName());}
                mAddress.add(device.getAddress());//取得地址//mAddress是我拿來放實體地址的地方
                ArrayAdapter<String> adapter=new ArrayAdapter<String>//創建LIST
                        (LIST創建參數1,LIST創建參數2);
                        */
                //我的方法是創建一個  LIST然後顯示出來 按下LIST之後進行連線

                if(device.getName()!=null){
                    Log.d("alonso1","device:"+device.getName() + "\n" + device.getAddress());
                    if(device.getName().contains(ODBIIDeviceName)){
                        GetBT6000sBTName=device.getName();
                        connectThread = new ConnectThread(device);
                        connectThread.start();
                    }
                }
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        myActivity=this;
        context = getApplicationContext();
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            boolean result =mBluetoothAdapter.enable();
            Log.i("mBluetoothAdapter.enable",String.valueOf(result));
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
// If there are paired devices
        ListView Olalist = (ListView) this.findViewById(R.id.listview1);
        ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1);
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d("alonso2","device:"+device.getName() + "\t" + device.getAddress()+"\t"+device.getName().contains(ODBIIDeviceName));
                if(device.getName().contains(ODBIIDeviceName)){
                    GetBT6000sBTName=device.getName();
                    //connect to device
                    connectThread = new ConnectThread(device);
                    connectThread.start();
                    break;
                }
            }
        }
        else {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//註冊找到藍芽廣播
            registerReceiver(mReceiver,filter);
            mBluetoothAdapter.startDiscovery();
        }
        Olalist.setAdapter(mArrayAdapter);
        /*
        new Thread(new Runnable() {
            public void run() {
                //POST
                HashMap<String,String> params = new HashMap<String, String>();
                params.put("key1","value1");
                params.put("key2","value2");
                String result = JieHttpClient.POST(_httpRequestUrl,params);
                System.out.println("result:"+result);
            }
        }).start();
*/
        //set one message to http post
        //SendHttpPost("speed:10,battery:50");


        //
        new Thread(new Runnable() {
            public void run() {

            }
        }).start();
        /*
        //set location
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);
                */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            normalClose=true;
            if(connectedThread!=null) {
                connectedThread.interrupt();
                connectedThread=null;
            }
            if(connectThread!=null) {
                connectThread.interrupt();
                connectThread=null;
            }
            if(sendFuelLevelInputCmd!=null)
            {
                sendFuelLevelInputCmd.interrupt();
                sendFuelLevelInputCmd=null;
            }
            // Make sure we're not doing discovery anymore
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
            }
            while (!connectedThread.isInterrupted());
            if(BTSocket!=null)
                BTSocket.close();
            BTSocket=null;
            // Unregister broadcast listeners
            this.unregisterReceiver(mReceiver);
        }
        catch (Exception e)
        {
            Log.d(this.toString(),e.toString());
        }

    }

    private String SendHttpPost(String message) {
        final HttpPostRequest httpPost = new HttpPostRequest(_httpRequestUrl,message);
        Thread SendHttpPost = new Thread(httpPost);
        SendHttpPost.start();
        try {
            SendHttpPost.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final String messageWithNewLineAndResult = httpPost.getResult()+"\n"+message.replaceAll(",","\n");
        myActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView myTextView = (TextView)findViewById(R.id.mytextview);
                myTextView.setText(messageWithNewLineAndResult);
            }
        });
        return messageWithNewLineAndResult;
    }
/*
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            final String loca="lat:"+location.getLatitude()+","+"long:"+location.getLongitude();
            Runnable httpPost = new HttpPostRequest(_httpRequestUrl,loca);
            Thread SendHttpPost = new Thread(httpPost);
            SendHttpPost.start();
            Log.d(this.toString(),loca);

            myActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView myTextView = (TextView)findViewById(R.id.mytextview);
                    myTextView.setText(loca);
                }
            });
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
    */
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            //synchronized (MyActivity.this) {
                isConnected = false;
            //}

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                    //tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public synchronized void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    mmSocket.connect();
                    //synchronized (MyActivity.this) {
                        isConnected = true;
                    sendToast("Connected");
                    //}
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    Log.d("mmSocket.connect()", connectException.toString());
                    try {
                        mmSocket.close();
                        sendToast("Connect lost");
                    } catch (IOException closeException) {
                        Log.d("mmSocket.close()", closeException.toString());
                    }
                    return;
                }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            //synchronized (this) {
                connectThread = null;
            //}

            connectedThread=new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
                sendToast("Connect lost");
                interrupt();
            } catch (IOException e) { }
        }
    }
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final double ODB2SendDelayTime=15*1000;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = BTSocket=socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            sendFuelLevelInputCmd = new SendFuelLevelInputCmd(mmOutStream);
            sendFuelLevelInputCmd.start();
            //write("ats\r".getBytes());
            // execute commands
            /*
            try {
                new EchoOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
                new LineFeedOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
                new TimeoutObdCommand(_timeoutObdCommand).run(socket.getInputStream(), socket.getOutputStream());
                new SelectProtocolObdCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                new AmbientAirTemperatureObdCommand().run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                // handle errors
            }
            */
        }

        public void run() {
            byte[] buffer = null;  // buffer store for the stream
            int bytes; // bytes returned from read()
            StringBuilder stringBuilderHttpPost=new StringBuilder();
            int Fstatus, EngineTemperature = 0,FuelPressure = 0,IntakeManifoldPressure = 0,Rpm = 0,Speed = 0,IntakeAirTemperature = 0,AirFlowRate = 0;
            StringBuffer FstatusSB = null;
            double EngineLoading = 0,ThrottlePosition = 0,BatteryVoltag = 0,FuelLevelInput=0;
            String DTC = null,LFT= null,RFT= null,LRT= null,RRT= null;
            // Keep listening to the InputStream until an exception occurs
            double ODB2startTime=0;

            while (!Thread.currentThread().isInterrupted()) {
                try {
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
                        } else
                        {
                            if(firstThree[0]==0x34 && firstThree[1]==0x31)
                            {
                                buffer = new byte[11 - firstThree.length];
                                head[0] = (byte) firstThree[0];
                                head[1] = (byte) firstThree[1];
                                head[2] = (byte) firstThree[2];
                            }
                            else
                                continue;
                        }
                    }
                    if(buffer.length>0)
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
                                            for(int i=0;i<10;i++)
                                                error[i]=data[i+2];
                                            StringBuffer stringBuffer=new StringBuffer();
                                            for(int i=0;i<10;i+=2)
                                            {
                                                byte first,second;
                                                String header;
                                                first=error[i];
                                                second=error[i+1];
                                                /*if(first+second!=0)*/ {
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
                                            DTC=stringBuffer.toString();
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
                                                            LFT="LFT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(LFT);//胎溫 單位：°C 胎壓 單位：Psi 電池電壓 單位：V
                                                            break;
                                                        case 2://the sensor is assigned to Right Front tire.
                                                            RFT="RFT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(RFT);
                                                            break;
                                                        case 3://the sensor is assigned to Right Rear tire.
                                                            RRT="RRT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
                                                            //stringBuilderHttpPost.append(RRT);
                                                            break;
                                                        case 4://the sensor is assigned to Left Rear tire.
                                                            LRT="LRT:" + Temperature + "-" + Pressure + "-" + BatteryVoltage + ",";
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
                            if(data[0]==0x34 && data[1]==0x31 && data[3]==0x32 &&data[4]==0x46)
                            {
                                //Log.d("data[6]-'0'", String.valueOf((data[6]-'0')));
                                //Log.d("data[7]-'0'", String.valueOf((data[7]-'0')));
                                FuelLevelInput = ((data[6]-'0')*16+(data[7]-'0')) * 100 / 255;
                            }
                            break;
                    }
                    if (System.currentTimeMillis() - ODB2startTime > ODB2SendDelayTime) {
                        ODB2startTime = 0;
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
                        stringBuilderHttpPost.append(DTC);
                        DTC=null;
                        if(LFT!=null)
                        stringBuilderHttpPost.append(LFT);//胎溫 單位：°C 胎壓 單位：Psi 電池電壓 單位：V
                        if(RFT!=null)
                        stringBuilderHttpPost.append(RFT);
                        if(RRT!=null)
                        stringBuilderHttpPost.append(RRT);
                        if(LRT!=null)
                        stringBuilderHttpPost.append(LRT);
                        stringBuilderHttpPost.append("FuelLevelInput:"+FuelLevelInput+",");
                    }

                    if (stringBuilderHttpPost.length() > 0)
                    {
                        Log.d("alonso3", stringBuilderHttpPost.toString());
                        //Log.d(this.toString(),SendHttpPost(stringBuilderHttpPost.toString()));
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
                        final String receive = new String(stringBuilderHttpPost.toString());
                        myActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                TextView myTextView = (TextView) findViewById(R.id.mytextview);
                                myTextView.setText(receive);
                            }
                        });
                        stringBuilderHttpPost.setLength(0);
                    }
                } catch (Exception e) {
                    normalClose=isConnected=false;
                    Log.d("ConnectedThread111",e.toString());
                    cancel();
                    //while (!isConnected) {
                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
// If there are paired devices
                        //ListView Olalist = (ListView) this.findViewById(R.id.listview1);
                        //ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1);
                        if (pairedDevices.size() > 0) {
                            // Loop through paired devices
                            for (BluetoothDevice device : pairedDevices) {
                                // Add the name and address to an array adapter to show in a ListView
                                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                Log.d("alonso2", "device:" + device.getName() + "\n" + device.getAddress() + "\n" + ODBIIDeviceName.contains(device.getName()));
                                if (device.getName().contains(ODBIIDeviceName)) {
                                    //connect to device
                                    while (!isConnected) {
                                        connectThread = new ConnectThread(device);
                                        connectThread.start();
                                        try
                                        {
                                            Thread.sleep(1000);
                                        }
                                        catch (InterruptedException ex)
                                        {
                                            Thread.currentThread().interrupt(); // restore interrupted status
                                            break;
                                        }
                                    }
                                    break;

                                }
                            }
                        } else {
                            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//註冊找到藍芽廣播
                            registerReceiver(mReceiver, filter);
                            mBluetoothAdapter.startDiscovery();
                        }
                    //}
                    //Olalist.setAdapter(mArrayAdapter);
                    //cancel();
                    break;
                }
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                    break;
                }
            }
            while (normalClose==null);
            if(normalClose==true)
            cancel();
            /*
            EngineRPMObdCommand engineRpmCommand = new EngineRPMObdCommand();
            SpeedObdCommand speedCommand = new SpeedObdCommand();
            ThrottlePositionObdCommand throttlePositionObdCommand = new ThrottlePositionObdCommand();
            EngineCoolantTemperatureObdCommand engineCoolantTemperatureObdCommand = new EngineCoolantTemperatureObdCommand();
            MassAirFlowObdCommand massAirFlowObdCommand = new MassAirFlowObdCommand();
            TroubleCodesObdCommand troubleCodesObdCommand = new TroubleCodesObdCommand();
            EngineLoadObdCommand engineLoadObdCommand = new EngineLoadObdCommand();
            //ThrottlePositionObdCommand throttlePositionObdCommand1 = new ThrottlePositionObdCommand();

            FuelLevelObdCommand fuelLevelObdCommand = new FuelLevelObdCommand();
            FuelPressureObdCommand fuelPressureObdCommand = new FuelPressureObdCommand();
            FuelConsumptionRateObdCommand fuelConsumptionRateObdCommand = new FuelConsumptionRateObdCommand();
            IntakeManifoldPressureObdCommand intakeManifoldPressureObdCommand = new IntakeManifoldPressureObdCommand();

            while (!Thread.currentThread().isInterrupted())
            {
                try {
                    engineRpmCommand.run(mmInStream, mmOutStream);
                    speedCommand.run(mmInStream, mmOutStream);
                    throttlePositionObdCommand.run(mmInStream,mmOutStream);
                    engineCoolantTemperatureObdCommand.run(mmInStream,mmOutStream);
                    massAirFlowObdCommand.run(mmInStream,mmOutStream);
                    troubleCodesObdCommand.run(mmInStream,mmOutStream);
                    engineLoadObdCommand.run(mmInStream,mmOutStream);
                    //throttlePositionObdCommand1.run(mmInStream,mmOutStream);

                    fuelLevelObdCommand.run(mmInStream,mmOutStream);
                    fuelPressureObdCommand.run(mmInStream,mmOutStream);
                    fuelConsumptionRateObdCommand.run(mmInStream,mmOutStream);
                    intakeManifoldPressureObdCommand.run(mmInStream,mmOutStream);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final String RPM,Speed,ThrottlePosition,EngineCoolantTemperature,
                        MassAirFlow,TroubleCodes,EngineLoad,FuelLevel,
                        FuelPressure,FuelConsumptionRate,IntakeManifoldPressure,
                CombineWithComma,CombineWithNewLine;
                RPM="RPM:" + engineRpmCommand.getFormattedResult();
                Speed="Speed:" + speedCommand.getFormattedResult();
                ThrottlePosition="ThrottlePosition:" + throttlePositionObdCommand.getFormattedResult();
                EngineCoolantTemperature="EngineCoolantTemperature:" + engineCoolantTemperatureObdCommand.getFormattedResult();
                MassAirFlow="MassAirFlow:" + massAirFlowObdCommand.getFormattedResult();
                TroubleCodes="TroubleCodes:" + troubleCodesObdCommand.getFormattedResult();
                EngineLoad="EngineLoad:" + engineLoadObdCommand.getFormattedResult();

                FuelLevel="FuelLevel:" + fuelLevelObdCommand.getFormattedResult();
                FuelPressure="FuelPressure:" + fuelPressureObdCommand.getFormattedResult();
                FuelConsumptionRate="FuelConsumptionRate:" + fuelConsumptionRateObdCommand.getFormattedResult();
                IntakeManifoldPressure="IntakeManifoldPressure:" + intakeManifoldPressureObdCommand.getFormattedResult();

                CombineWithComma=RPM+","+
                        Speed+","+
                        ThrottlePosition+","+
                        EngineCoolantTemperature+","+
                        MassAirFlow+","+
                        TroubleCodes+","+
                        EngineLoad+","+
                        FuelLevel+","+
                        FuelPressure+","+
                        FuelConsumptionRate+","+
                        IntakeManifoldPressure+",";
                //SendHttpPost(CombineWithComma);
                //CombineWithNewLine=CombineWithComma.replaceAll(",","\n");
                Log.d(this.toString(),SendHttpPost(CombineWithComma));

                // TODO handle commands result
                Log.d(this.toString(), "RPM: " + engineRpmCommand.getFormattedResult());
                Log.d(this.toString(), "Speed: " + speedCommand.getFormattedResult());
                Log.d(this.toString(), "ThrottlePosition: " + throttlePositionObdCommand.getFormattedResult());
                Log.d(this.toString(), "EngineCoolantTemperature: " + engineCoolantTemperatureObdCommand.getFormattedResult());
                Log.d(this.toString(), "MassAirFlow: " + massAirFlowObdCommand.getFormattedResult());
                Log.d(this.toString(), "TroubleCodes: " + troubleCodesObdCommand.getFormattedResult());
                Log.d(this.toString(), "EngineLoad: " + engineLoadObdCommand.getFormattedResult());
                //Log.d(this.toString(), "ThrottlePosition: " + throttlePositionObdCommand1.getFormattedResult());

                Log.d(this.toString(), "FuelLevel: " + fuelLevelObdCommand.getFormattedResult());
                Log.d(this.toString(), "FuelPressure: " + fuelPressureObdCommand.getFormattedResult());
                Log.d(this.toString(), "FuelConsumptionRate: " + fuelConsumptionRateObdCommand.getFormattedResult());
                Log.d(this.toString(), "IntakeManifoldPressure: " + intakeManifoldPressureObdCommand.getFormattedResult());
                */
                /*
                myActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        TextView myTextView = (TextView)findViewById(R.id.mytextview);
                        myTextView.setText(CombineWithNewLine);
                    }
                });

            }
            */
        }
        public  char toHexChar(int input) {
            return input >= 0 && input <= 9?(char)(input + 48):(char)(65 + (input - 10));
        }
        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
                sendToast("Connect lost");
                interrupt();
            } catch (IOException e) { }
        }

    }
    public void sendToast(final CharSequence text )
    {
        myActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(myActivity, text, Toast.LENGTH_SHORT).show();
            }
        });
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
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                    break;
                }
                write("ats\r".getBytes());
                try
                {
                    Thread.sleep(60*1000);
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
