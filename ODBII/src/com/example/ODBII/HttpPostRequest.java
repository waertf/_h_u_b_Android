package com.example.ODBII;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wavegisAAA on 9/26/2014.
 */
public class HttpPostRequest extends AsyncTask<String,String,String> {
    private String parameter;
    private String URL;
    private String result;
    private Context mContext;
    private TaskCompleted mCallback;

    public HttpPostRequest(Context context) {
        this.mCallback = (TaskCompleted) context;
        this.mContext = context;
    }
    /*
    @Override
    public void run() {
        try {
            HashMap<String, String> myMap = new HashMap<String, String>();
            String s = "SALES:0,SALE_PRODUCTS:1,EXPENSES:2,EXPENSES_ITEMS:3";
            String[] pairs = parameter.split(",");
            for (int i=0;i<pairs.length;i++) {
                String pair = pairs[i];
                String[] keyValue = pair.split(":");
                myMap.put(keyValue[0], keyValue[1]);
            }
            result = JieHttpClient.POST(URL,myMap);
            Log.d(this.toString(),"send http post-" + parameter + "-result:" + result);
        }
        catch (Exception ex){
            Log.d(this.toString(),ex.toString());
        }

    }
    public String getResult()
    {
        return "send http post-" + parameter + "-result:" + result;
    }
    */
    @Override
    protected String doInBackground(String... strings) {
        this.URL=strings[0];
        this.parameter=strings[1];
        java.util.Date date= new java.util.Date();
        try {
            HashMap<String, String> myMap = new HashMap<String, String>();
            String s = "SALES:0,SALE_PRODUCTS:1,EXPENSES:2,EXPENSES_ITEMS:3";
            String[] pairs = parameter.split(",");
            for (int i=0;i<pairs.length;i++) {
                String pair = pairs[i];
                String[] keyValue = pair.split(":");
                myMap.put(keyValue[0], keyValue[1]);
            }
            result = JieHttpClient.POST(URL,myMap);
            Log.d(this.toString(),"send http post-" + parameter + "-result:" + result);
        }
        catch (Exception ex){
            Log.d(this.toString(),ex.toString());
        }
        return "-send http post:"+new Timestamp(date.getTime())+ "," + parameter + "-result:" + result;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        mCallback.onTaskComplete(s);
    }
}
