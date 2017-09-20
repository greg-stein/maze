package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;
import com.example.neutrino.maze.rendering.VectorHelper;

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

    private Locator locator;
    private final Set<ITeleport> mProximityTeleports = new HashSet<>();
    private Map<ITeleport, List<ITeleport>> mDestinationTeleports = new HashMap<>();
    private ITeleport mTargetTeleport;
    private boolean mUnsibscribed = false;
    private WiFiLocator.WiFiFingerprint mLastFingerprint;
    private List<OnFloorChangedHandler> mOnFloorChangedHandlers = new ArrayList<>();

    private boolean inRangeOfATeleport() {
        return mProximityTeleports.size() > 0;
    }

    public FloorWatcher(Locator locator) {
        this.locator = locator;
        WifiScanner.getInstance().addFingerprintAvailableListener(this);
        Locator.getInstance().addLocationUpdatedListener(this);
    }

    public void unsubscribe() {
        if (!mUnsibscribed) {
            WifiScanner.getInstance().removeFingerprintAvailableListener(this);
            Locator.getInstance().removeLocationUpdatedListener(this);
            mUnsibscribed = true;
        }
    }

    private boolean teleportsInProximityExist(PointF location) {
        // Check if user exit range of teleports
        Iterator<ITeleport> iter = mProximityTeleports.iterator();
        while (iter.hasNext()) {
            ITeleport teleport = iter.next();

            if (VectorHelper.squareDistance(teleport.getLocation(), location) > TELEPORT_RANGE_EXIT_SQ) {
                mDestinationTeleports.remove(teleport);
                iter.remove();
            }
        }

        // Check if user enter range of new teleport
        List<ITeleport> teleports = locator.getFloorPlan().getTeleportsOnFloor();
        for (ITeleport teleport : teleports) {
            if (VectorHelper.squareDistance(teleport.getLocation(), location) <= TELEPORT_RANGE_ENTER_SQ) {
                mProximityTeleports.add(teleport);
            }
        }

        for (ITeleport teleport : mProximityTeleports) {
            mDestinationTeleports.put(teleport, locator.getFloorPlan().getTeleportsById(teleport.getId()));
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
        for (OnFloorChangedHandler handler : mOnFloorChangedHandlers) {
            handler.onFloorChanged(destinationTeleport);
        }
    }

    public void addOnFloorChangedListenerHandler(OnFloorChangedHandler handler) {
        mOnFloorChangedHandlers.add(handler);
    }

    public void removeOnFloorChangedListenerHandler(OnFloorChangedHandler handler) {
        mOnFloorChangedHandlers.remove(handler);
    }

    public interface OnFloorChangedHandler {
        void onFloorChanged(ITeleport destinationTeleport);
    }
}
