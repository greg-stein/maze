package world.maze.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Greg Stein on 9/26/2016.
 */
public class JsonSerializer {
    private static Gson gson = null;

    public static String serialize(Object entity) {
        if (gson == null)
            gson = new GsonBuilder().create();
        Writer writer = new StringWriter();
        gson.toJson(entity, writer);

        return writer.toString();
    }

    public static <T> T deserialize(String jsonString, Class<T> klazz) {
        if (gson == null)
            gson = new GsonBuilder().create();
        T entity = gson.fromJson(jsonString, klazz);

        return entity;
    }
}