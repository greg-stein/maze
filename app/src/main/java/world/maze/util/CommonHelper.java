package world.maze.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Greg Stein on 11/23/2016.
 */
public class CommonHelper {

    public static final <T> List<T> extractObjects(Class<T> klazz, Iterable<Object> collection) {
        List<T> result = new ArrayList<>();
        Iterator<Object> iterator = collection.iterator();

        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (klazz.isAssignableFrom(o.getClass())) {
                result.add(klazz.cast(o));
                iterator.remove();
            }
        }

        return result;
    }

    public static final <T> List<T> filterObjects(Class<T> klazz, Iterable<Object> collection) {
        List<T> result = new ArrayList<>();

        for (Object o : collection) {
            if (klazz.isAssignableFrom(o.getClass())) {
                result.add(klazz.cast(o));
            }
        }

        return result;
    }
}
