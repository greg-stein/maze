package world.maze;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import world.maze.data.DataAggregator;
import world.maze.data.LocalStore;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Greg Stein on 8/31/2018.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
@Ignore // Issue with WRITE_EXTERNAL_STORAGE permission for test package.
public class PersistanceTests {
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testPermissions() {
        PackageManager pm = context.getPackageManager();

        int expected = PackageManager.PERMISSION_GRANTED;
        int actual = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context.getPackageName());
        assertEquals(expected, actual);
    }

    @Test
    public void kickOffTestForLocalStorage() throws IOException {
        try {
//            ActivityCompat.requestPermissions(context,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    0xff);

             File testDir = new File(Environment.getExternalStorageDirectory() + "/test/");
            final String externalStorageState = Environment.getExternalStorageState();
            Log.d("STATE", externalStorageState);
            if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
                Log.d("PATH", testDir.getAbsolutePath());
                if (!testDir.exists()) {
                    Log.d("MAKE DIRS", testDir.mkdirs() + "");
                    Log.d("MAKE DIR", testDir.mkdir() + "");
                }
                File aFile = new File(testDir, "somefile");
                FileOutputStream fos = new FileOutputStream(aFile);
                fos.write("data".getBytes());
                fos.close();
            }
        } catch (IOException e){
            String msg = e.getMessage();
            Log.d("ERROR", msg);
        }
    }

    @Test
    public void serializationCommonTest() {
//        LocalStore.init(context);
//        LocalStore.save(context, "kishkush", DataAggregator.FLORPLANS_SUBDIR, "filename.ext");
    }
}
