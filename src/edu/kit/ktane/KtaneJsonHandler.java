package edu.kit.ktane;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by maxis on 24.05.2017.
 */
public class KtaneJsonHandler {

    private int ipadress;
    private int ktaneport = 8085;
    private String url;

    HttpClient client;
    HttpGet request;

    public KtaneJsonHandler(int ip, String url) {
        ipadress = ip;
        this.url = url;

        client = HttpClientBuilder.create().build();

    }

    public JSONObject fetchBombInfos() {

        HttpResponse response;
        request = new HttpGet(url + "bombInfo");
        String result = "";
        JSONObject json = null;
        try {
            response = client.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                result = convertStreamToString(instream);
                // now you have the string representation of the HTML request

                org.json.simple.parser.JSONParser jparser = new org.json.simple.parser.JSONParser();
                json = (JSONObject) jparser.parse(result);

                instream.close();
                if (response.getStatusLine().getStatusCode() == 200) {
                    // netState.setLogginDone(true);
                }

            }
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            // e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            // e1.printStackTrace();
        } catch (ParseException e) {
            // e.printStackTrace();
        }
        return json;
    }

    private String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    // http://localhost:8085/startMission?missionId=fairgame&seed=1234

    public boolean startMission(String missionId, String seed) {
        String callUrl = url+"startMission?missionId="+missionId+"&seed="+seed;
        request = new HttpGet(callUrl);
        try {
            System.out.println("Starting mission: " + callUrl);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            return (entity != null);
        }
        catch(IOException ioe) {
            System.out.println("Kann Mission nicht starten!");
            return false;
        }
    }
}
