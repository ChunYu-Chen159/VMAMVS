package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.neo4j.Endpoint;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.SleuthRepository;
import com.sun.tools.javac.resources.version;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class SleuthService {

    private static final Logger logger = LoggerFactory.getLogger(SleuthService.class);

    @Value("${zipkin.basePath}")
    private String Zipkin_V1_BASEPATH;

    // 抓取多少時間以內的資料，單位毫秒，604800000=7天
    private final String lookback = "604800000";
    // 最多抓幾筆資料回來
    private final String limit = "10000";



    @Autowired
    private SleuthRepository sleuthRepository;

    @Autowired
    private GeneralRepository generalRepository;

    public List<String> calculateNumofRequest(){

        List<String> temp = sleuthRepository.getAllServiceAndPathWithHTTP_REQUEST();

        //System.out.println(temp);

        for(int i = 0; i < temp.size(); i++){
            JSONObject jsonObj = new JSONObject(temp.get(i));

            int count = 0;

            String result = "";
            try {
                URL url = new URL(Zipkin_V1_BASEPATH+"api/v1/traces?annotationQuery=http.version%3D"+jsonObj.getString("serviceVersion")+"&limit=" + limit + "&lookback=" + lookback + "&minDuration=&serviceName="+jsonObj.getString("appName").toLowerCase()+"&spanName=http:"+jsonObj.getString("path").toLowerCase()+"&sortOrder=timestamp-desc");
                URLConnection urlConnection = url.openConnection();


                BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()) );
                String current = "";
                while((current = in.readLine()) != null)
                {
                    result += current;
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONArray array = new JSONArray(result);

            for(int j = 0; j < array.length(); j++) {
                //System.out.println("result_" + i + j + "_" + jsonObj.getString("appName") + "_" + jsonObj.getString("path") + ":" + array.getJSONArray(j).getJSONObject(0).getJSONArray("binaryAnnotations").getJSONObject(5).get("key") + ":" + array.getJSONArray(j).getJSONObject(0).getJSONArray("binaryAnnotations").getJSONObject(5).get("value"));

                for(int j2 = 0;j2 < array.getJSONArray(j).length(); j2++) {
                    for (int j3 = 0; j3 < array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").length(); j3++) {
                        if (array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("key").equals("http.appName")) {
                            //System.out.println("http.appName:" + array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("value"));
                            //System.out.println("targetAppName:" + jsonObj.getString("targetAppName").toLowerCase());
                            if(array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("value").equals(jsonObj.getString("targetAppName").toLowerCase()))
                                continue;
                            else
                                break;
                        }
                        if (array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("key").equals("http.version")) {
                            //System.out.println("http.version:" + array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("value"));
                            if(array.getJSONArray(j).getJSONObject(j2).getJSONArray("binaryAnnotations").getJSONObject(j3).get("value").equals(jsonObj.getString("targetServiceVersion")))
                                count++;
                        }
                    }
                }

            }


            // 計算數量塞到要給前端的json資料內
            String newContent = temp.get(i).replaceFirst("}","") + ",\"num\":\"" + count + "\"}";

            temp.set(i,newContent);


        }

        //System.out.println("new：" + temp);

        return temp;
    }


    public String getTraceInfo(String appName)
    {
        String result = "";
        try {
            URL url = new URL(Zipkin_V1_BASEPATH+"api/v1/traces?annotationQuery=&limit=" + limit + "&lookback=" + lookback + "&minDuration=&serviceName="+appName.toLowerCase()+"&sortOrder=timestamp-desc");
            URLConnection urlConnection = url.openConnection();


            BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()) );
            String current = "";
            while((current = in.readLine()) != null)
            {
                result += current;
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public String searchZipkin(String appName, String version, int statusCode, long startTime, long endTime, int limit){
        String result = "";
        try {
            URL url = new URL(Zipkin_V1_BASEPATH+"api/v1/traces?annotationQuery=http.version%3D" + version + "%20and%20" + "http.status_code%3D" + statusCode + "&limit=" + limit + "&startTs=" + startTime + "&endTs=" + endTime + "&serviceName="+appName.toLowerCase()+"&sortOrder=timestamp-desc");
            URLConnection urlConnection = url.openConnection();


            BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()) );
            String current = "";
            while((current = in.readLine()) != null)
            {
                result += current;
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public int getTotalNum(String str)
    {
        JSONArray array = new JSONArray(str);


        return 0;
    }





}
