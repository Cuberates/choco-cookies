# Iteration 6 — Implementation Review

## Delivered

- Replaced the three static editorial featured cards with four database-backed selections in the requested order: Storm, Ebonite, 900 Global, Brunswick.
- Each card displays its real ball and matching core images in adjacent columns, plus source-backed name, brand, coverstock/core information, and a Bowwwl source link. Existing browser-arsenal saving and carousel controls continue to work.
- Added `FeaturedCatalog.BRAND_SLUGS`, randomized candidate selection, a CSRF-protected refresh POST, per-brand failure recovery, three-detail-attempt limits, five-minute cooldown, and concurrent-refresh protection.
- Added Flyway V4 and the `featured_ball` entity/repository. Featured snapshots are independent of catalog records and journal associations. Normal catalog GETs read the saved rows without network requests.
- Added the optional `--catalog.featured.refresh=true` startup command. Servlet-only security configuration now applies only in web contexts so operator commands can run without a web server.
- Preserved the Phaze II hero, permanently expanded filters, neutral colors, header logo, reduced corner radii, authentication, and journal.

## Initial data and source review

The brand listings and selected detail pages were retrieved from Bowwwl on 2026-10-04. Candidate detail URLs were deduplicated and randomly selected from each brand's listing. The initial rows are Phaze Crimson, Spartan, Reality Incursion, and Fury Orange/Red Pearl. All four have both ball and core image fields; optional missing specifications remain null.

The ball image is taken from `.field--name-field-ball-image`, while its core comes from `.field--name-field-core .field--name-field-core-image`. This prevents picking a brand logo or a similar-ball image. Image query tokens are retained; HTTPS source host/path/format validation rejects unrelated URLs. Fixtures preserve the relevant live markup shapes.

The Phaze Crimson product and Velocity A.I. core image URLs were downloaded successfully and visually inspected. Production browsers load the images directly from Bowwwl; no generated artwork is used.

## Verification

`./mvnw test`: **44 passed, 1 skipped, 0 failures, 0 errors**.

Fourteen new tests cover source parsing, brand isolation, image URL restrictions, query tokens, matched media fields, randomized selection, bounded retries, partial failures, cooldown, concurrency, interruption, seeded migration rows, card rendering, CSRF/POST requirements, persistence, and non-web command startup. The existing account/journal/catalog/tournament regressions pass.

The integration tests caught late CSRF session creation after the catalog response began rendering. A head metadata tag now materializes the token before the POST form renders, avoiding the committed-response session error while retaining CSRF protection.

`node --check src/main/resources/static/js/catalog.js` and `git diff --check` pass.

The optional PostgreSQL persistence test remains skipped without dedicated database configuration. Migration/schema checks use H2 PostgreSQL mode. Live PostgreSQL migration and browser viewport screenshots were not performed.

## Use

Restart the application to apply V4, then open `/catalog`. The seeded selections appear immediately. Use “Refresh featured balls” to choose new random balls. Refresh samples each brand listing's first page and can select a previously featured ball again. A source failure preserves the prior selection.

Image availability depends on Bowwwl. Failed image loads show labeled Ball/Core fallback text. Mobile CSS keeps both images side by side while the cards scroll horizontally.
