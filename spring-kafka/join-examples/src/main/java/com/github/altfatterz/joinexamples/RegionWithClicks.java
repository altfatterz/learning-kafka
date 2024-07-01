package com.github.altfatterz.joinexamples;

public class RegionWithClicks {

    private final String region;
    private final long clicks;

    public RegionWithClicks(final String region, final long clicks) {
        if (region == null || region.isEmpty()) {
            throw new IllegalArgumentException("region must be set");
        }
        if (clicks < 0) {
            throw new IllegalArgumentException("clicks must not be negative");
        }
        this.region = region;
        this.clicks = clicks;
    }

    String getRegion() {
        return region;
    }

    long getClicks() {
        return clicks;
    }

}
