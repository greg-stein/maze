package world.maze.floorplan;

import world.maze.floorplan.transitions.Teleport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Greg Stein on 7/5/2017.
 * Entity to serialize/deserialize for the webservice
 */
public class Floor {
    public static Floor current;

    private String mName;
    private String mId; // TODO: this should be changed to ObjectId later
    private List<Teleport> mTeleports;
    private List<Tag> mTags;
    private Set<String> mAccessPoints; // TODO: HashSet<Long>
    private transient boolean mIsSelected;

    public Floor() {}

    public Floor(String name, String id) {
        mName = name;
        mId = id;
        mTags = new ArrayList<>();
        mTeleports = new ArrayList<>();
        mAccessPoints = new HashSet<>();
    }

    public void addMacs(Set<String> macs) {
        if (macs != null) mAccessPoints.addAll(macs);
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public List<Teleport> getTeleports() {
        return mTeleports;
    }

    public void setTeleports(List<Teleport> teleports) {
        mTeleports = teleports;
    }

    public void addTag(Tag tag) {
        mTags.add(tag);
    }

    public void removeTag(Tag tag) {
        mTags.remove(tag);
    }

    public List<Tag> getTags() {
        if (mTags != null) {
            return Collections.unmodifiableList(mTags); // TODO: WHY??
        }
        return null;
    }

    public void setTags(List<Tag> tags) {
        mTags = tags;
    }

    public void addTeleport(Teleport teleport) {
        mTeleports.add(teleport);
    }

    public void removeTeleport(Teleport teleport) {
        mTeleports.remove(teleport);
    }

    public Set<String> getMacs() {
        return mAccessPoints;
    }
}
