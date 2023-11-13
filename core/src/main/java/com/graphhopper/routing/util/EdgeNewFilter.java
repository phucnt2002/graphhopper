package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public class EdgeNewFilter implements EdgeFilter{
    private double weightThreshold;

    public EdgeNewFilter(double weightThreshold) {
        this.weightThreshold = weightThreshold;
    }

    @Override
    public boolean accept(EdgeIteratorState edgeState) {
        double totalWeight = edgeState.getEdge() + edgeState.getAdjNode();
        return totalWeight > weightThreshold;
    }
}
