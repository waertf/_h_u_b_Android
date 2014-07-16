package com.example.yourapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Created by wavegis on 2014/7/8.
 */
public class WavegisService extends Service {
    private Handler handler = new Handler();
    private Handler closeDiscoverableHandler=new Handler();
    private BluetoothAdapter mBluetoothAdapter = null;
    private int closeDiscoverableTimeout=300*1000;//unit:sec
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handler.postDelayed(showTime, 1000);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("YourActivity"));
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }
        super.onStart(intent, startId);
        if (!mBluetoothAdapter.isEnabled()) {
            boolean result =mBluetoothAdapter.enable();
            SendBroadcast("BT enable");
            Log.i("mBluetoothAdapter.enable",String.valueOf(result));
        }
        setDiscoverableTimeout(closeDiscoverableTimeout);
        SendBroadcast("BT Discoverable");
        closeDiscoverableHandler.postDelayed(CloseDiscoverableRunnable,closeDiscoverableTimeout);
        /*
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
*/
        /*
        Logger log = LoggerFactory.getLogger(WavegisService.class);

        log.info("info");
        log.warn("warn");
        log.error("error");
        */

    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(showTime);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
    private Runnable showTime = new Runnable() {
        public void run() {
            //log目前時間
            //Log.i("time:", new Date().toString());
            SendBroadcast("任務:路況");
            handler.postDelayed(this, 1000);
        }
    };
    private Runnable CloseDiscoverableRunnable=new Runnable(){
        @Override
        public void run() {
            closeDiscoverableTimeout();
        }
    };

    private void SendBroadcast(String txt) {
        Intent intent = new Intent("WavegisService");
        intent.putExtra("message",txt);
        Log.d("sender", "WavegisService Broadcasting message:"+txt);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("WavegisService receiver", "Got message: " + message);
        }
    };
    public void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeDiscoverableTimeout() {
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE,1);
            SendBroadcast("BT Close Discoverable");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
