package com.soton.opendata.roadaccidentopendata.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpURLConnectionNetworkTask extends NetworkTask {

    public HttpURLConnectionNetworkTask(String method) {
        super(method);
    }

    @Override
    public String doGet(String httpUrl) {
        String result;
        try {
            URL url = new URL(httpUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(5000);
            urlConnection.setConnectTimeout(5000);
            urlConnection.setRequestProperty("Charset", "UTF-8");
            if (urlConnection.getResponseCode() == 200) {
                InputStream is = urlConnection.getInputStream();
                result = readFromStream(is);
            } else {
                isSuccess = false;
                result = "网络响应状态码不为200!";
            }
        } catch(IOException e) {
            isSuccess = false;
            result = "网络访问错误:" + e.getMessage();
        }
        return result;
    }

    @Override
    public String doPost(String url, Map<String, String> paramMap) {
        return null;
    }

    public String readFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        is.close();
        String result = baos.toString();
        baos.close();
        return result;
    }
}
