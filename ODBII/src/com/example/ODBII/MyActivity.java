package com.example.ODBII;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private String _httpRequestUrl="http://192.168.1.13/new_tms/work/carInfo.json";
    private final long LOCATION_REFRESH_TIME=1;
    private final float LOCATION_REFRESH_DISTANCE=1;
    private Activity myActivity;
    private BluetoothAdapter mBluetoothAdapter = null;
    private final UUID MY_UUID= UUID.fromString("");
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
            }
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
        SendHttpPost("speed:10,battery:50");


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

    private void SendHttpPost(String message) {
        final HttpPostRequest httpPost = new HttpPostRequest(_httpRequestUrl,message);
        Thread SendHttpPost = new Thread(httpPost);
        SendHttpPost.start();
        try {
            SendHttpPost.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        myActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView myTextView = (TextView)findViewById(R.id.mytextview);
                myTextView.setText(httpPost.getResult());
            }
        });
    }

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
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
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
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            //.sendToTarget();
                    new String(buffer,"UTF-8");
                } catch (IOException e) {
                    break;
                }
            }
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
