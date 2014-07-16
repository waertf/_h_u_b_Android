package com.example.yourapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

public class YourActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    TextView txtView=null;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        txtView=(TextView)findViewById(R.id.textView);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("WavegisService"));

        Intent intent1 = new Intent();
        intent1.setClass(this, WavegisService.class);
        startService(intent1);
        handler.postDelayed(showTime, 1000);
    }

    private void send(String test) {
        Log.d("sender", "YourActivity Broadcasting message");
        Intent intent = new Intent("YourActivity");
        // You can also include some extra data.

        intent.putExtra("message",test);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            txtView.append(message);
            Log.d("YourActivity receiver", "Got message: " + message);
        }
    };
    private Handler handler = new Handler();
    private Runnable showTime = new Runnable() {
        public void run() {
            //log目前時間
            //Log.i("time:", new Date().toString());
            SendBroadcast("速度:電量");
            handler.postDelayed(this, 1000);
        }
    };
    private void SendBroadcast(String txt) {
        Intent intent = new Intent("YourActivity");
        intent.putExtra("message",txt);
        Log.d("sender", "YourActivity Broadcasting message:"+txt);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
