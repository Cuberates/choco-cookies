package com.stormhacks2026.choco_cookies.catalog.featured;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "featured_ball")
public class FeaturedBall {
    @Id @Column(length = 40) private String brandSlug;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String brand;
    @Column(nullable = false, length = 1024) private String sourceUrl;
    @Column(nullable = false, length = 2048) private String ballImageUrl;
    @Column(nullable = false, length = 2048) private String coreImageUrl;
    private String coreName;
    private String coverstockType;
    private String coreType;
    @Column(nullable = false) private Instant selectedAt;

    protected FeaturedBall() {}
    public FeaturedBall(String brandSlug, String brand, FeaturedBallParser.Detail detail, Instant selectedAt) {
        this.brandSlug = brandSlug; this.brand = brand; this.name = detail.name();
        this.sourceUrl = detail.sourceUrl(); this.ballImageUrl = detail.ballImageUrl();
        this.coreImageUrl = detail.coreImageUrl(); this.coreName = detail.coreName();
        this.coverstockType = detail.coverstockType(); this.coreType = detail.coreType(); this.selectedAt = selectedAt;
    }
    public String getBrandSlug() { return brandSlug; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getSourceUrl() { return sourceUrl; }
    public String getBallImageUrl() { return ballImageUrl; }
    public String getCoreImageUrl() { return coreImageUrl; }
    public String getCoreName() { return coreName; }
    public String getCoverstockType() { return coverstockType; }
    public String getCoreType() { return coreType; }
    public Instant getSelectedAt() { return selectedAt; }
}
