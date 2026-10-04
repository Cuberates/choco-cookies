# Specification: Iteration 6 — Random Bowwwl Ball and Core Showcase

## Source and scope

Implements `requirements_iteration_6.md`: use `["storm", "ebonite", "900-global", "brunswick"]` to fetch random balls with their real Bowwwl product and core images, displayed next to each other.

The featured catalog cards become a four-brand showcase with one random ball per brand. The existing Phaze II hero, expanded catalog filters, public search, journal, neutral colors, shared logo, and reduced corner radii are preserved.

## Selection and refresh

- Keep the four permitted brand slugs in an immutable ordered list.
- Fetch each brand's Bowwwl listing and extract distinct ball-detail links for that exact brand. Ignore brand links, core directories, promotions, and other-brand links.
- Randomly shuffle candidates from the listing's first page and select the first detail page with both usable images. Limit detail attempts to three per brand so missing images cannot trigger an unbounded crawl.
- “Refresh featured balls” performs an explicit CSRF-protected POST. Normal catalog GETs read the saved selections without scraping.
- Use the existing Bowwwl fetcher, including its robots checks, two-second pacing, timeouts, and fixed-host URL restrictions.
- Preserve the previous card when a brand cannot be refreshed; successful brands may still update. Show a concise complete/partial/failure status without raw network errors.
- Limit refresh attempts to once per five minutes per application instance and prevent concurrent refreshes within that instance.
- Provide a property-controlled startup command for operators to refresh outside the UI.

## Data and initial content

- Add a Flyway-managed `featured_ball` table, keyed by brand slug, with ball name, brand display name, source URL, product image URL, core image URL, optional core name/type and coverstock type, and selection timestamp.
- Keep showcase selections separate from catalog rows and journal ball associations. Refreshes must not replace or delete user-linked catalog records.
- Seed the four slots with real, randomly selected and verified Bowwwl detail-page data gathered during implementation so the showcase appears immediately after migration.
- Product and core images must come from the same detail page. Accept only HTTPS Bowwwl `/sites/default/files/` image URLs; preserve Drupal image query tokens. Never mistake brand logos or similar-ball thumbnails for the selected ball/core.
- Missing/unsafe images disqualify that candidate. Missing specifications display “Not listed.”

## Presentation

- Replace the three static editorial cards with four live-data cards in the existing carousel layout.
- Each card shows brand/name, a labeled ball image and core image in two adjacent columns, available coverstock/core information, a source link, and the existing save-to-browser-arsenal action.
- Use `object-fit: contain` so complete ball and core artwork remains visible. Keep both images side by side on mobile; the cards themselves can scroll horizontally.
- Use descriptive alt text and visible Ball/Core captions. Show a labeled fallback if an image fails in the browser rather than inventing artwork.
- Keep carousel controls neutral and avoid fabricating RG, differential, finish, release-year, lane-fit, or reaction claims absent from the imported data.
- The source link opens Bowwwl with `noopener noreferrer`. The card's local search link uses the actual brand/name.

## Acceptance and tests

1. Exactly the four requested brands are represented after migration.
2. Refresh uses randomized eligible candidates and selects only complete matching image pairs.
3. Product and core images are visibly adjacent on desktop and mobile.
4. Normal page rendering never calls the remote fetcher.
5. Failures retain existing cards; refresh cooldown/concurrency bounds are enforced.
6. Parser tests cover real markup shapes, relative/absolute URLs, query preservation, unsafe hosts, missing images, and unrelated thumbnails.
7. Service tests cover brand isolation, retries, complete/partial failures, persistence, random ordering, cooldown, and interruption.
8. Integration tests cover migration seeds, rendered source/images, POST/CSRF behavior, and existing catalog/journal regressions.

## Operational limits

Random selection is from each brand listing's first page, not an exhaustive crawl of every historical ball. Image URLs are stored locally, but browsers load the real images from Bowwwl; external image availability remains dependent on the source. Refresh cooldown is per application instance.
