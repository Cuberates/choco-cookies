# Iteration 2 specification

## Goal

Replace the temporary custom-name page with a searchable bowling ball catalog sourced from Bowwwl. Store catalog data in Render PostgreSQL and let users search by ball name and filter by weight, coverstock type, core type, and brand.

## 1. Configure PostgreSQL access

- Obtain the Render PostgreSQL connection values: internal host, port, database name, username, and password. Do not guess or commit credentials.
- Configure the app using environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` (plus any required driver or pool settings).
- Configure local runs and containers to pass environment variables explicitly; Spring Boot does not load a plain `.env` file by itself.
- Pass the database variables through Render's service environment configuration. The internal database host is for services on the matching private network; local runs outside that network need a separate reachable development database/connection.
- Ensure `.env` is ignored by Git and keep a `.env.example` with empty or clearly fake values for setup documentation.
- Validate required database settings at startup and fail clearly if they are missing or invalid. Never log passwords or commit real credentials.

## 2. Remove the temporary name endpoint

- Delete `NameController` and its `name.html` template.
- Keep Thymeleaf for the catalog page unless the UI is changed to another rendering approach.
- Replace dependencies used only by the temporary page if they are no longer needed; retain Web, Thymeleaf, JPA, PostgreSQL, and test support for the catalog.

## 3. Inspect and import the Bowwwl catalog

- Inspect `https://www.bowwwl.com/bowling-ball-database` and identify the catalog fields, pagination, and page structure before implementing extraction.
- Confirm the site's access rules and use a conservative request rate. Avoid scraping on every application startup or every user request.
- Expose import as a deliberate admin/on-demand operation only. It must not run during normal application startup or in response to catalog browsing.
- Map each ball to a stable local record with at least: name, brand, weight, coverstock type, and core type. Preserve its source URL and source identifier when available.
- Define a deterministic canonical source key for each ball: use the source's stable ID when available, otherwise use a normalized source URL. Enforce uniqueness on this key and use it to make reruns update existing records rather than create duplicates.
- Inspect the source data and decide whether weight is one numeric value, a range, or multiple values per ball before finalizing the database model and filter behavior. Preserve the source meaning; do not silently collapse ranges or sets to one number.
- Represent missing source values consistently as null. Keep usable records with missing optional attributes, exclude nulls from filter choices, and display a clear unknown/unspecified value where appropriate.
- Report import counts for inserted records, updated records, skipped records, and malformed records. Include enough error detail to diagnose malformed input without exposing secrets.
- Keep parsing separate from network fetching so parser behavior can be validated against saved fixtures.
- Store the imported records in PostgreSQL through JPA entities and a repository. Use schema migrations for database changes rather than relying on automatic production schema creation.

## 4. Build catalog search and filters

- Add a catalog route that displays a paginated list of bowling balls. 
- Support a name search and optional filters for weight, coverstock type, core type, and brand. Allow filters to be combined and preserve selected values in the page controls.
- Use case-insensitive partial matching for names and exact matching for categorical filters unless product requirements specify otherwise.
- Add sorting and pagination defaults so large result sets remain usable.
- Show a clear empty-results state and provide a way to clear filters.

## 5. Verify the complete flow

- Verify database connectivity with valid configuration and confirm startup fails clearly when required database settings are missing or invalid.
- Run the importer against a small fixture/sample first; confirm inserted, updated, skipped, and malformed counts and verify repeated imports do not duplicate records.
- Verify name search and each individual filter, combinations of filters, pagination, and no-result behavior.
- Add automated tests for repository filtering, catalog routes, and importer parsing using version-controlled HTML/data fixtures. Tests must never fetch or depend on the live Bowwwl site.
- Document local setup, database environment variables, how to run an import, and deployment configuration. Keep credentials out of documentation and source control.

## Inputs to resolve before implementation

- Render database credentials and confirmation that the app service is attached to the database's private network are not included in `task_2.md`; provide these through secure environment configuration before connecting the deployed app.
- The Bowwwl source must be inspected to determine whether each ball has one weight, a range, or multiple weight values. This determines the persisted representation and exact filter semantics.
