package com.soton.opendata.roadaccidentopendata.utils;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ParseJSON {


    public static ArrayList<LatLng> parseLatlng(String jsonData) {
        ArrayList<LatLng> list=new ArrayList<LatLng>();
        try {
            JSONArray data=null;
            JSONObject jsonObj = new JSONObject(jsonData);
            // Getting JSON Array node
            data = jsonObj.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < data.length(); i++) {
                JSONObject jsonObject = data.getJSONObject(i);
                double lat = jsonObject.getDouble("Latitude");
                double lng = jsonObject.getDouble("Longitude");
                list.add(new LatLng(lat,lng));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    public static String parseWeather(String jsonData){
        String weathercode = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray array = jsonObject.getJSONArray("weather");
            jsonObject = new JSONObject(array.getString(0));
            String weather= jsonObject.getString("main");
            if (weather.equals("Thunderstorm")  || weather .equals("Drizzle")  || weather .equals("Rain") ) {
                weathercode = "(2+%7C%7C+5)";
            } else if (weather .equals("Snow") ) {
                weathercode = "(3+%7C%7C+6)";
            } else if (weather .equals("Atmosphere") ) {
                weathercode = "7";
            } else {
                weathercode = "(1+%7C%7C+4)";
            }
        } catch (Exception e) {

        }
        return weathercode;
    }
}
