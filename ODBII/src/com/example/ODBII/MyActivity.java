package com.example.ODBII;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private String _httpRequestUrl="http://192.168.1.13/new_tms/work/carInfo.json";
    private final long LOCATION_REFRESH_TIME=1;
    private final float LOCATION_REFRESH_DISTANCE=1;
    private Activity myActivity;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        myActivity=this;
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
}
