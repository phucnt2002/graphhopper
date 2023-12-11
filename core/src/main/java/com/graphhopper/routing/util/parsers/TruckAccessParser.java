package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;

import java.util.*;
public class TruckAccessParser extends AbstractAccessParser implements TagParser {

    protected final Set<String> trackTypeValues = new HashSet<>();
    protected final Set<String> highwayValues = new HashSet<>();
    protected final BooleanEncodedValue roundaboutEnc;

    public TruckAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", "truck"))),
                lookup.getBooleanEncodedValue(Roundabout.KEY),
                properties,
                TransportationMode.HGV
        );
    }

    public TruckAccessParser(BooleanEncodedValue accessEnc,
                           BooleanEncodedValue roundaboutEnc, PMap properties,
                           TransportationMode transportationMode) {
        super(accessEnc, transportationMode);
        this.roundaboutEnc = roundaboutEnc;
        restrictedValues.add("delivery");

        blockPrivate(properties.getBool("block_private", true));
        blockFords(properties.getBool("block_fords", false));

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("permissive");

        barriers.add("kissing_gate");
        barriers.add("fence");
        barriers.add("bollard");
        barriers.add("stile");
        barriers.add("turnstile");
        barriers.add("cycle_barrier");
        barriers.add("motorcycle_barrier");
        barriers.add("block");
        barriers.add("bus_trap");
        barriers.add("sump_buster");
        barriers.add("jersey_barrier");

        highwayValues.addAll(Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link",
                "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
                "unclassified", "residential", "living_street", "service", "road", "track"));

        trackTypeValues.addAll(Arrays.asList("grade1", "grade2", "grade3", null));
    }

    public WayAccess getAccess(ReaderWay way) {
        // TODO: Ferries have conditionals, like opening hours or are closed during some time in the year
        String highwayValue = way.getTag("highway");
        String firstValue = way.getFirstPriorityTag(restrictions);
        if (highwayValue == null) {
            if (FerrySpeedCalculator.isFerry(way)) {
                if (restrictedValues.contains(firstValue))
                    return WayAccess.CAN_SKIP;
                if (intendedValues.contains(firstValue) ||
                        // implied default is allowed only if foot and bicycle is not specified:
                        firstValue.isEmpty() && !way.hasTag("foot") && !way.hasTag("bicycle") ||
                        // if hgv is allowed than smaller trucks and cars are allowed too
                        way.hasTag("hgv", "yes"))
                    return WayAccess.FERRY;
            }
            return WayAccess.CAN_SKIP;
        }

        if ("service".equals(highwayValue) && "emergency_access".equals(way.getTag("service")))
            return WayAccess.CAN_SKIP;

        if ("track".equals(highwayValue) && !trackTypeValues.contains(way.getTag("tracktype")))
            return WayAccess.CAN_SKIP;

        if (!highwayValues.contains(highwayValue))
            return WayAccess.CAN_SKIP;

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            return WayAccess.CAN_SKIP;

        // multiple restrictions needs special handling
        boolean permittedWayConditionallyRestricted = getConditionalTagInspector().isPermittedWayConditionallyRestricted(way);
        boolean restrictedWayConditionallyPermitted = getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way);
        if (!firstValue.isEmpty()) {
            String[] restrict = firstValue.split(";");
            for (String value : restrict) {
                if (restrictedValues.contains(value) && !restrictedWayConditionallyPermitted)
                    return WayAccess.CAN_SKIP;
                if (intendedValues.contains(value) && !permittedWayConditionallyRestricted)
                    return WayAccess.WAY;
            }
        }

        if (isBlockFords() && ("ford".equals(highwayValue) || way.hasTag("ford")))
            return WayAccess.CAN_SKIP;

        if (permittedWayConditionallyRestricted)
            return WayAccess.CAN_SKIP;

        return WayAccess.WAY;
    }
    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        WayAccess access = getAccess(way);
        if (access.canSkip())
            return;

        if (!access.isFerry()) {
            boolean isRoundabout = roundaboutEnc.getBool(false, edgeId, edgeIntAccess);
            if (isOneway(way) || isRoundabout) {
                if (isForwardOneway(way))
                    accessEnc.setBool(false, edgeId, edgeIntAccess, true);
                if (isBackwardOneway(way))
                    accessEnc.setBool(true, edgeId, edgeIntAccess, true);
            } else {
                accessEnc.setBool(false, edgeId, edgeIntAccess, true);
                accessEnc.setBool(true, edgeId, edgeIntAccess, true);
            }

        } else {
            accessEnc.setBool(false, edgeId, edgeIntAccess, true);
            accessEnc.setBool(true, edgeId, edgeIntAccess, true);
        }

        if (way.hasTag("gh:barrier_edge")) {
            List<Map<String, Object>> nodeTags = way.getTag("node_tags", Collections.emptyList());
            handleBarrierEdge(edgeId, edgeIntAccess, nodeTags.get(0));
        }
    }
    /**
     * make sure that isOneway is called before
     */
    protected boolean isBackwardOneway(ReaderWay way) {
        return way.hasTag("oneway", "-1")
                || way.hasTag("vehicle:forward", restrictedValues)
                || way.hasTag("motor_vehicle:forward", restrictedValues);
    }

    /**
     * make sure that isOneway is called before
     */
    protected boolean isForwardOneway(ReaderWay way) {
        return !way.hasTag("oneway", "-1")
                && !way.hasTag("vehicle:forward", restrictedValues)
                && !way.hasTag("motor_vehicle:forward", restrictedValues);
    }

    protected boolean isOneway(ReaderWay way) {
        return way.hasTag("oneway", oneways)
                || way.hasTag("vehicle:backward", restrictedValues)
                || way.hasTag("vehicle:forward", restrictedValues)
                || way.hasTag("motor_vehicle:backward", restrictedValues)
                || way.hasTag("motor_vehicle:forward", restrictedValues);
    }
}
