# Specification: Iteration 5 — Accounts and Bowling Journal

## Source and scope

Implements `requirements_interation_5.md`. The source's iteration 4 headings are treated as iteration 5. Existing public catalog, tournament, and location browsing remain available. The neutral colors, shared bowling-ball icon, reduced corner radii, and permanently expanded catalog filters are preserved.

## Major changes

1. Replace the placeholder sign-in action with registration, login, and POST logout using Spring Security.
2. Persist users and adaptive password hashes; persist authenticated sessions through Spring Session JDBC in the existing database.
3. Add an authenticated `/journal` page with create, read, update, and delete operations scoped to the signed-in user.
4. Link each journal entry to zero through six distinct existing catalog balls, displayed inside its card.
5. Aggregate journal entries into weekly total pin-fall and weighted average charts, plus overall totals and average.

## Accounts and sessions

- Register with a unique case-insensitive username (3–40 ASCII letters, digits, underscores, or hyphens) and a password of 8–72 UTF-8 bytes. No default or seeded credentials.
- Store BCrypt password hashes, never plaintext passwords. Password validation must not truncate input.
- Login failures use a generic message. Successful login opens the journal; logout invalidates the session.
- Spring Security handles session fixation protection and CSRF checks for all writes. Thymeleaf POST forms include CSRF tokens.
- JDBC session tables are managed by Flyway, including expiry and principal indexes. Sessions expire after 30 minutes of inactivity; cookies are HttpOnly and SameSite=Lax. HTTPS deployments must enable secure cookies.
- Private endpoints require authentication. Ownership checks on reads, edits, and deletions return 404 for another user's entry.

## Journal fields and validation

- Required: bowling date, league name, bowling alley name, location, games played, and total pin-fall.
- Optional: notes and up to six ball selections from the imported catalog.
- Date must be January 1, 1900 through today, bounding chart history. Games played must be 1–100. Pin-fall must be between zero and 300 × games played.
- Names/location are trimmed, nonblank, and at most 160 characters; notes are at most 2,000 characters.
- Average is calculated server-side as pin-fall ÷ games played, displayed to two decimal places. The user cannot supply it independently.
- Reject duplicate, unknown, or more than six ball IDs before saving. Display ball brand/name on each entry; choosing a ball never triggers scraping.
- Render escaped text. Invalid forms retain non-sensitive user input and show actionable validation messages without changing stored records.
- Successful writes redirect to the journal to avoid duplicate submissions on refresh. Edit forms reuse the entry form; cards expose edit and delete controls.

## Analytics

- Group by the bowling date's Monday-starting ISO week, independent of creation time.
- Weekly pins = sum of pin-fall; weekly average = sum of pin-fall ÷ sum of games. Overall average uses the same weighted calculation.
- Insert zero-pin/no-average weeks between the first and last recorded weeks. Do not plot no-game averages as zero scores.
- Use responsive SVG charts and an accessible data table, without a chart CDN. All saved history contributes; horizontal scrolling accommodates long histories.
- Empty journals show an onboarding state, zero totals, and no misleading average.
- Create, update, and delete recompute displayed aggregates from persisted entries.

## Data and routes

- `app_user`: ID, normalized unique username, password hash.
- `journal_entry`: ID, owner FK, date, league, alley, location, games, pins, notes; index on owner/date.
- `journal_entry_ball`: entry and catalog ball FKs with a unique pair. Deleting entries removes associations.
- Spring Session's `spring_session` and `spring_session_attributes` schema uses PostgreSQL bytea (compatible with test H2 PostgreSQL mode).
- Public: GET/POST `/register`, GET/POST `/login`, existing catalog and event routes, static assets.
- Private: GET `/journal`, GET `/journal/new`, POST `/journal`, GET `/journal/{id}/edit`, POST `/journal/{id}`, POST `/journal/{id}/delete`, POST `/logout`.

## Acceptance and verification

- Accounts register with hashed passwords, log in, retain their authentication through a JDBC session, and log out.
- Anonymous requests cannot access journals; users cannot read or mutate another user's entries.
- CRUD persists valid data and selected catalog balls. Invalid scores, dates, and ball selections are rejected.
- Weekly grouping handles year boundaries and weighted averages; editing/deleting changes charts and totals.
- Desktop/mobile navigation provides journal and authentication access while keeping public pages usable.
- Automated integration tests exercise the actual security/session filter chain, CSRF, registration/login/logout, ownership, validation, CRUD, and charts. Run the existing regression suite as well.

## Framework references

- [Spring Security form login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html)
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html)

Password reset, email verification, individual-game scoring, recommendations, and sharing are outside this iteration.
