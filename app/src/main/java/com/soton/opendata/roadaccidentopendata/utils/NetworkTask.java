package com.soton.opendata.roadaccidentopendata.utils;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

public abstract class NetworkTask extends AsyncTask<String,Integer,String> {
    public static final String TAG = "NetworkTask";

    public static String GET = "GET";
    public static String POST = "POST";
    // 访问方式，只能是GET或POST
    private String mRequestMethod;
    // 是否访问网络成功
    protected boolean isSuccess = true;
    // 监听回调
    private ResponceLintener mResponceLintener;

    public NetworkTask(String method) {
        this.mRequestMethod = method;
    }

    @Override
    protected String doInBackground(String... params) {
        String data;
        String url = params[0];
        if(GET.equals(mRequestMethod)) {
            data = doGet(url);
        } else if(POST.equals(mRequestMethod)) {
            Map<String, String> paramMap = new HashMap<String, String>();
            // 第一个参数为访问的接口,不为body参数
            for (int i = 1; i < params.length; i++) {
                String[] value = new String[2];
                if (params[i].split("=").length == 1) {
                    value[0] = params[i].split("=")[0];
                    value[1] = "";
                } else {
                    value = params[i].split("=");
                }
                paramMap.put(value[0], value[1]);
            }
            data = doPost(url, paramMap);
        } else {
            throw new RuntimeException("Request mode can only be GET or POST!");
        }
        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        if(null != mResponceLintener) {
            if(isSuccess) {
                mResponceLintener.onSuccess(result);
            } else {
                mResponceLintener.onError(result);
            }
        }
    }

    /**
     * 以GET的方式访问网络
     *
     * @param url
     * @return 返回的字符串数据
     */
    public abstract String doGet(String url);

    /**
     * 以POST的方式访问网络
     *
     * @param url
     * @param paramMap
     * @return 返回的字符串数据
     */
    public abstract String doPost(String url, Map<String, String> paramMap);

    public void setResponceLintener(ResponceLintener l) {
        this.mResponceLintener = l;
    }

    public interface ResponceLintener {
        /**
         * 成功的监听回调
         * @param result
         */
        void onSuccess(String result);
        /**
         * 失败的监听回调
         * @param error
         */
        void onError(String error);
    }

}
