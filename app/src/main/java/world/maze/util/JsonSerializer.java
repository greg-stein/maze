package world.maze.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import world.maze.floorplan.FloorPlanPrimitiveBase;
import world.maze.floorplan.IFloorPlanPrimitive;

/**
 * Created by Greg Stein on 9/26/2016.
 */
public class JsonSerializer {
    public static final Type TYPE = (new TypeToken<List<IFloorPlanPrimitive>>() {}).getType();
    private static Gson gson = null;

    public static void initGson() {
        if (gson == null) {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(TYPE, new FloorPlanAdapter());
            gson = gsonBuilder.create();
        }
    }

    public static String serialize(Object entity) {
        initGson();
        Writer writer = new StringWriter();
        gson.toJson(entity, writer);

        return writer.toString();
    }

    public static <T> T deserialize(String jsonString, Class<T> klazz) {
        initGson();
        return deserialize(jsonString, (Type)klazz);
    }

    public static <T> T deserialize(String jsonString, Type type) {
        initGson();
        T entity = gson.fromJson(jsonString, type);

        return entity;
    }

    public static class FloorPlanAdapter implements com.google.gson.JsonSerializer<List<IFloorPlanPrimitive>>,
            JsonDeserializer<List<IFloorPlanPrimitive>> {

        private static final String CLASSNAME = "CLASSNAME";
        private static final String INSTANCE = "INSTANCE";

        @Override
        public JsonElement serialize(List<IFloorPlanPrimitive> src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonArray array = new JsonArray();

            for (Object primitiveBase : src) {
//                if (primitiveBase.isRemoved()) continue; // skip removed primitives

                JsonObject primitiveJson = new JsonObject();

                String className = primitiveBase.getClass().getSimpleName();
                primitiveJson.addProperty(CLASSNAME, className);

                JsonElement elem = context.serialize(primitiveBase);
                primitiveJson.add(INSTANCE, elem);

                array.add(primitiveJson);
            }
            return array;
        }

        @Override
        public List<IFloorPlanPrimitive> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final String packageName = IFloorPlanPrimitive.class.getPackage().getName();
            List<IFloorPlanPrimitive> result = Collections.synchronizedList(new ArrayList<IFloorPlanPrimitive>()); // Synchronized!!
            JsonArray jsonArray = json.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                final JsonObject asJsonObject = element.getAsJsonObject();
                String className = asJsonObject.get(CLASSNAME).getAsString();
                JsonElement serializedInstance = asJsonObject.get(INSTANCE);

                Class<?> klass = null;
                try {
                    if (className.equals("WifiMark")) className = "Fingerprint"; // Backward compatibility
                    klass = Class.forName(packageName + '.' + className);
                    final IFloorPlanPrimitive deserializedInstance = context.deserialize(serializedInstance, klass);

                    if (deserializedInstance instanceof FloorPlanPrimitiveBase) {
                        FloorPlanPrimitiveBase primitiveBase = (FloorPlanPrimitiveBase) deserializedInstance;
                        if (!primitiveBase.isRemoved()) {
                            result.add(deserializedInstance);
                        }
                    } else {
                        result.add(deserializedInstance);
                    }
                } catch (ClassNotFoundException e) {
                    throw new JsonParseException("Unknown element type: " + klass, e);
                }
            }
            return result;
        }
    }
}