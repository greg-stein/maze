package com.example.neutrino.maze.floorplan;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 9/26/2016.
 */
public class FloorPlanSerializer {

    private static final Type TYPE = (new TypeToken<List<FloorPlanPrimitiveBase>>() {}).getType();

    public static String serializeFloorPlan(List<IFloorPlanPrimitive> floorPlan) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(TYPE, new FloorPlanAdapter());
        Gson gson = builder.create();

        String jsonString = gson.toJson(floorPlan, TYPE);

        return jsonString;
    }

    @Nullable
    public static List<IFloorPlanPrimitive> deserializeFloorPlan(String jsonString) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(TYPE, new FloorPlanAdapter());
        Gson gson = builder.create();

        return gson.fromJson(jsonString, TYPE);
    }

    /**
     * Created by Greg Stein on 9/25/2016.
     */
    public static class FloorPlanAdapter implements JsonSerializer<List<FloorPlanPrimitiveBase>>,
            JsonDeserializer<List<FloorPlanPrimitiveBase>> {

        private static final String CLASSNAME = "CLASSNAME";
        private static final String INSTANCE = "INSTANCE";

        @Override
        public JsonElement serialize(List<FloorPlanPrimitiveBase> src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonArray array = new JsonArray();

            for (FloorPlanPrimitiveBase primitiveBase : src) {
                if (primitiveBase.isRemoved()) continue; // skip removed primitives

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
        public List<FloorPlanPrimitiveBase> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final String packageName = IFloorPlanPrimitive.class.getPackage().getName();
            List<FloorPlanPrimitiveBase> result = new ArrayList<>();
            JsonArray jsonArray = json.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                final JsonObject asJsonObject = element.getAsJsonObject();
                String className = asJsonObject.get(CLASSNAME).getAsString();
                JsonElement serializedInstance = asJsonObject.get(INSTANCE);

                Class<?> klass = null;
                try {
                    klass = Class.forName(packageName + '.' + className);
                    final FloorPlanPrimitiveBase deserializedInstance = context.deserialize(serializedInstance, klass);
                    if (!deserializedInstance.isRemoved() &&
                            (deserializedInstance instanceof Wall)) { // || deserializedInstance instanceof WifiMark)) { // skip removed primitives
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
