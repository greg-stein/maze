package com.example.neutrino.maze;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 7/4/2018.
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // NOTE: run test ordered by name
public class AzureFloorplanApiTests {
    public static final String BASE_URL = "http://maze.westeurope.cloudapp.azure.com:8001/api/";

    private static String floorId;
    private static FloorPlan mCreatedFloorPlan;

    public interface MazeApiInterface {
        @POST("floorplans")
        Call<FloorPlan> createFloorPlan(@Body FloorPlan floorPlan);

        @GET("floorplans/{id}")
        Call<FloorPlan> getFloorPlan(@Path("id") String id);

        @PUT("floorplans/{id}")
        Call<FloorPlan> updateFloorPlan(@Path("id") String id, @Body FloorPlan newFloorPlanData);

        @DELETE("floorplans/{id}")
        Call<FloorPlan> deleteFloorPlan(@Path("id") String id);
    }

    @BeforeClass
    static public void setUpTests() {
        floorId = UUID.randomUUID().toString();
        System.out.println("floorId: " + floorId);

        mCreatedFloorPlan = new FloorPlan(floorId);
        List<IFloorPlanPrimitive> sketch = new ArrayList<>();
        sketch.add(new Wall(0,0,1,0));
        sketch.add(new Wall(1,0,1,1));
        sketch.add(new Wall(1,1,0,1));
        sketch.add(new Wall(0,1,0,0));
        mCreatedFloorPlan.setSketch(sketch);
    }

    @Test
    public void test01CreateFloorPlan() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<FloorPlan> responseCall = mazeApiInterface.createFloorPlan(mCreatedFloorPlan);
        Response<FloorPlan> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
    }

    @Test
    public void test02FloorPlanGet() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<FloorPlan> responseCall = mazeApiInterface.getFloorPlan(floorId);
        Response<FloorPlan> response = responseCall.execute();
        FloorPlan floorPlan = response.body();

        assertNotNull(floorPlan);
        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        assertThat(floorPlan.getSketch().size(), is(equalTo(4)));
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
        FloorPlan newFloorPlanData = new FloorPlan();
        Call<FloorPlan> responseCall = mazeApiInterface.updateFloorPlan(floorId, newFloorPlanData);
        Response<FloorPlan> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));

        responseCall = mazeApiInterface.getFloorPlan(floorId);
        response = responseCall.execute();
        FloorPlan floorPlan = response.body();

        assertNotNull(floorPlan);
        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));
        assertThat(floorPlan.getSketch().size(), is(equalTo(0)));
    }

    @Test
    public void test04BuildingDelete() throws IOException {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MazeApiInterface mazeApiInterface = retrofit.create(MazeApiInterface.class);

        Call<FloorPlan> responseCall = mazeApiInterface.deleteFloorPlan(floorId);
        Response<FloorPlan> response = responseCall.execute();

        assertTrue(response.isSuccessful());
        assertThat(response.code(), is(equalTo(200)));
        assertThat(response.message(), is(equalTo("OK")));

        responseCall = mazeApiInterface.getFloorPlan(floorId);
        response = responseCall.execute();
        FloorPlan floorPlan = response.body();

        assertNull(floorPlan);
        assertFalse(response.isSuccessful());
        assertThat(response.code(), is(equalTo(500))); // Building with this ID should not exist
    }

}
