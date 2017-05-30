package com.indusos.plusi;

import android.app.Application;

import com.bebound.sdk.application.BeBoundApplicationDelegate;

/**
 * Created by sunil on 5/29/2017.
 */

public class BeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BeBoundApplicationDelegate.onCreate(this, new wow.AppConfig());
    }
}
