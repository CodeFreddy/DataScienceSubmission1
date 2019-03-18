package main.java.EntityLinking;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class SpotLight {


    private static String spotLightUrl = "http://model.dbpedia-spotlight.org/en/annotate?";

    public static String getRelatedJson(String text) throws Exception{

        String buildUrl = spotLightUrl+"text="+text.replace(" ","%20")+"&confidence=0.5";
        String response = get(buildUrl);

        response = response.replace("\"@","\"");
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

        return res.toString();
    }
}
