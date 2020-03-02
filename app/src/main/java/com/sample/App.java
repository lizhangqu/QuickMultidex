package com.sample;

import android.app.Application;
import android.content.Context;

import com.android.quickmultidex.Multidex;


/**
 * @version V1.0
 * @author: lizhangqu
 * @date: 2016-11-25 20:59
 */
public class App extends Application {

    @Override
    protected final void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        boolean install = Multidex.install(context);
    }


}
