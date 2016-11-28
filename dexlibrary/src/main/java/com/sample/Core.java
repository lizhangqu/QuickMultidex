package com.sample;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.io.Serializable;
import java.util.Map;

/**
 * @version V1.0
 * @author: lizhangqu
 * @date: 2016-11-27 21:08
 */
public class Core {

    public static class Status implements Serializable {

        public int code;
        public String message;
        public String description;

    }

    public static class Result implements Serializable {
        public Status status;
        public Object result;
    }

    public static String json = "{\"status\":{\"code\":0,\"message\":\"OK\",\"description\":\"\"},\"result\":{\"key\":\"value\"}";

    public static void parse() {
        Result result = JSON.parseObject(json.getBytes(), Result.class);
        Log.e("TAG", "result:" + result);
    }
}
