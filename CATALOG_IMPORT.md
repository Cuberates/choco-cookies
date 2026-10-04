# Bowwwl catalog import

The catalog is stored in PostgreSQL using JPA. Flyway applies
`src/main/resources/db/migration/V1__create_bowling_catalog.sql`; Hibernate validates
that schema instead of creating it. Use the environment variables in DATABASE.md.
Run migrations with the application on a new database; existing unrelated tables
are not automatically baselined. Never enable automatic schema creation in production.

## Source inspection (2026-10-03)

- Listing: https://www.bowwwl.com/bowling-ball-database . Drupal HTML table rows
  use `td.views-field-nothing-2` for detail links, `.coverstock-type` and
  `.core-type` for categories, and image alt text for brand logos.
- The listing defaults to weight 15. Clear it with `?weight=`. The inspected
  unfiltered listing has pagination through `?weight=&page=81`; follow the
  `.pagination a[rel=next]` links rather than hardcoding the page count.
  Listing rows may repeat a ball for different weights; imports deduplicate links.
- Detail pages expose a title, canonical URL, brand, coverstock type, core type,
  and separate `h6` headings such as `16 pounds` in the core-specs field.
  https://www.bowwwl.com/bowling-ball-database/motiv/raptor-pursuit lists
  12, 13, 14, 15, and 16 pounds. These are multiple discrete values, not one
  number or a continuous range. Store each weight in `bowling_ball_weight`;
  a future weight filter should test membership in this set.
- No stable ball ID was exposed in the inspected detail HTML. The canonical
  HTTPS URL, stripped of query strings, fragments, and trailing slashes, is the
  unique source key. `source_id` is nullable for a future supported source ID.
- Missing optional string attributes are SQL NULL. Missing weight information
  has no weight rows, meaning unknown (not zero pounds). Future filter choices
  should exclude missing values and the page should display “Unspecified”.
- https://www.bowwwl.com/robots.txt currently does not disallow the catalog paths.
  The importer checks robots rules before requesting pages and conservatively
  honors all Disallow groups. It makes at most one request every two seconds,
  uses timeouts, and aborts on network errors, redirects, or non-200 responses.
- https://www.bowwwl.com/terms-and-conditions restricts copying, redistribution,
  and republication. Robots permission is not a content license. Resolve reuse
  permission with the site owner before bulk importing or publishing its data.
  No bulk import is part of setup or automated testing.

## Deliberate admin command

Only an operator with shell access and database credentials can run the importer.
There is no scheduler or normal-startup import. The Search button can also import a single missing ball as described below. Export
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` through your secure environment configuration.

Build and import at most one listing page first:

```sh
./mvnw -DskipTests package
java -jar target/choco-cookies-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --catalog.import=true --catalog.import.max-pages=1
```

After checking the sample and obtaining appropriate source reuse permission,
raise `catalog.import.max-pages` (maximum 1000) to traverse more listing pages.
The default is one. Each run begins at the first listing page; the process exits
when it finishes. Never add `catalog.import=true` to the normal Render service
configuration. Use a separate operator command on the matching database network.

Successful runs print inserted, updated, skipped, and malformed counts. “Updated”
includes an existing record that was unchanged. Duplicate links/canonical keys
within a run are skipped. Malformed detail pages include source URL and a bounded
parser reason; passwords and raw HTML are not logged. A missing listing table
aborts rather than silently reporting an empty import. Network/database failures
abort with a nonzero exit status. Already committed records remain available;
rerunning updates those records rather than duplicating them. A database unique
constraint and transaction advisory lock protect concurrent cooperating imports.

## Tests

```sh
./mvnw -Dtest=CatalogImportTests test
```

Fixtures under `src/test/resources/fixtures/bowwwl` contain synthetic records
using the inspected HTML structure, without copied descriptions or images.
Tests never access the live site. They check optional attributes, multiple weights,
canonical keys, malformed input, pagination, duplicate counts, and repeat updates.

`CatalogPersistenceTests` is opt-in and checks the real PostgreSQL migration and
repeated upserts, including weight replacement and stable record IDs. Point these
variables at a disposable test database (not a production database):

- `CATALOG_TEST_DATABASE_URL`
- `CATALOG_TEST_DATABASE_USERNAME`
- `CATALOG_TEST_DATABASE_PASSWORD`

Then run `./mvnw -Dtest=CatalogPersistenceTests test`. Flyway creates the schema;
the test's record changes are rolled back. Without these variables this test skips.

## Browse the catalog

Open `/catalog` (or `/`) after starting the application. Search matches literal, case-insensitive substrings in ball names, brands,
coverstock types, and core types. Weight matches any listed weight for a ball; brand,
coverstock type, and core type match exactly. Filters combine, and their choices
come from the complete imported catalog, excluding missing values. Missing
attributes display as “Unspecified”.

Results default to name A–Z with 20 balls per page; name Z–A is also available.
Pagination preserves the selected filters and sorting. Search resets to the first
page, and “Clear filters” restores defaults.

Run the offline catalog and parser tests with
`./mvnw test -Dtest=CatalogSearchTests,CatalogImportTests`. Catalog search tests use
an isolated H2 database with the Flyway migration and never fetch Bowwwl.

## Import one ball by brand and name

Use source slugs or names with spaces (converted to lowercase hyphenated slugs):

```sh
java -jar target/choco-cookies-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --catalog.import=true \
  --catalog.import.brand=storm \
  --catalog.import.name=phaze-ii
