package com.example.neutrino.maze;

import com.example.neutrino.maze.util.CommonHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 5/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class CommonHelperTests {
    @Test
    public void extractObjectsTest() {
        Collection<Object> objects = new ArrayList<>();
        objects.add("string 1");
        objects.add("string 2");
        objects.add("string 3");
        objects.add("string 4");
        objects.add("string 5");
        objects.add(1);
        objects.add(2);
        objects.add(3);
        objects.add(4);
        objects.add(5);
        objects.add(true);
        objects.add(false);
        objects.add(Math.E);
        objects.add(Math.PI);
        objects.add(1.2f);

        int objectsNum = objects.size();
        List<String> strings = CommonHelper.extractObjects(String.class, objects);
        assertNotNull(strings);
        assertThat(strings, hasSize(5));
        assertThat(objects, hasSize(objectsNum - strings.size()));

        objectsNum = objects.size();
        List<Integer> ints = CommonHelper.extractObjects(Integer.class, objects);
        assertNotNull(ints);
        assertThat(ints, hasSize(5));
        assertThat(objects, hasSize(objectsNum - ints.size()));

        objectsNum = objects.size();
        List<Double> doubles = CommonHelper.extractObjects(Double.class, objects);
        assertNotNull(doubles);
        assertThat(doubles, hasSize(2));
        assertThat(objects, hasSize(objectsNum - doubles.size()));
    }

    @Test
    public void filterObjectsTest() {
        Collection<Object> objects = new ArrayList<>();
        objects.add("string 1");
        objects.add("string 2");
        objects.add("string 3");
        objects.add("string 4");
        objects.add("string 5");
        objects.add(1);
        objects.add(2);
        objects.add(3);
        objects.add(4);
        objects.add(5);
        objects.add(true);
        objects.add(false);
        objects.add(Math.E);
        objects.add(Math.PI);
        objects.add(1.2f);

        int objectsNum = objects.size();
        List<String> strings = CommonHelper.filterObjects(String.class, objects);
        assertNotNull(strings);
        assertThat(strings, hasSize(5));
        assertThat(objects, hasSize(objectsNum));

        objectsNum = objects.size();
        List<Integer> ints = CommonHelper.filterObjects(Integer.class, objects);
        assertNotNull(ints);
        assertThat(ints, hasSize(5));
        assertThat(objects, hasSize(objectsNum));

        objectsNum = objects.size();
        List<Double> doubles = CommonHelper.filterObjects(Double.class, objects);
        assertNotNull(doubles);
        assertThat(doubles, hasSize(2));
        assertThat(objects, hasSize(objectsNum));
    }
}
