# Bowler's Journal
Bowler's Journal is a unified platform for bowlers to manage equipment, performance, and league information in one place.

## Accounts and journal

Open `/register` to create an account, then sign in at `/login` and open `/journal`. Add, edit, and delete league sessions with their date, league name, alley, location, games played, total pin-fall, notes, and up to six balls from the imported catalog. Averages are calculated automatically; the journal shows weekly pin-fall and weighted averages alongside an accessible scores table.

Catalog, tournament, and location browsing remain public. Journals are private to their account. Passwords use BCrypt hashes, authentication uses Spring Security, and sessions persist through Spring Session JDBC with a 30-minute inactivity timeout. The browser-local arsenal remains separate from journal ball selections.

Flyway migration `V3__create_accounts_and_journal.sql` creates the account, journal, ball association, and JDBC session tables automatically on startup. Configure the existing PostgreSQL connection as described in [DATABASE.md](DATABASE.md). For HTTPS deployments, set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` and configure trusted proxy forwarding if TLS terminates at a proxy.

The [iteration 5 specification](todo/specification_iteration_5.md) documents the behavior and validation rules. Run `./mvnw test` for automated checks; the optional PostgreSQL catalog persistence test requires its dedicated test database variables.

## Inspiration
Bowlers across the US and Canada often use multiple fragmented platforms to track league results, equipment specs, and ball data. Bowler's Journal brings those pieces together into a single, centralized hub so bowlers can access everything they need without jumping between tools.

## What It Does
Bowler's Journal helps users:
- Track their bowling equipment arsenal
- Log technical specifications such as RG, differential, and surface
- Monitor league scores and performance trends
- View equipment and performance data in one dashboard

## How We Built It
### Frontend
A responsive user interface designed for quick access both at home and at the lanes.

### Backend
A structured backend and database designed to connect complex equipment specifications with performance data in a consistent way.

## Challenges We Ran Into
- Consolidating disparate data types, from numerical equipment specs to scoring logs, into a clean schema
- Designing an interface that is fast, intuitive, and practical for active league bowlers

## Accomplishments
- Built the foundation for a centralized ecosystem that combines equipment tracking and performance monitoring
- Created a platform structure that can scale toward a fuller bowlers' data experience

## What We Learned
A frictionless data entry experience is critical for adoption, especially in a community that is used to traditional tracking methods and manual record-keeping.

## What's Next for Bowler's Journal
### Advanced Analytics
- Smart recommendations based on historical performance
- Insights tied to lane conditions and recent trends

### Community Features
- Arsenal sharing
- Direct pro shop integration
- Stronger social and collaborative functionality
