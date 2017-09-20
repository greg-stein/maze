package com.example.neutrino.maze;

import android.graphics.PointF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

/**
 * Created by Dima Ruinskiy on 20/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 23)
public class ElevatorTests {
    @Test
    public void dataAcquisitionTest() {
        List<Float> ZGrav11 = loadZGravFromAccelerometerDataFile("D:\\Programming\\maze\\app\\src\\test\\resources\\liftg-slow-up.txt");
        System.out.println(ZGrav11.size());
        LiftDetector detector = LiftDetector.getInstance();
        detector.setVerboseSystemOutput(true);

        for (float grav: ZGrav11) {
            detector.onGravityChanged(grav);
        }
    }

    private List<Float> loadZGravFromAccelerometerDataFile(String resFileName) {
        List<Float> ZGrav = new ArrayList<>();
        try {
            FileReader resFile = new FileReader(resFileName);
            BufferedReader reader = new BufferedReader(resFile);
            String line;
            Scanner scanner;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') continue;
                scanner = new Scanner(line);
                scanner.nextFloat();
                scanner.nextFloat();
                ZGrav.add(scanner.nextFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ZGrav;
    }
}
