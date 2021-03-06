package com.kingweather.app.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.kingweather.app.db.KingWeatherDB;
import com.kingweather.app.model.City;
import com.kingweather.app.model.County;
import com.kingweather.app.model.Province;

/**
 * 工具类
 * 由于使用现成的SQLite数据库，原有的读取TXT到数据库的语句不再有效
 */
public class Utility {
    
    
    /**
     * 天气现象编码表
     */
    private static Map<Integer, String> weatherCodes = new HashMap<Integer, String>();
    static {
        String[] weatherCodeArray = {"晴","多云","阴","阵雨","雷阵雨","雷阵雨伴有冰雹"
                ,"雨夹雪","小雨","中雨","大雨","暴雨","大暴雨","特大暴雨","阵雪"
                ,"小雪","中雪","大雪","暴雪","雾","冻雨","沙尘暴","小到中雨","中到大雨"
                ,"大到暴雨","暴雨到大暴雨","大暴雨到特大暴雨","小到中雪","中到大雪"
                ,"大到暴雪","浮尘","扬沙","强沙尘暴"};
        for (int i = 0; i < weatherCodeArray.length; i++) {
            weatherCodes.put(i, weatherCodeArray[i]);
        }
        weatherCodes.put(53, "霾");
        weatherCodes.put(99, "未知");
    }
    
    /**
     * 用来查重复
     */
    private static Map<String, Integer> provinceCodes = new HashMap<String, Integer>();
    private static Map<String, Integer> cityCodes = new HashMap<String, Integer>();
    private static Map<String, Integer> countyCodes = new HashMap<String, Integer>();
    
    /**
     * 直辖市
     */
    private static String[] municipalities = {"北京", "上海", "重庆", "天津"};
    
    /**
     * 存取数据库相关的字符串
     */
    private static final String PACKAGE_NAME = "com.kingweather.app";
    private static final String DB_PATH = "/data" + Environment.getDataDirectory().getAbsolutePath()
            + "/" + PACKAGE_NAME + "/databases/" + KingWeatherDB.DB_NAME;
    
    
   
