# Iteration 4 — verified tournament data and implementation specification

This supplements `specification_iteration_4.md` without replacing it. Verified by fetching the listing and two detail pages on 2026-10-03. No account or browser execution was needed to read these fields.

## Scraping evidence

| Property | Source markup | Result / limitation |
| --- | --- | --- |
| Tournament identity | `TournamentHome.cfm?ID_Tournament=...` links in `table.card` | Numeric ID is stable; normalize URL case and remove tracking parameters. |
| Full title | Detail page `<title>` / welcome heading | Detail title avoids truncated listing names. |
| Venue | Detail welcome `.card`, following `Hosted by` | May be linked or plain text; listing row supplies a fallback. |
| Address, city, region | Lines after the venue | Optional, preserve address text; listing location provides city/region fallback. |
| Date | Detail card date line | Observed `Oct 3,2026` and `Oct 3,2026 to Oct 04,2026`; preserve original text and parsed start/end dates. |
| Entry fees | Detail card lines such as `event - $amount entry` | Multiple named fees, including zero-cost later rounds. Store each option. |
| Starting price | Derived from listed event fees | Lowest positive listed fee; zero only if every listed fee is zero. Unknown or mixed currency symbols remain null. This is a listed event-option price, not a guaranteed total tournament cost. |
| Currency | Printed `$` symbol | Currency code is not disclosed; do not assume USD, since the listing includes Canadian venues. |

Live examples: [Maple Lanes event](https://tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=36040) lists multiple fees; [John Willey Memorial](https://tournamentbowl.com/open/TournamentHome.cfm?ID_Tournament=35497) includes a paid qualifying round and free subsequent rounds. These are illustrative source snapshots, not hardcoded application records.

The [upcoming list](https://tournamentbowl.com/open/tournaments.cfm?which=upcoming) includes city/region grouping and can contain ongoing or expired events. Use imported end date (or start date) to hide completed events. Undated records remain visible with “Date not listed.” A source that moves dates into flyers alone is outside structured HTML extraction; do not infer prices from unrelated dollar amounts in director notes.

## Model structure

`Tournament` (JPA entity, `tournament` table):

| Field | Type / constraints | Purpose |
| --- | --- | --- |
| id | generated Long primary key | Local identity |
| sourceId | Long, required, unique | Tournament Bowl ID and upsert identity |
| sourceUrl | String(1024), required | Canonical original listing detail URL |
| title | String(1000), required | Full event name |
| venueName, city | nullable String(255) | Venue/location search and grouping |
| region | nullable String(64) | State/province/region filter |
| address | nullable String(1000) | Original address lines |
| startDate, endDate | nullable LocalDate | Upcoming/ongoing visibility and range browsing |
| dateText | nullable String(255) | Retain source date formatting when parsing fails |
| entryPriceFrom | nullable BigDecimal(10,2) | Lowest positive listed fee, or zero if all are zero |
| currencySymbol | nullable String(8) | Source symbol rather than inferred currency code |
| importedAt | required Instant | Last successful refresh |
| entryFees | collection of `TournamentEntryFee` | Named fee options, in source order |

`TournamentEntryFee` is an embedded value stored in `tournament_entry_fee`: event name String(500), amount BigDecimal(10,2), symbol String(8), and collection position. DTO `TournamentData` carries parsed data; listing view DTOs materialize collections inside read transactions. No separate venue entity is needed yet: venues are grouped by normalized venue name + city + region, avoiding merging same-name venues in different cities.

Flyway V2 creates the tables and indexes. A unique source ID and a transaction-scoped PostgreSQL advisory lock serialize cooperating imports. Refresh replaces fee options while retaining tournament identity.

## Controller endpoints

| Method/path | Parameters | Behavior |
| --- | --- | --- |
| `GET /tournaments` | `q`, `venue`, `region`, `city`, `venueMissing`, `from`, `to`, `sort`, `page` | Stored upcoming/ongoing records, 20/page. Literal case-insensitive text search across title, venue, city, region. Case-insensitive exact venue/region filters. Optional exact `city` and `venueMissing` preserve location-card links, including missing venue names. Date range matches overlapping known dates; undated records are excluded when a range is selected. `sort=date` (default) or `date-desc`, undated last, stable ID tie-breaker. Negative/oversized page numbers are clamped; invalid or reversed dates return HTTP 400. |
| `GET /locations` | `q`, `region`, `page` | Venue cards grouped from matching upcoming stored records, 20 locations/page; counts, city/region, next date, and links to filtered tournament listings. |

These are read-only routes. No GET endpoint imports data. No public unauthenticated administrative import endpoint is added. Catalog navigation exposes Tournaments and Locations; both new screens use existing Bowler’s Journal colors, fonts, cards, buttons, and responsive spacing. Source links, fee details, clear filters, and empty states remain accessible without JavaScript.

## Explicit import flow

```sh
java -jar target/choco-cookies-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --tournaments.import=true \
  --tournaments.import.max-events=25
```

Default cap 25, permitted range 1–1000. An optional `--tournaments.import.source-id=35497` refreshes exactly one known tournament. Import is off by default. CLI closes the application context after import.

Fetcher uses HTTPS on the fixed Tournament Bowl host, validates IDs/paths, follows no redirects, has connection/request timeouts, checks robots rules, and spaces requests by at least the published 10-second crawl delay (or a larger current delay). No venue websites, ads, login pages, or flyers are scraped. Listing failure aborts; malformed/missing individual pages are reported and valid later records continue; database failures abort. Requests are bounded, duplicates within a listing are skipped, and reruns upsert by source ID. A single response HTML is parsed into an immutable DTO before writing.

## Acceptance and validation

- Source-derived HTML fixtures verify actual card structure, date ranges, fee semantics, plain-text venues, incomplete fields, foreign links, and malformed pages.
- Import tests verify deduplication, continuation after bad records, explicit single-event refresh, and bounds.
- Database/controller tests verify V2 migration, idempotent replacement, literal combined filtering, ongoing/expired/undated behavior, stable pagination, location grouping, missing-field display, HTML escaping, and no fetch during browsing.
- Desktop/mobile browser smoke verifies readable cards, no horizontal overflow, navigation, filters, and source/fee controls.
- Live scrape verification and an isolated import demonstrate real records without altering the configured production database.

## Implementation verification

- Live controlled listing import with a three-event cap: 3 inserted, 0 failed, persisted in an isolated H2 file database.
- Live single-event refresh: first import inserted; rerun updated the same ID, with no duplicate. Parsed the 2026-10-03–2026-10-04 event range and $175 starting fee correctly.
- Flyway V1/V2 migration and schema validation passed on H2 in PostgreSQL compatibility mode. The PostgreSQL advisory-lock branch requires PostgreSQL; H2 tests verify uniqueness and sequential updates.
- Parser/import/persistence/controller tests cover malformed details, explicit caps, missing fields, literal search, date overlaps, expired events, venue groups, pagination, HTML escaping, and no fetch on page views.
- Chrome checks passed at 1440, 900, and 390 pixels with actual source-derived records: no horizontal overflow, working filters, fee disclosure, navigation, and location links.
- Imports remain disabled by default. Run the documented command against the configured application database to populate the deployed pages; preview/live verification used temporary databases.
