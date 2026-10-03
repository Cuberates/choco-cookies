package com.stormhacks2026.choco_cookies.catalog;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bowling_ball")
public class BowlingBall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 1024)
    private String sourceKey;
    @Column(nullable = false, length = 1024)
    private String sourceUrl;
    private String sourceId;
    @Column(nullable = false)
    private String name;
    private String brand;
    private String coverstockType;
    private String coreType;
    @ElementCollection
    @CollectionTable(name = "bowling_ball_weight", joinColumns = @JoinColumn(name = "ball_id"))
    @Column(name = "weight", nullable = false)
    private Set<Integer> weights = new HashSet<>();

    public BowlingBall() {}
    public void update(BallData data) {
        sourceKey = data.sourceKey(); sourceUrl = data.sourceUrl(); sourceId = data.sourceId();
        name = data.name(); brand = data.brand(); coverstockType = data.coverstockType();
        coreType = data.coreType(); weights.clear(); weights.addAll(data.weights());
    }
    public Long getId() { return id; }
    public String getSourceKey() { return sourceKey; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSourceId() { return sourceId; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCoverstockType() { return coverstockType; }
    public String getCoreType() { return coreType; }
    public Set<Integer> getWeights() { return Set.copyOf(weights); }
}
