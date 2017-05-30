package com.indusos.plusi;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.bebound.common.exception.BeBoundException;
import com.bebound.common.listener.request.OnFailedListener;
import com.bebound.common.listener.request.OnSentListener;
import com.bebound.common.listener.request.OnSuccessListener;
import com.bebound.common.model.request.Request;
import com.bebound.sdk.request.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.Excluder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

   private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(new BeBoundServices(), 2000);

        new Handler().postDelayed(new IndusNativeCode(), 4000);
    }


    public static class IndusNativeCode implements Runnable {

        private long startTime = 0L;
        private final OkHttpClient client = new OkHttpClient();
        private String serverUrl = "https://wowelb-1747564302.indusos.com/content/news?language=en&category=top&pageLimit=500";

        @Override
        public void run() {


            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(serverUrl)
                    .header("Accept-Encoding", "gzip")
                    .build();

            startTime = System.currentTimeMillis();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    Log.e(TAG,"Indus native call failed with exception ",e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {

                        Log.e(TAG,String.format("invalid response received, %s",response.body().toString()));
                    }

                    //unzip data

                    StringBuffer strBuffer = new StringBuffer();
                    StringBuilder sb = new StringBuilder(strBuffer);
                    try (BufferedReader rdr = new BufferedReader(new InputStreamReader(new GZIPInputStream(response.body().byteStream())))) {
                        for (int c; (c = rdr.read()) != -1; ) {
                            sb.append((char) c);

                        }
                    }
                    String unzippedData = sb.toString();

                    Log.d(TAG,String.format("Indus Native call Response time : %d",System.currentTimeMillis()-startTime));


                    response.body().close();
                }
            });
        }
    }



    public static class BeBoundServices implements Runnable {

        private long startTime = 0L;
        @Override
        public void run() {

            FetchNewsRequest fetchNewsRequest = FetchNewsRequest.builder();
            fetchNewsRequest.pageLimit = 500;
            fetchNewsRequest.category = "top";
            fetchNewsRequest.language = "en";
            try {
                fetchNewsRequest.toRequest().onSent(new OnSentListener() {
                    @Override
                    public boolean onSent(Context context, Request request) {

                        startTime = System.currentTimeMillis();
                        Log.d(TAG,"Be-Bound request sent successfully");
                        return true;
                    }
                }).onFailed(new OnFailedListener() {
                    @Override
                    public boolean onRequestFailed(Context context, Request request, int requestStatusCode, String requestStatusMessage) {
                       Log.e(TAG,String.format("Be-Bound request failed. request = {%s},requestStatusCode = (%d},requestStatusMessage={%s}",request.toString(),requestStatusCode,requestStatusMessage ));
                        return true;
                    }

                    @Override
                    public boolean onResponseError(Context context, Request request, com.bebound.common.model.request.Response response, int responseStatusCode, String responseStatusMessage) {
                       Log.e(TAG,String.format("Be-Bound response error, request = {%s}, response = {%s},responseStatusCode = {%d},responseStatusMessage={%s}",request.toString(),response.toString(),responseStatusCode,responseStatusMessage));
                        return true;
                    }

                    @Override
                    public boolean onTimeout(Context context, Request request) {

                        Log.e(TAG,String.format("Be-Bound timeout occurred. request = {%s}",request.toString()));
                        return true;
                    }

                    @Override
                    public boolean onResponseNotValidated(Context context, Request request, com.bebound.common.model.request.Response response, int validationResult) {
                        Log.e(TAG,String.format("Be-Bound response not validated. request = {%s}, response = {%s}, validationResult = {%d}" ,request.toString(),response.toString(),validationResult));
                        return true;
                    }

                    @Override
                    public boolean onFallbackTimeout(Context context, Request request) {
                        Log.e(TAG,String.format("Be-Bound fallback timeout occurred. request = {%s}",request.toString()));
                       return true;
                    }
                })
                        .onSuccess(new OnSuccessListener() {
                            @Override
                            public boolean onSuccess(Context context, com.bebound.common.model.request.Response response, Request request) {

                                Log.d(TAG,String.format("Be-Bound Response time : %d",System.currentTimeMillis()-startTime));
                                Log.d(TAG,String.format("Be-Bound response successfully received. request = {%s},response = {%s}",request.toString(),response.toString()));

                                return super.onSuccess(context, response, request);
                            }
                        }).forceType(com.bebound.common.model.request.Request.Type.HTTP).send();

            }catch (BeBoundException e){

                Log.e(TAG,"Be-Bound request failed with exception ",e);
            }

        }
    }
}