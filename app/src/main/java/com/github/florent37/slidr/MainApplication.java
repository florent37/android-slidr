package com.github.florent37.slidr;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by florentchampigny on 30/05/2017.
 */

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
