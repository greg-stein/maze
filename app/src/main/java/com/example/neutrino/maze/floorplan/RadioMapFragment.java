package com.example.neutrino.maze.floorplan;

import java.util.List;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class RadioMapFragment {
    private String mFloorId;
    private List<Fingerprint> mFingerprints;

    public RadioMapFragment(List<Fingerprint> fingerprints, String floorId) {
        mFingerprints = fingerprints;
        mFloorId = floorId;
    }

    public List<Fingerprint> getFingerprints() {
        return mFingerprints;
    }

    public void setFingerprints(List<Fingerprint> fingerprints) {
        mFingerprints = fingerprints;
    }

    public String getFloorId() {
        return mFloorId;
    }
}