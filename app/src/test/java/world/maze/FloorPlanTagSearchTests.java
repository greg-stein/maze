package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Floor;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.Tag;
import com.opencsv.CSVReader;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 4/30/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class FloorPlanTagSearchTests {
    private static CSVReader csvReader;
    private Building mBuilding;

    @Before
    public void setup() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in_s = classLoader.getResourceAsStream("inexact_matching_test_source.csv");
            csvReader = new CSVReader(new InputStreamReader(in_s), ';');

            List<Tag> tags = new ArrayList<>();
            String [] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                tags.add(new Tag(new PointF(), nextLine[1]));
            }

            mBuilding = new Building();
            Floor floor = new Floor();
            floor.setTags(tags);
            List<Floor> floors = new ArrayList<>();
            floors.add(floor);
            mBuilding.setFloors(floors);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void CommonTagSearchTest() {
        List<Tag> similarTags = mBuilding.searchMostSimilarTags("smasnung", 10);

        assertThat(similarTags, hasSize(10));
        for (Tag t : similarTags) {
            System.out.println(t.getLabel());
        }
    }
}
