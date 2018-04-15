package com.example.neutrino.maze;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by Greg Stein on 4/13/2018.
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
public class AzureDbConnectionTest {
    public static final String DB_URL = "https://maze.documents.azure.com:443/";
    public static final String PRIMARY_KEY = "6sYVR4O7v0QQxB7dPdAZbYLHaVpSSH4lcPT8ZgA8mKelYx7qsO9BKvcMgb2TiIRAduSTlH2JxkdePO2mB8Qmog==";
    public static final String SECONDARY_KEY = "DKDUK6Kvijop2YeEK52G3AsRoccqcKVq9ZWFaASByTqk19iNMwBKqEAdXR62CmRgsEb5mMJ58YDx2jfbjP57CQ==";

    public class IpResponse {
        public String ip;
    }

    public interface JsonTestService {
        @GET Call<IpResponse> getIp(@Url String empty);
    }

    @Test
    public void retrofitSimpleApiRequestTest() throws IOException {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://ip.jsontest.com/").addConverterFactory(GsonConverterFactory.create()).build();

        JsonTestService service = retrofit.create(JsonTestService.class);
        Call<IpResponse> ipRequest = service.getIp("");
        Response<IpResponse> ip = ipRequest.execute();
        System.out.println(ip.toString());
    }

    @Test
    public void serverConnectionTest() {

    }
}
