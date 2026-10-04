# Specification: Iteration 4 – Tournament Listing and Location Page

## Objective

Add a tournament listing feature sourced from Tournament Bowl and present it in a format consistent with the existing catalog page design. The feature should provide a clear, searchable, and user-friendly view of upcoming tournaments, including the relevant location and pricing details.

## Background

The application currently supports a bowling ball catalog and related UI patterns. This iteration extends the product by adding a tournament list pulled from the Tournament Bowl upcoming tournaments page:

https://tournamentbowl.com/open/tournaments.cfm?which=upcoming

The goal is to surface upcoming tournament opportunities in a way that matches the catalog page design and is easy to browse.

## Functional Requirements

### 1. Tournament data import

- Scrape upcoming tournament data from the Tournament Bowl upcoming tournaments page.
- For each tournament entry, capture the following fields:
  - tournament name or event title
  - lane name / venue name
  - entry price starting at
  - date
  - source URL for the tournament listing item
- Extract the relevant details from each tournament hyperlink, not only the summary row.
- Handle incomplete or missing values gracefully without breaking the page.
- Store the scraped tournament records in a data model suitable for display and future filtering.

### 2. Data ingestion and reliability

- Use a controlled, explicit import process rather than scraping during normal page load.
- The fetcher should be repeatable and resilient to missing or malformed data.
- The import should avoid relying on live scraping during the runtime experience unless explicitly triggered.
- Preserve the source data so the imported record remains traceable to the original Tournament Bowl listing.
- If a tournament record is already present, update the existing entry instead of creating duplicates.

### 3. Tournament listing page

- Add a tournament list page that displays the upcoming events.
- Present the list in a structured card or table layout consistent with the catalog page design.
- Show at least the following information for each tournament:
  - tournament name
  - venue / lane name
  - date
  - entry price
- Use a clean, readable layout with consistent spacing and styling.

### 4. Location tab / section

- Add a location-oriented tab or section that follows the visual design pattern of the catalog page.
- The location view should make it easy for users to identify the venue or location associated with each tournament.
- If the tournament page uses location-related grouping, preserve that structure in a user-friendly way.
- Keep the location presentation aligned with the catalog page design language.

### 5. Search and filtering behavior

- Allow users to browse tournaments by at least the included fields available in the page data.
- Support filtering or grouping based on venue/location when available.
- Support basic date-based browsing or sort order so upcoming tournaments are easy to find.
- Keep the UI simple and aligned with the catalog page experience.

## Data Model Requirements

The tournament record should include the following minimum fields:

- title / name
- location or lane name
- entry price
- date
- source URL
- optional source identifier if available

The model should be designed to support future list and detail views without forcing a redesign.

## Non-Functional Requirements

- The page should remain easy to navigate and visually consistent with the existing catalog design.
- Tournament display should degrade gracefully when a field is missing.
- The app should not require live network access on every page view.
- Data imported from Tournament Bowl should be stored locally and reusable for display.

## Acceptance Criteria

1. Upcoming tournaments are successfully scraped and stored from the Tournament Bowl source.
2. Each tournament includes the lane name, entry price, and date.
3. Tournament entries are not duplicated when the import is rerun.
4. A tournament listing page is available and uses a layout consistent with the catalog page design.
5. A location-focused view/tab is included and styled to match the catalog page patterns.
6. Missing or incomplete data does not prevent valid tournament records from appearing.
7. The page provides a clear and usable summary of future tournaments.

## Agent Prompt

Implement the Iteration 4 tournament feature by adding a tournament import flow from Tournament Bowl, storing the required fields, and presenting the results in a location-aware listing that matches the design of the catalog page. Use a controlled, repeatable import process, make the importer idempotent, and ensure the page displays lane name, entry price, and date clearly. Keep the implementation focused on this feature and do not broaden scope beyond the tournament list and location view.
