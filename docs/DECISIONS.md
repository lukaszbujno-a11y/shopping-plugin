# Log decyzji architektonicznych (ADR)

> Każda trwała decyzja techniczna = jeden wpis. Format lekki. Nie usuwaj wpisów —
> jeśli decyzja się zmienia, dodaj nowy wpis ze statusem „Zastępuje ADR-XXX”.

Szablon:

```
## ADR-XXX: <tytuł>
- Data: RRRR-MM-DD
- Status: Zaproponowane | Zaakceptowane | Zastąpione przez ADR-YYY
- Kontekst: co i dlaczego trzeba było rozstrzygnąć
- Decyzja: co wybrano
- Konsekwencje: skutki, kompromisy, co to blokuje/otwiera
```

---

## ADR-001: Manifest V3 + TypeScript
- Data: 2026-07-18
- Status: Zaakceptowane
- Kontekst: Projekt to rozszerzenie przeglądarki; MV2 jest wygaszane przez Chrome.
- Decyzja: Manifest V3, kod w TypeScript (strict), service worker zamiast
  persistent background page.
- Konsekwencje: Brak trwałego stanu w tle → stan w `chrome.storage`. Zgodność
  z przyszłymi wymogami Chrome Web Store.

## ADR-002: (do uzupełnienia) Wybór bundlera
- Status: Zaproponowane
- Kontekst: Potrzebny bundler dla wielu entry-pointów (content, background, popup, options).
- Opcje: Vite (DX, HMR), esbuild (szybkość, prostota), webpack (dojrzałość).
- Decyzja: —

## ADR-003: (do uzupełnienia) Źródła cen
- Status: Zaproponowane
- Kontekst: Skąd pobieramy ceny — własny scraping vs publiczne API porównywarek.
  Wpływ na legalność (ToS sklepów), koszt, niezawodność.
- Decyzja: —

## ADR-004: Sprawdzanie gramatyki commit message przez LanguageTool API
- Data: 2026-07-18
- Status: Zaakceptowane
- Kontekst: Autor uczy się angielskiego i chce, żeby commity były poprawne
  gramatycznie, z wyjaśnieniem błędów (nie tylko detekcja PL/EN). Lokalny
  git hook `commit-msg` (`.githooks/commit-msg`) wysyła treść commit message
  do publicznego API `api.languagetool.org` i wypisuje sugestie w terminalu.
  To jedyne miejsce w projekcie, gdzie jakikolwiek tekst opuszcza maszynę bez
  udziału użytkownika strony sklepu — dotyczy to wyłącznie treści commitów
  (dev-tooling), nie danych z rozszerzenia/stron sklepów.
- Decyzja: Używamy publicznego, darmowego API LanguageTool zamiast
  self-hosted (Docker) dla prostoty setupu. Hook domyślnie **blokuje**
  commit przy znalezionym błędzie (`exit 1`) — wersja ostrzegawcza
  (pierwotna decyzja) okazała się bezużyteczna w praktyce: output trafiał
  do kanału Output → Git w VSCode i ginął niezauważony. Zejście do trybu
  ostrzegawczego dla pojedynczego commita: `COMMIT_MSG_STRICT=0`. Hook
  aktywuje się lokalnie przez `git config core.hooksPath .githooks` (nie
  jest globalny ani wymuszony przez CI).
- Konsekwencje: Treść commit message (nie kod, nie dane ze stron sklepów)
  jest wysyłana do zewnętrznego serwisu przy każdym commicie na maszynach,
  które włączyły ten hook. Wymaga sieci — offline hook po prostu pomija
  sprawdzanie (fail-open). Jeśli w przyszłości prywatność treści commitów
  stanie się problemem, można przejść na self-hosted LanguageTool (Docker)
  bez zmiany interfejsu hooka.

## ADR-005: Backend (Spring Boot + Postgres) + serwis analiz (Python)
- Data: 2026-07-18
- Status: Zaakceptowane
- Kontekst: Pierwotny model (ADR-001, ARCHITECTURE.md) zakładał rozszerzenie
  w pełni client-side, bez backendu. Chcemy jednak: śledzenie produktu w
  czasie z cyklicznym odświeżaniem ceny, wyszukiwanie alternatyw i generowanie
  podsumowania plusów/minusów — to wymaga trwałego stanu po stronie serwera
  i mocy obliczeniowej nieadekwatnej dla service workera. Dodatkowo autor
  uczy się Spring Boota i chce go zastosować praktycznie w tym projekcie —
  to również ważny czynnik przy wyborze stacku backendu (nie tylko czysto
  techniczny).
