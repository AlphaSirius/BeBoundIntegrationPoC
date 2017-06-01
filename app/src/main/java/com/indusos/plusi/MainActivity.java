package com.indusos.plusi;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bebound.common.exception.BeBoundException;
import com.bebound.common.listener.request.OnFailedListener;
import com.bebound.common.listener.request.OnSentListener;
import com.bebound.common.listener.request.OnSuccessListener;
import com.bebound.common.model.request.Request;
import com.bebound.sdk.BeBound;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static int uidBeGore = 0;
    private static int uidIndus = 0;

    private TextView lblInfosWith, lblInfosWithout;

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lblInfosWith = (TextView) findViewById(R.id.lbl_infos_with);
        lblInfosWithout = (TextView) findViewById(R.id.lbl_infos_without);

        final PackageManager pm = getPackageManager();
        try {
            uidBeGore = pm.getPackageUid(BeBound.getBeBoundServicesPackageName(), 0);
            uidIndus = pm.getPackageUid(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        findViewById(R.id.bt_with).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TEST", "onCLick(bt_with)");
                new Handler().post(new BeBoundServices(lblInfosWith, MainActivity.this));
            }
        });

        findViewById(R.id.bt_without).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TEST", "onCLick(bt_without)");
                new Handler().post(new IndusNativeCode(lblInfosWithout, MainActivity.this));
            }
        });
    }


    public static class IndusNativeCode implements Runnable {

        private long startTime = 0L;
        private final OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        private String serverUrl = "https://wowelb-1747564302.indusos.com/content/news?language=en&category=top&pageLimit=500";

        private long rxBytesBefore;
        private long txBytesBefore;

        private TextView lblInfos;
        private Activity activity;

        IndusNativeCode(TextView lblInfosWithout, Activity activity) {
            this.lblInfos = lblInfosWithout;
            this.activity = activity;
        }

        @Override
        public void run() {
            rxBytesBefore = TrafficStats.getUidRxBytes(uidIndus);
            txBytesBefore = TrafficStats.getUidTxBytes(uidIndus);
            lblInfos.setText("Sending...");
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(serverUrl)
                    .header("Accept-Encoding", "gzip")
                    .build();

            startTime = System.currentTimeMillis();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lblInfos.setText("Failure");
                        }
                    });
                    Log.e(TAG, "Indus native call failed with exception ", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, String.format("invalid response received, %s", response.body().toString()));
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
                    final long duration = System.currentTimeMillis() - startTime;

                    Log.d(TAG, String.format("Indus Native call Response time : %d", System.currentTimeMillis() - startTime));

                    long newRxBytes = TrafficStats.getUidRxBytes(uidIndus);
                    long newtxBytes = TrafficStats.getUidTxBytes(uidIndus);
                    final long deltaRx = newRxBytes - rxBytesBefore;
                    final long deltaTx = newtxBytes - txBytesBefore;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String builder = "Infos Indus request:\n" +
                                    "Duration : " + duration + " ms\n" +
                                    "Request size : " + convertBytesToHumanReadableString(deltaTx) + "\n" +
                                    "Response size : " + convertBytesToHumanReadableString(deltaRx) + "\n";
                            lblInfos.setText(builder);
                            Log.d(TAG, String.format("Indus Native call Received bytes : %d bytes.", deltaRx));
                            Log.d(TAG, String.format("Indus Native call Sent bytes : %d bytes.", deltaTx));
                        }
                    });

                    response.body().close();
                }
            });
        }
    }


    public static class BeBoundServices implements Runnable {

        private long startTime = 0L;
        private long rxBytesBefore;
        private long txBytesBefore;

        private TextView lblInfos;
        private Activity activity;

        BeBoundServices(TextView lblInfosWith, Activity activity) {
            this.lblInfos = lblInfosWith;
            this.activity = activity;
        }

        @Override
        public void run() {
            lblInfos.setText("Sending...");
            rxBytesBefore = TrafficStats.getUidRxBytes(uidBeGore);
            txBytesBefore = TrafficStats.getUidTxBytes(uidBeGore);

            FetchNewsRequest fetchNewsRequest = FetchNewsRequest.builder();
            fetchNewsRequest.pageLimit = 500;
            fetchNewsRequest.category = "top";
            fetchNewsRequest.language = "en";
            try {
                fetchNewsRequest.toRequest().onSent(new OnSentListener() {
                    @Override
                    public boolean onSent(Context context, Request request) {
                        startTime = System.currentTimeMillis();
                        Log.d(TAG, "Be-Bound request sent successfully");
                        return true;
                    }
                }).onFailed(new OnFailedListener() {
                    @Override
                    public boolean onRequestFailed(Context context, Request request, int requestStatusCode, String requestStatusMessage) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblInfos.setText("Failure");
                            }
                        });
                        Log.e(TAG, String.format("Be-Bound request failed. request = {%s},requestStatusCode = (%d},requestStatusMessage={%s}", request.toString(), requestStatusCode, requestStatusMessage));
                        return true;
                    }

                    @Override
                    public boolean onResponseError(Context context, Request request, com.bebound.common.model.request.Response response, int responseStatusCode, String responseStatusMessage) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblInfos.setText("Failure");
                            }
                        });
                        Log.e(TAG, String.format("Be-Bound response error, request = {%s}, response = {%s},responseStatusCode = {%d},responseStatusMessage={%s}", request.toString(), response.toString(), responseStatusCode, responseStatusMessage));
                        return true;
                    }

                    @Override
                    public boolean onTimeout(Context context, Request request) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblInfos.setText("Failure");
                            }
                        });
                        Log.e(TAG, String.format("Be-Bound timeout occurred. request = {%s}", request.toString()));
                        return true;
                    }

                    @Override
                    public boolean onResponseNotValidated(Context context, Request request, com.bebound.common.model.request.Response response, int validationResult) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblInfos.setText("Failure");
                            }
                        });
                        Log.e(TAG, String.format("Be-Bound response not validated. request = {%s}, response = {%s}, validationResult = {%d}", request.toString(), response.toString(), validationResult));
                        return true;
                    }

                    @Override
                    public boolean onFallbackTimeout(Context context, Request request) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblInfos.setText("Failure");
                            }
                        });
                        Log.e(TAG, String.format("Be-Bound fallback timeout occurred. request = {%s}", request.toString()));
                        return true;
                    }
                }).onSuccess(new OnSuccessListener() {
                    @Override
                    public boolean onSuccess(Context context, com.bebound.common.model.request.Response response, Request request) {
                        final long duration = System.currentTimeMillis() - startTime;
                        Log.d(TAG, String.format("Be-Bound Response time : %d", System.currentTimeMillis() - startTime));
                        Log.d(TAG, String.format("Be-Bound response successfully received. request = {%s},response = {%s}", request.toString(), response.toString()));

                        long newRxBytes = TrafficStats.getUidRxBytes(uidIndus);
                        long newtxBytes = TrafficStats.getUidTxBytes(uidIndus);
                        final long deltaRx = newRxBytes - rxBytesBefore;
                        final long deltaTx = newtxBytes - txBytesBefore;
                        Log.d(TAG, String.format("Be-Bound response Received bytes : %d bytes.", deltaRx));
                        Log.d(TAG, String.format("Be-Bound response Sent bytes : %d bytes.", deltaTx));

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String builder = "Infos Be-Bound request:\n" +
                                        "Duration : " + duration + "\n" +
                                        "Request size : " + convertBytesToHumanReadableString(deltaTx) + "\n" +
                                        "Response size : " + convertBytesToHumanReadableString(deltaRx) + "\n";
                                lblInfos.setText(builder);
                                Log.d(TAG, String.format("Indus Native call Received bytes : %d bytes.", deltaRx));
                                Log.d(TAG, String.format("Indus Native call Sent bytes : %d bytes.", deltaTx));
                            }
                        });

                        return super.onSuccess(context, response, request);
                    }
                }).forceType(Request.Type.HTTP).send();

            } catch (BeBoundException e) {

                Log.e(TAG, "Be-Bound request failed with exception ", e);
            }

        }
    }

    private static String convertBytesToHumanReadableString(long bytesize) {
        long bits = bytesize * 8;
        int unit = 1000; // And not 1024
        if (bits < unit)
            return bits + " b";

        int exp = (int) (Math.log(bits) / Math.log(unit));
        return String.format(Locale.US, "%.1f %sb", bits / Math.pow(unit, exp), String.valueOf("kMGTPE".charAt(exp - 1)));
    }
}