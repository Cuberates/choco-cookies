# Iteration 5 — Implementation and Major Change Review

## Delivered

The source is `requirements_interation_5.md`, including its authentication and journal requirements despite the iteration 4 headings. The agreed implementation behavior is documented in `specification_iteration_5.md`.

| Area | Previous behavior | Iteration 5 behavior |
| --- | --- | --- |
| Accounts | Placeholder sign-in dialog | Register, sign in with a BCrypt password hash, and POST sign out |
| Sessions | No authenticated persistence | Spring Session JDBC in the existing database; 30-minute inactivity timeout |
| Journal | No journal routes or records | Private entry list, create form, edit form, and deletion |
| Equipment | Browser-local saved arsenal | Journal entries can additionally associate up to six distinct imported catalog balls |
| Scores | No persisted performance history | Date, league, alley, location, games, pins, notes, and calculated average |
| Analytics | No journal graphs | Weekly pin-fall, weighted weekly average, overall totals, and accessible weekly table |
| Navigation | Catalog and tournaments | Journal link and functional authentication actions on desktop and mobile |

## Review decisions

- Authentication uses Spring Security's form-login filter rather than custom credential/session handling. Usernames are normalized for registration and login. Passwords are limited to BCrypt's 72-byte boundary without truncation.
- CSRF remains enabled. POST forms use Thymeleaf action processing to render tokens. Logout is a POST operation.
- Session cookies explicitly use HttpOnly and SameSite=Lax, and retain HTTPS-aware secure-cookie behavior unless configured explicitly. Flyway owns the session tables rather than repeated startup initialization.
- Every entry access, update, and delete queries by both entry ID and current account ID. Invalid ownership returns 404. Owners and calculated averages are not form-bindable fields.
- Entry validation rejects future/too-old dates, blank locations/names, invalid game counts, impossible pin-fall, duplicate balls, nonexistent balls, and more than six selections. Pin-fall constraints also exist in the database.
- The average is weighted by games, not the mean of entry averages. Weeks begin Monday and can span calendar years. Empty weeks have zero pins and no plotted average; the average graph does not connect across gaps.
- Charts use local SVG rendering with a text/table alternative, preserving the neutral palette and avoiding third-party chart services. Ball images/icons and previous catalog filters remain intact.
- The existing browser-local arsenal is preserved separately. Journal equipment links reference actual catalog records, not the editorial featured cards.
- Migrations add new tables without altering existing catalog/tournament rows. No real database was migrated during this implementation.

## Validation

`./mvnw test`: **30 passed, 1 skipped, 0 failures, 0 errors**.

Seven new integration tests exercise the Spring Security and JDBC session filter chain, registration hashing and validation, case-insensitive login, session rotation and stored security context, cookie attributes, logout, public/anonymous access, CSRF rejection, entry CRUD, equipment persistence/removal, escaped notes, ownership isolation, invalid inputs, ISO week grouping, empty weeks, and weighted averages. Existing catalog and tournament regression tests also pass. The application context test now uses an isolated H2 database rather than local deployment credentials.

`node --check src/main/resources/static/js/catalog.js` and `git diff --check` pass.

The optional PostgreSQL catalog persistence test was skipped because its dedicated database variables are not configured. Migration and session integration were verified in H2 PostgreSQL mode; a live PostgreSQL deployment and visual browser checks were not performed.

## Use and deployment

Restart the application to load the new dependencies and apply Flyway V3. Open `/register`, create an account, sign in, and use `/journal`.

The existing PostgreSQL datasource configuration is reused. HTTPS deployments should set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` and configure trusted proxy forwarding where needed. No initial passwords or seeded accounts are provided.
