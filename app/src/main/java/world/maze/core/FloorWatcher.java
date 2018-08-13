package world.maze.core;

import android.content.Context;
import android.graphics.PointF;

import world.maze.floorplan.Building;
import world.maze.floorplan.transitions.ITeleport;
import world.maze.floorplan.transitions.Teleport;
import world.maze.rendering.VectorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Greg Stein on 7/31/2017.
 */
public class FloorWatcher implements Locator.ILocationUpdatedListener, WifiScanner.IFingerprintAvailableListener {
    private static final float TELEPORT_RANGE_EXIT = 30; // in meters
    private static final float TELEPORT_RANGE_EXIT_SQ = TELEPORT_RANGE_EXIT * TELEPORT_RANGE_EXIT;
    private static final float TELEPORT_RANGE_ENTER = 10; // in meters
    private static final float TELEPORT_RANGE_ENTER_SQ = TELEPORT_RANGE_ENTER * TELEPORT_RANGE_ENTER;

    private final Set<ITeleport> mProximityTeleports = new HashSet<>();
    private Map<ITeleport, List<ITeleport>> mDestinationTeleports = new HashMap<>();
    private boolean mEnabled = false;
    private WiFiLocator.WiFiFingerprint mLastFingerprint;
    private List<IFloorChangedHandler> mOnFloorChangedHandlers = new ArrayList<>();
    private Locator mLocator;
    private Building mBuilding;

    private static volatile FloorWatcher instance = null;
    private static final Object mutex = new Object();
    public static FloorWatcher getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new FloorWatcher(context);
                }
            }
        }
        return instance;
    }
    private boolean inRangeOfATeleport() {
        return mProximityTeleports.size() > 0;
    }

    private FloorWatcher(Context context) {
        mLocator = Locator.getInstance(context);
    }

    public void enable(Context context) {
        if (!mEnabled) {
            WifiScanner.getInstance(context).addFingerprintAvailableListener(this);
            Locator.getInstance(context).addLocationUpdatedListener(this);
            mEnabled = true;
        }
    }

    public void disable(Context context) {
        if (mEnabled) {
            WifiScanner.getInstance(context).removeFingerprintAvailableListener(this);
            Locator.getInstance(context).removeLocationUpdatedListener(this);
            mEnabled = false;
        }
    }

    private boolean teleportsInProximityExist(PointF location) {
        // Check if user is exiting range of teleport
        Iterator<ITeleport> iter = mProximityTeleports.iterator();
        while (iter.hasNext()) {
            ITeleport teleport = iter.next();

            if (VectorHelper.squareDistance(teleport.getLocation(), location) > TELEPORT_RANGE_EXIT_SQ) {
                mDestinationTeleports.remove(teleport);
                iter.remove();
            }
        }

        // Check if user is entering range of new teleport
        List<Teleport> teleports = mBuilding.getCurrentFloor().getTeleports();
        for (Teleport teleport : teleports) {
            if (VectorHelper.squareDistance(teleport.getLocation(), location) <= TELEPORT_RANGE_ENTER_SQ) {
                mProximityTeleports.add(teleport);
            }
        }

        for (ITeleport teleport : mProximityTeleports) {
            mDestinationTeleports.put(teleport, mBuilding.getTeleportsById(teleport.getId()));
        }

        return !mProximityTeleports.isEmpty();
    }

    private ITeleport findDestinationTeleport(final WiFiLocator.WiFiFingerprint fingerprint) {
        if (mProximityTeleports.isEmpty()) return null;

        SortedMap<Float, ITeleport> candidates = new TreeMap<>(Collections.<Float>reverseOrder());

        for (ITeleport teleport : mProximityTeleports) {
            List<ITeleport> teleports = mDestinationTeleports.get(teleport);

            float minDissimilarity = WiFiLocator.dissimilarity(fingerprint, teleport.getFingerprint());
            ITeleport mostProbableTeleport = teleport;

            for (ITeleport destinationTeleport : teleports) {

                final float diff = WiFiLocator.dissimilarity(fingerprint, destinationTeleport.getFingerprint());

                if (diff < minDissimilarity) {
                    minDissimilarity = diff;
                    mostProbableTeleport = destinationTeleport;
                }
            }

            // If floor is different for this teleport, remember it
            if (mostProbableTeleport != teleport) {
                candidates.put(minDissimilarity, mostProbableTeleport);
            }
        }

        if (candidates.isEmpty()) return null;

        final float minDissimilarityAmongAllTeleports = candidates.firstKey();
        final ITeleport targetTeleport = candidates.get(minDissimilarityAmongAllTeleports);

        return targetTeleport;
    }

    @Override
    public void onFingerprintAvailable(WiFiLocator.WiFiFingerprint fingerprint) {
        mLastFingerprint = fingerprint;
    }

    @Override
    public void onLocationUpdated(PointF location) {
        if (teleportsInProximityExist(location)) {
            ITeleport destinationTeleport = findDestinationTeleport(mLastFingerprint);

            if (destinationTeleport != null) {
                emitFloorChangedEvent(destinationTeleport);
            }
        }
    }

    private void emitFloorChangedEvent(ITeleport destinationTeleport) {
        for (IFloorChangedHandler handler : mOnFloorChangedHandlers) {
            handler.onFloorChanged(destinationTeleport.getFloor());
        }
    }

    public void addOnFloorChangedListener(IFloorChangedHandler handler) {
        mOnFloorChangedHandlers.add(handler);
    }

    public void removeOnFloorChangedListenerHandler(IFloorChangedHandler handler) {
        mOnFloorChangedHandlers.remove(handler);
    }

    public Building getBuilding() {
        return mBuilding;
    }

    public void setBuilding(Building building) {
        mBuilding = building;
    }
}