    /**
     * 从area_id.txt文件中读取数据并存入数据库
     */
    public static void fileToDatabase(KingWeatherDB kingWeatherDB){
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = MyApplication.getContext().getResources()
                    .openRawResource(com.kingweather.app.R.raw.king_weather);
            out = new FileOutputStream(DB_PATH);
            byte[] buffer = new byte[1024];
            int hasRead = 0;
            while ((hasRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, hasRead);
            }
//            // 如果要使用txt读取数据，则使用下列代码：
//            input = new BufferedReader(new InputStreamReader(in, "utf-8"));
//            String line = null;
//            while ((line = input.readLine()) != null) {
//                handleLine(line, kingWeatherDB);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
         // 如果要使用txt读取数据，则使用下列代码：
//            if (input != null){
//                try {
//                    input.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }
    
    /**
     * 解析和处理area_id.txt的每一行数据
     */
    private static void handleLine(String line, KingWeatherDB kingWeatherDB){
        if (!TextUtils.isEmpty(line)) {
            LogUtil.v("读取文本", "读取出的文本文件每一行内容为:" + line);
            String[] data = line.split("\\|");
            if (data.length < 9 ) return;
            LogUtil.v("读取文本", "data数组长度为：" + data.length + "\nDATA数组为：" + Arrays.toString(data));
            if (data != null && data.length > 0) {
                int provinceId = handleProvince(data, kingWeatherDB);
                int cityId = handleCity(data, provinceId, kingWeatherDB);
                handleCounty(data, cityId, kingWeatherDB);
            }
        }
    }
    
    /**
     * 从分割后的数组中获取省级数据
     */
    private static int handleProvince(String[] data, KingWeatherDB kingWeatherDB){
        String provinceName = data[6];
        String provinceCode = data[0].substring(3, 5);
        // 数据库中的id
        int id = -1;
        if (!provinceCodes.containsKey(provinceCode)) {
            // Map中的value与数据库中的id对应
            id = provinceCodes.size() + 1;
            LogUtil.v("提取省", "省：" + provinceName+ "|省Code："  + provinceCode + " ID" + id + "数组第一个元素" + data[0] + " 长度：" + data[0].length()  );
            provinceCodes.put(provinceCode, id);
            Province province = new Province();
            province.setProvinceCode(provinceCode);
            province.setProvinceName(provinceName);
            kingWeatherDB.saveProvince(province);
        } else {
            id = provinceCodes.get(provinceCode);
        }
        return id;
    }
    
    /**
     * 从分割后的数组中解析市级数据
     */
    private static int handleCity(String[] data, int provinceId, 
            KingWeatherDB kingWeatherDB) {
        String cityName = data[4];
        // 如果是直辖市则取二位市区代码
        String cityCode = null;
        if (isMunicipality(cityName)) {
            cityCode = data[0].substring(3, 5); 
        } else {
            cityCode = data[0].substring(3, 7);
        }
        // 数据库中的id
        int id = -1;
        if (!cityCodes.containsKey(cityCode)) {
            id = cityCodes.size() + 1;
            cityCodes.put(cityCode, id);
            City city = new City();
            city.setCityCode(cityCode);
            city.setCityName(cityName);
            city.setProvinceId(provinceId);
            kingWeatherDB.saveCity(city);
        } else {
            id = cityCodes.get(cityCode);
        }
        return id;
    }
    
    /**
     * 从分割数组中解析县级数据
     */
    private static void handleCounty(String[] data, int cityId,
            KingWeatherDB kingWeatherDB) {
        String countyName = data[2];
        String countyCode = data[0]; // 用于查天气
        if (!countyCodes.containsKey(countyCode)) {
            int id = countyCodes.size() + 1;
            countyCodes.put(countyCode, id);
            County county = new County();
            county.setCountyName(countyName);
            county.setCountyCode(countyCode);
            county.setCityId(cityId);
            kingWeatherDB.saveCounty(county);
        }
    }
    
    /**
     * 判断是否直辖市
     */
    private static boolean isMunicipality(String name) {
        for (int i = 0; i < municipalities.length; i++) {
            if (municipalities[i].equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 解析服务器返回的JSON数据，并将解析出的数据存储到本地
     */
    public static void handleWeatherResponse(Context context, String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            // 城市信息
            JSONObject city = jsonObject.getJSONObject("c");
            String cityName = city.getString("c3");
            String weatherCode = city.getString("c1");
            // 天气信息 包含未来三天
            JSONObject weatherInfo = jsonObject.getJSONObject("f");
            JSONArray weatherInfos = weatherInfo.getJSONArray("f1");
            // 第一天
            JSONObject firstDayWeather = weatherInfos.getJSONObject(0);
            String temp1 = firstDayWeather.getString("fd") + "℃";
            String temp2 = firstDayWeather.getString("fc") + "℃";
            // 白天和晚上两个天气编码，晚上更新天气时，白天数据会被清空
            String weatherDayCode = firstDayWeather.getString("fa");
            String weatherNightCode = firstDayWeather.getString("fb");
            String weatherDesp = "";
            if (!"".equals(weatherDayCode)) {
                weatherDesp = weatherCodes.get(Integer.valueOf(weatherDayCode)) 
                        + "转" + weatherCodes.get(Integer.valueOf(weatherNightCode));
            } else {
                weatherDesp = weatherCodes.get(Integer.valueOf(weatherNightCode));
            }
            String publishTimeAll = weatherInfo.getString("f0"); //201503181100形式
            String publishTime = publishTimeAll.substring(8, 10) + ":" + publishTimeAll.substring(10);
            saveWeatherInfo(context, cityName, weatherCode, temp1, temp2, weatherDesp, publishTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 将服务器返回的所有天气信息存储到SharedPreferences文件中
     */
    public static void saveWeatherInfo(Context context, String cityName, String weatherCode
            , String temp1, String temp2, String weatherDesp, String publishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        if (temp1.length() > 1) {
            editor.putString("temp1", temp1);
        }
        if (temp2.length() > 1) {
            editor.putString("temp2", temp2);
        }
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.commit();
    }
}
