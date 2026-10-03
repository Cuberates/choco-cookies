package com.stormhacks2026.choco_cookies.catalog;

import java.util.Set;

public record BallData(String sourceKey, String sourceUrl, String sourceId, String name,
                       String brand, String coverstockType, String coreType, Set<Integer> weights) {}
