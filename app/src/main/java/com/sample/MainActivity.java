package com.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        try {
            Class<?> hello = Class.forName("Hello");
            Method main = hello.getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            Log.e("TAG", "method:" + main);
            Object args = new String[]{"a", "b", "c"};
            main.invoke(null, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //以下代码在4.4上会奔溃
//        try {
//            Class<?> hello = Class.forName("com.sample.Core");
//            Method parse = hello.getDeclaredMethod("parse");
//            parse.setAccessible(true);
//            Log.e("TAG", "method:" + parse);
//            parse.invoke(null);
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }


    }


}
