package com.example.ODBII;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wavegisAAA on 9/26/2014.
 */
public class HttpPostRequest implements Runnable {
    private String parameter;
    private String URL;
    private String result;
    public HttpPostRequest(String URL,String parameter)
    {
        this.URL=URL;
        this.parameter=parameter;
    }
    @Override
    public void run() {
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
    public String getResult()
    {
        return "send http post-" + parameter + "-result:" + result;
    }
}
