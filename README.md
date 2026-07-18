# shopping-plugin
Plugin automatic search price to things 

## Setup

After cloning, enable the local git hooks (commit message grammar check —
see `docs/DECISIONS.md` ADR-004 and `docs/CONVENTIONS.md`):

```
git config core.hooksPath .githooks
```

This is a per-clone setting (git never runs repo-tracked hooks automatically
for security reasons), so it must be run again on every fresh clone/machine.

### Backend: local Postgres

The backend (`backend/`) expects a local Postgres, provided via Docker Compose:

```
cp .env.example .env
docker compose up -d
```

Then run the backend as usual (e.g. `mvn spring-boot:run` in `backend/`) — it
picks up the same credentials from `application.yml` defaults and applies
Flyway migrations from `backend/src/main/resources/db/migration` on startup.

`.env` is gitignored; `.env.example` documents the expected variables
(`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`) — see ADR-007 in `docs/DECISIONS.md`.