- Decyzja:
  - **Backend**: Spring Boot + PostgreSQL, REST API. Odpowiada za: przyjęcie
    zgłoszenia śledzenia produktu, trwałość danych, harmonogram odświeżania
    cen (`@Scheduled`, np. co ~6h) przez warstwę `PriceSourceClient` —
    serwerowy odpowiednik `PriceProvider` z rozszerzenia (interfejs + jedna
    implementacja per sklep, dodawana bez zmian w reszcie systemu).
  - **AI service**: osobny proces Python (FastAPI). Odpowiada za wyszukiwanie
    alternatyw i generowanie plusów/minusów (patrz ADR-006). Komunikacja
    backend → AI service przez wersjonowany kontrakt REST (`/v1/analyze`),
    żeby oba serwisy mogły ewoluować niezależnie.
  - **Śledzenie jest anonimowe, per-instalacja rozszerzenia** — `install_id`
    generowany lokalnie w `chrome.storage`, wysyłany z każdym requestem;
    brak logowania/kont użytkowników.
  - **Rozszerzenie staje się cienkim klientem**: ekstrakcja produktu ze
    strony (bez zmian) + wywołania API backendu + wyświetlanie wyniku.
    Lokalny `PriceProvider`/matching opisany w ARCHITECTURE.md/ADR-001 nie
    jest już rozwijany po stronie klienta — tę odpowiedzialność przejmuje
    backend.
  - **Struktura repo**: monorepo, top-level `extension/`, `backend/`,
    `ai-service/`, `docs/` (dotychczasowa zawartość `src/` z ARCHITECTURE.md
    przenosi się pod `extension/`).
- Konsekwencje: Dane o śledzonych produktach (URL, tytuł, EAN) trwale
  opuszczają maszynę użytkownika i są przechowywane w Postgres na serwerze —
  uchyla to wcześniejsze założenie „zero danych na zewnątrz” i wymaga jasnej
  informacji dla użytkownika w UI rozszerzenia o tym, co i dlaczego zbieramy.
  Utrzymanie trzech stacków (TypeScript/Java/Python) zamiast jednego to
  świadomy koszt, akceptowalny częściowo dlatego, że backend służy też jako
  nauka Spring Boota. Wymaga hostingu serwera i bazy (koszt operacyjny),
  którego wcześniej nie było. Otwiera drogę do pozycji z Backlogu (historia
  cen, alerty o spadku ceny) bez dalszej przebudowy architektury, bo dane są
  już trwałe po stronie serwera.

## ADR-006: Plusy/minusy produktu generowane przez zewnętrzny LLM
- Data: 2026-07-18
- Status: Zaakceptowane
- Kontekst: Po zebraniu ofert cenowych (ADR-005) trzeba wygenerować dla
  użytkownika czytelne podsumowanie plusów/minusów produktu. Alternatywa —
  ekstrakcja z realnych recenzji przez scraping + własny NLP — jest trudniejsza
  technicznie i ryzykowna prawnie (ToS sklepów/porównywarek).
- Decyzja: AI service (Python) woła zewnętrzny model LLM, przekazując treść
  specyfikacji/opisu produktu (dane produktowe, nie dane osobowe użytkownika)
  i zwraca ustrukturyzowane podsumowanie zapisywane w
  `analysis_results.pros_cons`.
- Konsekwencje: Treść produktu (nie dane użytkownika) opuszcza infrastrukturę
  projektu i trafia do zewnętrznego dostawcy AI przy każdej analizie — kolejny,
  świadomy punkt wycieku danych obok samego backendu (ADR-005), analogiczny
  w duchu do ADR-004 (LanguageTool), ale dla ścieżki produktowej, nie
  dev-tooling. Koszt per-request zależny od dostawcy/modelu — do
  monitorowania przy skalowaniu. Jeśli w przyszłości niezależność od
  zewnętrznego dostawcy stanie się priorytetem, można rozważyć self-hosted
  model bez zmiany kontraktu `/v1/analyze`.

## ADR-007: Migracje schematu Postgres przez Flyway (nie Liquibase)

- Data: 2026-07-18
- Status: Zaakceptowane
- Kontekst: ADR-005 wymaga trwałej migracji schematu Postgres w backendzie.
  Do wyboru: Flyway (proste migracje SQL, kolejność po numerze wersji) vs
  Liquibase (changelogi XML/YAML/JSON, więcej funkcji jak rollback per
  changeset, ale wyższy próg wejścia).
- Decyzja: Flyway. Migracje jako zwykłe pliki SQL
  (`backend/src/main/resources/db/migration/V{n}__opis.sql`) — najprostszy
  model mentalny dla osoby uczącej się Spring Boota (patrz ADR-005), bez
  dodatkowego DSL-a do nauki. Konfiguracja lokalna: Postgres przez
  `docker-compose.yml` w katalogu głównym repo, dane logowania przez `.env`
  (gitignored, wzorzec w `.env.example`) — zgodnie z zasadą „bez sekretów w
  repo” z `CLAUDE.md`. `application.yml` ma domyślne wartości
  (`shopping_plugin`/`shopping_plugin`) spójne z `.env.example`, więc backend
  łączy się od razu bez eksportowania zmiennych ręcznie.
- Konsekwencje: Zależność `org.flywaydb:flyway-core` sama w sobie **nie
  wystarcza** w Spring Boot 4 — autokonfiguracja Flyway przeniosła się do
  osobnego modułu `org.springframework.boot:spring-boot-flyway`, który trzeba
  dodać jawnie (bez niego Flyway milczy — nie loguje nic i nie tworzy
  `flyway_schema_history`, bez błędu). Ten moduł jest już dodany w
  `backend/pom.xml`. Rollback pojedynczej migracji wymaga ręcznego SQL-a
  (Flyway Community nie ma automatycznego rollbacku) — akceptowalne przy
  obecnej skali projektu.
