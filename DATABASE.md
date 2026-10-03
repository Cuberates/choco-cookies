# PostgreSQL configuration

The application requires PostgreSQL connection settings at startup. It reads:

- `SPRING_DATASOURCE_URL` — JDBC URL, for example `jdbc:postgresql://<host>:5432/<database>`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

For local Docker runs, copy `.env.example` to `.env`, fill in the values, then run `docker compose up --build`. Compose passes `.env` into the application container. Spring Boot does not load a plain `.env` file when run directly; for a local Maven run, export the variables in the shell first.

In Render, set the same variables in the app service's environment settings. Use the database's internal hostname only when the app service and database are on the same Render private network. For local development outside that network, use a separately provisioned database connection that is reachable from the development machine.

Keep `.env` and real credentials out of version control. `.env.example` contains placeholders only.
