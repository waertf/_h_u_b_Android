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
    private final String ODBIIDeviceName="HTC Butterfly s";
    private String ODBIIMacAddress="";
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
                    if(ODBIIDeviceName.equalsIgnoreCase(device.getName())){
                        Thread connectThread = new ConnectThread(device);
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
                Log.d(this.toString(),"device:"+device.getName() + "\n" + device.getAddress());
                if(ODBIIDeviceName.equalsIgnoreCase(device.getName())){
                    //connect to device
                    Thread connectThread = new ConnectThread(device);
                    connectThread.start();
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

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
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
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            new ConnectedThread(mmSocket).start();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
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
            write("ats\r".getBytes());
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
            final byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // Read from the InputStream
                    while ((bytes = mmInStream.read(buffer))!=-1)
                    {
                        baos.write(buffer, 0, bytes);
                    }
                    byte data [] = baos.toByteArray();
                    baos.close();
                    int[] intArray = new int[data.length];
                    String[] hexString = new String[data.length];
                    //StringBuilder sb = new StringBuilder();
                    for(int i=0;i<data.length;i++)
                    {
                        hexString[i]=(String.format("%02X", data[i]));
                        intArray[i]=(int) data[i] & 0xff;
                    }
                    StringBuilder stringBuilderHttpPost=new StringBuilder();
                    switch (data.length)
                    {
                        case 16://OBDII
                            switch (data[0])
                            {
                                case 64:
                                    switch (data[1])
                                    {
                                        case 78://Normal
                                            int Fstatus = (intArray[2]);
                                            StringBuffer FstatusSB= new StringBuffer();
                                            if((1 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to insufficient engine temperature.");
                                            } else if((2 & Fstatus) != 0) {
                                                FstatusSB.append("Closed loop and using oxygen sensor feedback to determine fuel " +
                                                        "mix.");
                                            } else if((4 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to engine load OR fuel cut due to deacceleration.");
                                            } else if((8 & Fstatus) != 0) {
                                                FstatusSB.append("Open loop due to system failure.");
                                            } else if((16 & Fstatus) != 0) {
                                                FstatusSB.append("Closed loop and using at least one oxygen sensor but there is a fault" +
                                                        " in the feedback system.");
                                            }
                                            double EngineLoading= (double)(100 * (intArray[3]) / 255);
                                            int EngineTemperature = -40 + (intArray[4]);
                                            int FuelPressure = 3 * (intArray[5]);
                                            int IntakeManifoldPressure = (intArray[6]);
                                            int Rpm = 256 * (intArray[7]) + (intArray[8]);
                                            int Speed = (intArray[9]);
                                            int IntakeAirTemperature = -40 + (intArray[10]);
                                            int AirFlowRate = (intArray[11]);
                                            double ThrottlePosition = (double)(100 * (intArray[12]) / 255);
                                            double BatteryVoltag = (double)(intArray[13])/10;
                                            stringBuilderHttpPost.append("FuelSystemStatus:"+FstatusSB.toString()+",");
                                            stringBuilderHttpPost.append("EngineLoading:"+EngineLoading+",");
                                            stringBuilderHttpPost.append("EngineTemperature:"+EngineTemperature+",");
                                            stringBuilderHttpPost.append("FuelPressure:"+FuelPressure+",");
                                            stringBuilderHttpPost.append("IntakeManifoldPressure:"+IntakeManifoldPressure+",");
                                            stringBuilderHttpPost.append("Rpm:"+Rpm+",");
                                            stringBuilderHttpPost.append("Speed:"+Speed+",");
                                            stringBuilderHttpPost.append("IntakeAirTemperature:"+IntakeAirTemperature+",");
                                            stringBuilderHttpPost.append("AirFlowRate:"+AirFlowRate+",");
                                            stringBuilderHttpPost.append("ThrottlePosition:"+ThrottlePosition+",");
                                            stringBuilderHttpPost.append("BatteryVoltag:"+BatteryVoltag+",");
                                            break;
                                        case 77://Malfunction
                                            break;
                                    }
                                    break;
                            }
                            break;

                        case 12://TPMS
                            break;
                    }
                    Log.d(this.toString(),SendHttpPost(stringBuilderHttpPost.toString()));
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
                   final String receive= new String(data,"UTF-8");
                    myActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            TextView myTextView = (TextView)findViewById(R.id.mytextview);
                            myTextView.setText(receive);
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
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
            } catch (IOException e) { }
        }
    }
}
