package com.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;



import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //以下代码在4.4上如果不调用  dexOrJar->pRawDexFile->pDvmDex->dex_object = dex_object; 会奔溃
                try {
                    Class<?> hello = Class.forName("com.sample.Core");
                    Method parse = hello.getDeclaredMethod("parse");
                    parse.setAccessible(true);
                    Log.e("TAG", "method:" + parse);
                    parse.invoke(null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
