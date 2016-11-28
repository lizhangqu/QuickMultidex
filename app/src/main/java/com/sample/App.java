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


        //VAP初始化
//        Configuration.Builder builder = Configuration.newBuilder()
//                .debug(false) // 是否输出日志拦截器的日志，非线上包都可以
//                .sign(true)//是否加密
//                .checksum(true)//加密的checksum
//                .gzip(true)//gzip压缩,大于150字节才会压缩
//                .signatureCode(1752055618)
//                .https(true)
//                .enableHttp2(true)
//                .publicContext();
//        VapCore.getInstance().init(context, builder.build());


    }


}
