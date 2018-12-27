package world.maze.floorplan;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class RadioMapFragment {
    private String mFloorId;
    private Set<Fingerprint> mFingerprints = new HashSet<>();

    public RadioMapFragment(List<Fingerprint> fingerprints, String floorId) {
        if (fingerprints != null) mFingerprints.addAll(fingerprints);
        mFloorId = floorId;
    }

    // TODO: return read only view
    public Set<Fingerprint> getFingerprints() {
        return mFingerprints;
    }

    public void setFingerprints(Set<Fingerprint> fingerprints) {
        mFingerprints = fingerprints;
    }

    public String getFloorId() {
        return mFloorId;
    }

    public void addFingerprint(Fingerprint fingerprint) {
        mFingerprints.add(fingerprint);
    }

    public void add(Collection<Fingerprint> fingerprints) {
        mFingerprints.addAll(fingerprints);
    }

    public Collection<IFloorPlanPrimitive> getFingerprintsAsIFloorPlanElements() {
        return (Collection<IFloorPlanPrimitive>) (Collection<? extends IFloorPlanPrimitive>) mFingerprints;
    }

    public void clear() {
        mFingerprints.clear();
    }
}