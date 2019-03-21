package main.java.EntityLinking;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;


public class SpotLight {

    private static String spotLightUrl = "http://api.dbpedia-spotlight.org/en/annotate?";

    public static String getRelatedJson(String text) throws Exception{



        //text = text.replaceAll("[^a-zA-Z0-9%]"," ");
        String encoderString = URLEncoder.encode(text, "utf-8");

        encoderString = encoderString.replace("+","%20");
        //System.out.println(encoderString);
        //String buildUrl = spotLightUrl+"text="+text.replace(" ","%20")+"&confidence=0.5";
        String buildUrl = spotLightUrl+"text="+encoderString+"&confidence=0.5";

        String response = get(buildUrl);
//        System.out.println(buildUrl);
        response = response.replace("\"@","\"");

        //System.out.println(response);
        return response;
    }

    public static String get(String url) throws Exception{
        StringBuffer res = new StringBuffer();

        HttpClient client = HttpClients.createDefault();

        HttpGet request = new HttpGet(url);

        request.addHeader("Accept","application/json");

        HttpResponse response = client.execute(request);


        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String line = "";

        while ((line = bufferedReader.readLine()) != null){
            res.append(line);
        }

        ((CloseableHttpClient) client).close();
        return res.toString();
    }
}
