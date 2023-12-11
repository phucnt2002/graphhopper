package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TruckAverageSpeedParser extends AbstractAverageSpeedParser implements  TagParser {

    protected final Map<String, Integer> trackTypeSpeedMap = new HashMap<>();
    protected final Set<String> badSurfaceSpeedMap = new HashSet<>();
    // This value determines the maximal possible on roads with bad surfaces
    private final int badSurfaceSpeed;
    protected final Map<String, Integer> defaultSpeedMap = new HashMap<>();

    public TruckAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "truck"))),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY));
    }

    public TruckAverageSpeedParser(DecimalEncodedValue speedEnc, DecimalEncodedValue ferrySpeed) {
        super(speedEnc, ferrySpeed);

        badSurfaceSpeedMap.add("cobblestone");
        badSurfaceSpeedMap.add("unhewn_cobblestone");
        badSurfaceSpeedMap.add("sett");
        badSurfaceSpeedMap.add("grass_paver");
        badSurfaceSpeedMap.add("gravel");
        badSurfaceSpeedMap.add("fine_gravel");
        badSurfaceSpeedMap.add("pebblestone");
        badSurfaceSpeedMap.add("sand");
        badSurfaceSpeedMap.add("paving_stones");
        badSurfaceSpeedMap.add("dirt");
        badSurfaceSpeedMap.add("earth");
        badSurfaceSpeedMap.add("ground");
        badSurfaceSpeedMap.add("wood");
        badSurfaceSpeedMap.add("grass");
        badSurfaceSpeedMap.add("unpaved");
        badSurfaceSpeedMap.add("compacted");

        // autobahn
        defaultSpeedMap.put("motorway", 100);
        defaultSpeedMap.put("motorway_link", 70);
        // bundesstraße
        defaultSpeedMap.put("trunk", 70);
        defaultSpeedMap.put("trunk_link", 65);
        // linking bigger town
        defaultSpeedMap.put("primary", 65);
        defaultSpeedMap.put("primary_link", 60);
        // linking towns + villages
        defaultSpeedMap.put("secondary", 60);
        defaultSpeedMap.put("secondary_link", 50);
        // streets without middle line separation
        defaultSpeedMap.put("tertiary", 50);
        defaultSpeedMap.put("tertiary_link", 40);
        defaultSpeedMap.put("unclassified", 30);
        defaultSpeedMap.put("residential", 30);
        // spielstraße
        defaultSpeedMap.put("living_street", 5);
        defaultSpeedMap.put("service", 20);
        // unknown road
        defaultSpeedMap.put("road", 20);
        // forestry stuff
        defaultSpeedMap.put("track", 15);

        trackTypeSpeedMap.put("grade1", 20); // paved
        trackTypeSpeedMap.put("grade2", 15); // now unpaved - gravel mixed with ...
        trackTypeSpeedMap.put("grade3", 10); // ... hard and soft materials
        trackTypeSpeedMap.put(null, defaultSpeedMap.get("track"));

        // limit speed on bad surfaces to 40 km/h
        badSurfaceSpeed = 40;
    }

    protected double getSpeed(ReaderWay way) {
        String highwayValue = way.getTag("highway", "");
        Integer speed = defaultSpeedMap.get(highwayValue);

        // even inaccessible edges get a speed assigned
        if (speed == null) speed = 10;

        if (highwayValue.equals("track")) {
            String tt = way.getTag("tracktype");
            if (!Helper.isEmpty(tt)) {
                Integer tInt = trackTypeSpeedMap.get(tt);
                if (tInt != null)
                    speed = tInt;
            }
        }

        return speed;
    }
    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        if (FerrySpeedCalculator.isFerry(way)) {
            double ferrySpeed = FerrySpeedCalculator.minmax(ferrySpeedEnc.getDecimal(false, edgeId, edgeIntAccess), avgSpeedEnc);
            setSpeed(false, edgeId, edgeIntAccess, ferrySpeed);
            if (avgSpeedEnc.isStoreTwoDirections())
                setSpeed(true, edgeId, edgeIntAccess, ferrySpeed);
            return;
        }

        // get assumed speed from highway type
        double speed = getSpeed(way);
        speed = applyBadSurfaceSpeed(way, speed);

        setSpeed(false, edgeId, edgeIntAccess, applyMaxSpeed(way, speed, false));
        setSpeed(true, edgeId, edgeIntAccess, applyMaxSpeed(way, speed, true));
    }
    protected double applyMaxSpeed(ReaderWay way, double speed, boolean bwd) {
        double maxSpeed = getMaxSpeed(way, bwd);
        return Math.min(140, isValidSpeed(maxSpeed) ? Math.max(1, maxSpeed * 0.9) : speed);
    }
    protected double applyBadSurfaceSpeed(ReaderWay way, double speed) {
        // limit speed if bad surface
        if (badSurfaceSpeed > 0 && isValidSpeed(speed) && speed > badSurfaceSpeed) {
            String surface = way.getTag("surface", "");
            int colonIndex = surface.indexOf(":");
            if (colonIndex != -1)
                surface = surface.substring(0, colonIndex);
            if (badSurfaceSpeedMap.contains(surface))
                speed = badSurfaceSpeed;
        }
        return speed;
    }
}

