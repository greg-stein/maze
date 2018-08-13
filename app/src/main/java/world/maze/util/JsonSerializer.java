package world.maze.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Greg Stein on 9/26/2016.
 */
public class JsonSerializer {
    public static String serialize(Object entity) {
        Writer writer = new StringWriter();
        Gson gson = new GsonBuilder().create();
        gson.toJson(entity, writer);

        return writer.toString();
    }

    public static <T> T deserialize(String jsonString, Class<T> klazz) {
        Gson gson = new GsonBuilder().create();
        T entity = gson.fromJson(jsonString, klazz);

        return entity;
    }
}