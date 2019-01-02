package world.maze.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Greg Stein on 11/23/2016.
 */
public class CommonHelper {

    public static Activity extractActivity(Context context) {
        if (context == null)
            return null;
        else if (context instanceof Activity)
            return (Activity)context;
        else if (context instanceof ContextWrapper)
            return extractActivity(((ContextWrapper)context).getBaseContext());

        return null;
    }

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

    public static <T> int intersectionSize(Set<T> set1, Set<T> set2) {
        Set<T> smallerSet;
        Set<T> largerSet;

        if (set1.size() <= set2.size()) {
            smallerSet = set1;
            largerSet = set2;
        } else {
            smallerSet = set2;
            largerSet = set1;
        }

        int count = 0;
        for (T element : smallerSet) {
            if (largerSet.contains(element)) {
                count++;
            }
        }
        return count;
    }
}
