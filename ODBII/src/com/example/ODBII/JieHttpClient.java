package com.example.ODBII;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wavegisAAA on 9/26/2014.
 */
public class JieHttpClient {
    public static String GET(String url){
        String result = "";
        HttpGet get = new HttpGet(url);
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(get);
            if(httpResponse.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
                result = EntityUtils.toString(httpResponse.getEntity());
            }else{
                result = "JieHttpClient GET Fail";
            }
        }catch(ClientProtocolException e){
            System.out.println("JieHttpClient GET Error = "+e.getMessage().toString());
        }catch (IOException e){
            System.out.println("JieHttpClient GET Error = "+e.getMessage().toString());
        }catch (Exception e){
            System.out.println("JieHttpClient GET Error = "+e.getMessage().toString());
        }
        return result;
    }

    public static String POST(String url,HashMap<String,String> paramsMap){
        String result = "";
        HttpPost post = new HttpPost(url);

        try {

            if(paramsMap!=null){
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for(Map.Entry<String,String> entry:paramsMap.entrySet()){
                    params.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
                }
                HttpEntity httpEntity = new UrlEncodedFormEntity(params,"utf-8");
                post.setEntity(httpEntity);
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(post);
            if(httpResponse.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
                result = EntityUtils.toString(httpResponse.getEntity());
            }else{
                result = "JieHttpClient POST Fail";
            }

        }catch(ClientProtocolException e){
            System.out.println("JieHttpClient POST Error = " + e.getMessage().toString());
        }catch (IOException e){
            System.out.println("JieHttpClient POST Error = " + e.getMessage().toString());
        }catch (Exception e){
            System.out.println("JieHttpClient POST Error = " + e.getMessage().toString());
        }
        return result;
    }
}
