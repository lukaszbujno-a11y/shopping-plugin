# TODO / Roadmapa — shopping-plugin

> Zasada: to jest żywy dokument. Zaczynając zadanie zmień `[ ]` → `[~]` (w toku),
> po ukończeniu → `[x]`. Nowe pomysły dopisuj do Backlogu.

Legenda: `[ ]` do zrobienia · `[~]` w toku · `[x]` zrobione

## Milestone 0 — Fundament projektu
- [x] Commit-msg hook: sprawdzanie gramatyki EN przez LanguageTool (ADR-004)
- [ ] Inicjalizacja `package.json`, `tsconfig.json` (strict)
- [ ] Skrypt `prepare` w `package.json` (`git config core.hooksPath .githooks`),
      żeby `npm install` aktywował commit-msg hook automatycznie zamiast
      ręcznego kroku z README
- [ ] Wybór i konfiguracja bundlera (Vite/esbuild) → ADR
- [ ] ESLint + Prettier + skrypty `lint` / `typecheck` / `build`
- [ ] Szkielet `manifest.json` (MV3) + ikony placeholder
- [ ] Struktura katalogów wg [ARCHITECTURE.md](ARCHITECTURE.md)
- [ ] Setup testów (Vitest) + jeden test sanity

## Milestone 1 — Wykrywanie produktu
- [ ] Content script ładowany na stronie
- [ ] Generyczny extractor JSON-LD (`schema.org/Product`)
- [ ] Fallback: Open Graph / heurystyki DOM
- [ ] Typ `ProductQuery` w `shared/`
- [ ] Testy extractorów na fixture'ach

## Milestone 2 — Integracja rozszerzenia z backendem
> Rewizja: patrz [ADR-005](DECISIONS.md#adr-005-backend-spring-boot--postgres--serwis-analiz-python).
> Lokalny `PriceProvider`/matching w rozszerzeniu **nie jest już rozwijany** —
> tę odpowiedzialność przejmuje backend (Milestone 5–7 niżej). Rozszerzenie
> tylko woła API i wyświetla wynik.
- [ ] `install_id`: generowanie i przechowywanie w `chrome.storage`
- [ ] Klient API backendu w service workerze (dodanie produktu do śledzenia,
      odpytanie o status/wynik)
- [ ] Typy `ProductQuery`, `PriceResult`, `AnalysisResult` w `shared/`
      (współdzielone z kontraktem API backendu)
- [ ] Cache ostatniego wyniku w `chrome.storage` (szybki podgląd bez roundtripu)

## Milestone 3 — UI
- [ ] Badge/overlay wstrzykiwany przez content script
- [ ] Popup: status śledzenia, cena, alternatywy, plusy/minusy (z backendu)
- [ ] Strona opcji (waluta, próg alertu)

## Milestone 4 — Utwardzenie rozszerzenia
- [ ] Minimalizacja permissions w manifeście
- [ ] Obsługa błędów i stany puste w UI (backend niedostępny, brak wyniku)
- [ ] Build produkcyjny + paczka do Chrome Web Store / AMO

## Milestone 5 — Backend: fundament (Spring Boot + Postgres)
> Patrz ADR-005. Katalog `backend/`.
- [ ] Inicjalizacja projektu Spring Boot (Maven/Gradle) w `backend/`
- [ ] Konfiguracja Postgres (lokalnie: Docker Compose) + Flyway/Liquibase do migracji
- [ ] Encje/tabele: `tracked_products`, `price_offers`, `analysis_results`
- [ ] REST endpoint: zgłoszenie śledzenia produktu (`install_id` + `ProductQuery`)
- [ ] REST endpoint: status/wynik śledzenia dla rozszerzenia

## Milestone 6 — Backend: skanowanie cen
- [ ] Interfejs `PriceSourceClient` + pierwsza implementacja (mock/lokalna) do testów E2E
- [ ] Scheduler (`@Scheduled`) do cyklicznego odświeżania cen śledzonych produktów
- [ ] Throttling/kolejka zapytań per źródło
- [ ] Zapis wyników do `price_offers`

## Milestone 7 — AI service (Python): alternatywy i plusy/minusy
> Patrz ADR-006. Katalog `ai-service/`.
- [ ] Szkielet FastAPI + endpoint `/v1/analyze`
- [ ] `AlternativeFinder` (matching podobnych produktów)
- [ ] `ProsConsGenerator` — wywołanie zewnętrznego LLM na bazie specyfikacji/opisu
- [ ] Backend: integracja z `/v1/analyze`, zapis do `analysis_results`

## Backlog (niezaplanowane)
- [ ] Historia cen produktu
- [ ] Alerty o spadku ceny (teraz realne dzięki schedulerowi z Milestone 6)
- [ ] Wsparcie wielu walut / regionów
- [ ] Firefox (weryfikacja różnic MV3)
- [ ] Self-hosted LLM zamiast zewnętrznego dostawcy (patrz ADR-006, konsekwencje)
