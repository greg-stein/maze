package world.maze;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 4/13/2018.
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
public class AzureDbConnectionTest {
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

    /**
     * Simple test for connection to API deployed on Azure
     */
    public class SimpleApiResponse {
        public int code;
        public String message;
    }

    public static final String BASE_URL = "http://maze.westeurope.cloudapp.azure.com:8001/";

    public interface ConnectionApiEndpointInterface {
        @GET
        Call<SimpleApiResponse> getApiOk(@Url String url);
    }

    @Test
    public void retrofitAzureSimpleConnectionTest() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        ConnectionApiEndpointInterface azureService = retrofit.create(ConnectionApiEndpointInterface.class);
        Call<SimpleApiResponse> responseCall = azureService.getApiOk("");
        Response<SimpleApiResponse> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        assertThat(response.body().code, is(equalTo(200)));
        assertThat(response.body().message, is(equalTo("Ok")));

        // ------------------------------------------------------------------------------------------
        responseCall = azureService.getApiOk("api/");
        response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        assertThat(response.body().code, is(equalTo(200)));
        assertThat(response.body().message, is(equalTo("API")));
    }
}
