package com.stormhacks2026.choco_cookies.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BowlingBallRepository extends JpaRepository<BowlingBall, Long>, JpaSpecificationExecutor<BowlingBall> {
    @org.springframework.data.jpa.repository.Query("select distinct b.brand from BowlingBall b where b.brand is not null order by b.brand")
    java.util.List<String> findBrands();
    @org.springframework.data.jpa.repository.Query("select distinct b.coverstockType from BowlingBall b where b.coverstockType is not null order by b.coverstockType")
    java.util.List<String> findCoverstockTypes();
    @org.springframework.data.jpa.repository.Query("select distinct b.coreType from BowlingBall b where b.coreType is not null order by b.coreType")
    java.util.List<String> findCoreTypes();
    @org.springframework.data.jpa.repository.Query("select distinct w from BowlingBall b join b.weights w order by w")
    java.util.List<Integer> findWeights();
    Optional<BowlingBall> findBySourceKey(String sourceKey);
}
