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

/**
 * Created by wavegis on 2014/7/8.
 */
public class WavegisService extends Service {
    private Handler handler = new Handler();
    private BluetoothAdapter mBluetoothAdapter = null;
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
            Log.i("mBluetoothAdapter.enable",String.valueOf(result));
        }

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
}