```

This fetches `https://www.bowwwl.com/bowling-ball-database/storm/phaze-ii`
and inserts or updates that ball without visiting listing pages. Both flags are
required for this mode; max-pages applies only to listing imports. Use the exact
source spelling: “Phaze II” becomes `phaze-ii`, while “Phaze 2” becomes `phaze-2`
and may return HTTP 404. Robots checks, request pacing, parsing, and canonical-key
upserts are shared with the listing importer. Pagination and ordinary catalog browsing never import data.

## Search-button lookup

The Search form submits `GET /catalog/search`. It checks local name/brand matches
first. If missing, a brand and full name construct the Bowwwl detail URL; an existing
canonical record is reused, otherwise that single page is fetched and saved. Brand
accepts typed values as well as suggestions, including brands not yet imported.
The response redirects to `/catalog` with filters preserved and a lookup status.
Pagination, refresh after redirect, and `/catalog` itself only read the database.
Example: `/catalog/search?brand=Storm&name=Phaze%20II`. Use source spelling rather
than guessing a slug; Phaze 2 does not resolve to Phaze II. This explicitly requested
search-button behavior supersedes iteration 2's original admin-only import restriction.

## Lane Index design

The catalog uses a neutral version of the Lane Index design with local hero artwork,
SVG icons, Inter, and Roboto Mono fonts under `src/main/resources/static`.
Featured cards now use real Bowwwl selections and adjacent ball/core images as
described below; “See more” opens live database results. Hero totals reflect actual
records, brands, and available weights. Search accepts ball names, brands,
coverstocks, and cores. Filters are always expanded; combine them or supply the
full brand/name for a missing ball.

Bookmarks save an arsenal in this browser's local storage. Compare and Arsenal
Builder show those selections; they are not synchronized to an account. Accounts
and private league journals are available through Sign in and Journal. Email
delivery is not configured; the email form never stores or sends an address.
Ctrl/Command K focuses the search.
Font licenses are included alongside the locally served font files.

## Iteration 6: random featured balls

`FeaturedCatalog.BRAND_SLUGS` holds `storm`, `ebonite`, `900-global`, and `brunswick`.
Each slot in `featured_ball` stores one selected ball and its matching core image.
Flyway V4 seeds four real selections randomly drawn and verified on 2026-10-04:
Phaze Crimson, Spartan, Reality Incursion, and Fury Orange/Red Pearl. The source
URLs and image query tokens are preserved in the migration.

“Refresh featured balls” submits CSRF-protected `POST /catalog/featured/refresh`.
It reads each brand's first listing page, shuffles its distinct card-detail links,
and tries at most three candidates to find a complete image pair. Brand listing
links use `a.card-link[rel=bookmark]`, rather than the main catalog's table selector.
Product images use `.field--name-field-ball-image img`; cores use
`.field--name-field-core .field--name-field-core-image img`. Logos and similar-ball
thumbnails are excluded. Only HTTPS `www.bowwwl.com/sites/default/files/` raster
image URLs are accepted.

The saved four slots survive restarts. Normal page loads perform no scraping;
browsers request their images directly from Bowwwl. A failed brand retains its
previous slot. The existing fetcher enforces robots checks, pacing, and timeouts.
Refresh is limited to once every five minutes and one in-flight operation per
application instance. Selection is random within the first listing page, not the
brand's complete historical catalog. Featured records are separate from imported
catalog rows and journal equipment associations.

Operators can also refresh once at startup with:

```sh
java -jar target/choco-cookies-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --catalog.featured.refresh=true
```

Use the same PostgreSQL datasource settings as other import commands. Network
failures retain existing data and print a summary. No source requests are made
by automated tests; parser/service tests use focused fixtures and mocked fetches.
