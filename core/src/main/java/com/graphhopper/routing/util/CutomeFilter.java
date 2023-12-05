package com.graphhopper.routing.util;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;

public class CutomeFilter implements EdgeFilter {
    @Override
    public boolean accept(EdgeIteratorState edgeState) {
        return false;
    }
}
