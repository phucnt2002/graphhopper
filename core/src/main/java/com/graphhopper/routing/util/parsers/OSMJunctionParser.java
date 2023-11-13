package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.Junction;
import com.graphhopper.storage.IntsRef;

public class OSMJunctionParser implements TagParser {
    private final EnumEncodedValue<Junction> junctionEnc;
    public OSMJunctionParser(EnumEncodedValue<Junction> junctionEnc) {
        this.junctionEnc = junctionEnc;
    }
    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String junction = way.getTag("junction");
        junctionEnc.setEnum(false, edgeId, edgeIntAccess, Junction.find(junction));
    }
}
