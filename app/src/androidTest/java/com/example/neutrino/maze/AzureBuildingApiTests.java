package com.example.neutrino.maze;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.example.neutrino.maze.floorplan.Building;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.Matchers.hasSize; // Hamcrest doesn't work with androidTest!
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 6/8/2018.
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // NOTE: run test ordered by name
public class AzureBuildingApiTests {
    public static final String BASE_URL = "http://maze.westeurope.cloudapp.azure.com:8001/api/";

    private static String buildingId;
    private static Building mCreatedBuilding;

    public interface MazeApiInterface {
        @POST("buildings")
        Call<Building> createBuilding(@Body Building building);

        @GET("buildings/{id}")
        Call<Building> getBuilding(@Path("id") String id);

        @GET("buildings/search")
        Call<List<Building>> searchBuilding(@Query("pattern") String pattern);

        @PUT("buildings/{id}")
        Call<Building> updateBuilding(@Path("id") String id, @Body Building newBuildingData);

        @DELETE("buildings/{id}")
        Call<Building> deleteBuilding(@Path("id") String id);
    }

    @BeforeClass
    static public void setUpTests() {
        buildingId = UUID.randomUUID().toString();

        mCreatedBuilding = new Building("Some rnadom building", "Mall",
                "Entuziastov koshesi 42, Ust-Kamenogorsk, Kazakhstan", buildingId);
    }

    @Test
    public void test01BuildingCreate() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<Building> responseCall = mazeApiInterface.createBuilding(mCreatedBuilding);
        Response<Building> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
    }

    @Test
    public void test02BuildingGet() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<Building> responseCall = mazeApiInterface.getBuilding(buildingId);
        Response<Building> response = responseCall.execute();
        Building building = response.body();

        assertNotNull(building);
        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        // Doesn't work! Don't know why Building.equals is not being called
        // assertTrue(building.equals(mCreatedBuilding));
        // assertThat(building, is(equalTo(mCreatedBuilding)));
        assertTrue(building.getId().equals(mCreatedBuilding.getId()));
        assertTrue(building.getAddress().equals(mCreatedBuilding.getAddress()));
        assertTrue(building.getType().equals(mCreatedBuilding.getType()));
        assertTrue(building.getName().equals(mCreatedBuilding.getName()));
    }

    @Test
    public void test03BuildingUpdate() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        // The mID remains the same
        Building newBuildingData = new Building("New name", "New type", "New Address", buildingId);
        Call<Building> responseCall = mazeApiInterface.updateBuilding(buildingId, newBuildingData);
        Response<Building> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));

        responseCall = mazeApiInterface.getBuilding(buildingId);
        response = responseCall.execute();
        Building building = response.body();

        assertNotNull(building);
        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        // Doesn't work! Don't know why Building.equals is not being called
        // assertTrue(building.equals(mCreatedBuilding));
        // assertThat(building, is(equalTo(mCreatedBuilding)));
        assertTrue(building.getId().equals(newBuildingData.getId()));
        assertTrue(building.getAddress().equals(newBuildingData.getAddress()));
        assertTrue(building.getType().equals(newBuildingData.getType()));
        assertTrue(building.getName().equals(newBuildingData.getName()));
    }

    @Test
    public void test04BuildingGetByName() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<List<Building>> responseCall = mazeApiInterface.searchBuilding(mCreatedBuilding.getName());
        Response<List<Building>> response = responseCall.execute();
        List<Building> buildings = response.body();

        assertNotNull(buildings);
        assertThat(buildings.size(), is(1));
        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
    }

    @Test
    public void test05BuildingDelete() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<Building> responseCall = mazeApiInterface.deleteBuilding(buildingId);
        Response<Building> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));

        responseCall = mazeApiInterface.getBuilding(buildingId);
        response = responseCall.execute();
        Building building = response.body();

        assertNull(building);
        assertFalse(response.isSuccessful());
        assertThat(response.code(), is(equalTo(500))); // Building with this ID should not exist
    }
}
