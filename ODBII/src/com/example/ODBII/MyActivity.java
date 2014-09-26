package com.example.ODBII;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private String _httpRequestUrl="http://192.168.1.13/new_tms/work/carInfo.json";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
        Runnable httpPost = new HttpPostRequest(_httpRequestUrl,"speed:0,battery:50");
        Thread SendHttpPost = new Thread(httpPost);
        SendHttpPost.start();
        new Thread(new Runnable() {
            public void run() {

            }
        }).start();
    }
}
