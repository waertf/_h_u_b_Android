package com.example.ODBII;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private String _httpRequestUrl="";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        new Thread(new Runnable() {
            public void run() {
                //POST
                HashMap<String,String> params = new HashMap<String, String>();
                params.put("key1","value1");
                params.put("key2","value2");
                String result = JieHttpClient.POST(_httpRequestUrl,params);
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {

            }
        }).start();
    }
}
