# Architektura — shopping-plugin

Rozszerzenie przeglądarki (Manifest V3) w TypeScript, które na stronie sklepu
wykrywa produkt i pozwala śledzić jego cenę. Śledzenie, skanowanie źródeł cen
i analiza (alternatywy, plusy/minusy) dzieją się po stronie backendu —
patrz [ADR-005](DECISIONS.md#adr-005-backend-spring-boot--postgres--serwis-analiz-python)
i [ADR-006](DECISIONS.md#adr-006-plusy-minusy-produktu-generowane-przez-zewnętrzny-llm).

> Historia: pierwotna wersja tego dokumentu opisywała rozszerzenie w pełni
> client-side (bez backendu) — ten model zastąpiono ADR-005. Sekcje poniżej
> opisują aktualny, docelowy stan systemu.

## Widok z lotu ptaka

```
┌───────────────────────────────────────────┐
│           Przeglądarka użytkownika          │
│                                             │
│  ┌──────────────┐   msg   ┌─────────────┐  │
│  │ Content       │◀──────▶│ Service       │  │
│  │ Script        │        │ Worker        │  │
│  │ - wykrycie    │        │ - klient API  │  │
│  │   produktu    │        │   backendu    │  │
│  │ - wstrzyknięcie│       │ - cache lokalny│  │
│  │   UI (badge)  │        └───────┬───────┘  │
│  └──────────────┘                │          │
│  ┌──────────────┐                │          │
│  │ Popup (UI)   │◀───────────────┘          │
│  │ - status/wynik│                          │
│  └──────────────┘                           │
└───────────────────┬─────────────────────────┘
                     │  REST (install_id, ProductQuery)
                     ▼
        ┌─────────────────────────────┐
        │   Backend (Spring Boot)     │
        │  - REST API (tracking)      │
        │  - Postgres (trwałość)      │
        │  - scheduler (rescan cen)   │
        │  - PriceSourceClient (iface)│
        └──────────┬───────────┬──────┘
                    │           │  REST /v1/analyze
     (fetch)        ▼           ▼
  Sklepy / źródła cen    ┌───────────────────┐
                         │ AI service (Python)│
                         │ - alternatywy       │
                         │ - plusy/minusy (LLM)│
                         └──────────┬─────────┘
                                    ▼ (fetch)
                          Zewnętrzny dostawca LLM
```

## Komponenty

### Rozszerzenie (`extension/`) — cienki klient

#### 1. Content Script (`extension/src/content/`)
- Uruchamiany na stronach sklepów.
- **Ekstrakcja produktu**: tytuł, cena, EAN/GTIN, obrazek — przez adaptery per
  sklep + heurystyki (schema.org / JSON-LD `Product`, Open Graph). Bez zmian
  względem pierwotnego modelu — ekstrakcja zostaje po stronie klienta.
- Wstrzykuje lekki UI (badge/overlay) z wynikiem śledzenia.
- Komunikuje się z service workerem przez `chrome.runtime.sendMessage`.

#### 2. Service Worker (`extension/src/background/`)
- Bezstanowy z natury MV3 → stan trwały w `chrome.storage`.
- Generuje i przechowuje `install_id` (losowy UUID) — jedyny identyfikator
  wysyłany do backendu, brak logowania/kont (ADR-005).
- Odbiera „znaleziono produkt X”, woła REST API backendu (dodanie do
  śledzenia, pobranie statusu/wyniku), cache'uje ostatni wynik lokalnie na
  wypadek szybkiego podglądu bez roundtripu do sieci.
- **Nie zawiera już logiki `PriceProvider`/matchingu** — to przeniosło się do
  backendu (ADR-005). `src/core/` w rozszerzeniu ogranicza się do typów i
  ewentualnych czystych helperów UI.

#### 3. Popup / Options UI (`extension/src/ui/`)
- Popup: status śledzenia bieżącego produktu — cena, alternatywy, plusy/minusy
  (dane z backendu), sortowanie po cenie.
- Options: konfiguracja (waluta, próg oszczędności do alertu).

#### 4. Shared (`extension/src/shared/`)
- Typy współdzielone z kontraktem API backendu (`ProductQuery`, `PriceResult`,
  `AnalysisResult`), wiadomości między kontekstami, warstwa logowania, helpery
  `chrome.storage`.

### Backend (`backend/`) — Spring Boot + Postgres

- **REST API**: przyjmuje zgłoszenia śledzenia produktu (`install_id` +
  `ProductQuery`), zwraca status/wynik śledzenia.
- **Persystencja (Postgres)**: `tracked_products`, `price_offers`,
  `analysis_results` — patrz szkic schematu niżej.
- **Scheduler** (`@Scheduled`, np. co ~6h): iteruje po śledzonych produktach i
  odpala skan cen z throttlingiem per źródło.
- **`PriceSourceClient`** — interfejs źródła cen, serwerowy odpowiednik
  pierwotnego `PriceProvider` z rozszerzenia:
  ```java
  public interface PriceSourceClient {
      String getSourceId();
      List<PriceOffer> search(ProductQuery query);
  }
  ```
  Każdy sklep = jedna implementacja (`@Component`), zebrane przez Springa
  jako `List<PriceSourceClient>` — dodanie nowego źródła nie wymaga zmian
  w orkiestracji (Open/Closed principle, ta sama filozofia co ADR-001).
- Woła AI service przez `/v1/analyze`, zapisuje wynik w `analysis_results`.

### AI service (`ai-service/`) — Python (FastAPI)

- Wystawia wersjonowany endpoint `/v1/analyze` (produkt + zebrane oferty →
  alternatywy + plusy/minusy), żeby backend i AI service mogły ewoluować
  niezależnie.
- Wewnętrznie: pipeline niezależnych analizatorów za wspólnym interfejsem/
  protokołem (np. `AlternativeFinder`, `ProsConsGenerator`) — dodanie nowego
  typu analizy to nowy analizator + rejestracja w pipeline, bez przepisywania
  orkiestracji.
- `ProsConsGenerator` woła zewnętrzny model LLM z treścią specyfikacji/opisu
  produktu (ADR-006) — to jedyne miejsce, gdzie treść produktu trafia do
  zewnętrznego dostawcy AI.

### Szkic schematu Postgres

```
tracked_products(id, owner_id /* install_id */, url, title, ean,
                  canonical_key, created_at)
price_offers(id, product_id FK, source, price, currency, url, scanned_at)
analysis_results(id, product_id FK, alternatives JSONB, pros_cons JSONB,
                  generated_at)
```
`alternatives`/`pros_cons` jako JSONB (nie sztywne kolumny) — AI service może
rozszerzać kształt wyniku bez migracji schematu za każdym razem; reszta
danych (ceny, produkty) zostaje relacyjna, bo są odpytywane/agregowane.

## Proponowana struktura katalogów (monorepo)

```
shopping-plugin/
├── CLAUDE.md
├── docs/
│   ├── ARCHITECTURE.md
│   ├── CONVENTIONS.md
│   ├── TODO.md
│   └── DECISIONS.md
├── extension/            # Manifest V3, TypeScript (dawny root)
│   ├── public/
│   │   ├── manifest.json
│   │   └── icons/
│   ├── src/
│   │   ├── background/  # service worker
│   │   ├── content/     # content script + adaptery sklepów
│   │   │   └── extractors/
│   │   ├── ui/
│   │   │   ├── popup/
│   │   │   └── options/
│   │   ├── core/        # typy, czyste helpery (bez PriceProvider)
│   │   └── shared/
│   ├── tests/
│   ├── package.json
│   ├── tsconfig.json
│   └── (bundler config: vite / esbuild / webpack)
├── backend/               # Spring Boot + Postgres (Maven)
│   ├── pom.xml
│   ├── src/main/java/com/shoppingplugin/backend/
│   │   ├── BackendApplication.java
│   │   ├── product/       # Product (entity), ProductRepository, ProductController, ProductService
│   │   ├── priceoffer/
│   │   │   ├── PriceOffer.java, PriceOfferRepository.java
│   │   │   ├── PriceSourceClient.java   # interfejs
│   │   │   └── impl/                    # per-sklep: AllegroPriceSourceClient itd.
│   │   ├── analysis/       # AnalysisResult, AnalysisResultRepository, AiServiceClient (/v1/analyze)
│   │   ├── scan/           # PriceScanScheduler (@Scheduled)
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/   # Flyway: V1__init.sql, V2__...
│   ├── src/test/java/...
│   └── Dockerfile
├── ai-service/              # Python (FastAPI)
│   ├── pyproject.toml
│   ├── app/
│   │   ├── main.py          # FastAPI app
│   │   ├── api/analyze.py   # router /v1/analyze, Pydantic schematy request/response
│   │   ├── analyzers/
│   │   │   ├── base.py      # wspólny interfejs analizatora
│   │   │   ├── alternatives.py  # AlternativeFinder
│   │   │   └── pros_cons.py     # ProsConsGenerator (wołanie LLM)
│   │   ├── llm/client.py    # wrapper na zewnętrznego dostawcę LLM
│   │   └── config.py
│   ├── tests/
│   └── Dockerfile
└── docker-compose.yml       # lokalny dev: Postgres + backend + ai-service
```

## Przepływ danych (happy path)

1. Użytkownik na stronie produktu klika „śledź” → **content script**
   ekstrahuje `ProductQuery`.
2. Content script → `sendMessage` → **service worker** → REST call do
   **backendu** (`install_id` + `ProductQuery`).
3. Backend zapisuje `tracked_products`, scheduler odpala **`PriceSourceClient`**
   równolegle dla aktywnych źródeł → zapis `price_offers`.
4. Backend woła **AI service** (`/v1/analyze`) → alternatywy + plusy/minusy
   (LLM) → zapis `analysis_results`.
5. Rozszerzenie odpytuje backend o status/wynik i wyświetla w popupie/badge;
   lokalny cache w `chrome.storage` na szybki podgląd bez roundtripu.

## Kluczowe decyzje do podjęcia (patrz DECISIONS.md)

- Bundler rozszerzenia: Vite vs esbuild vs webpack.
- Źródła cen: własny scraping vs publiczne API porównywarek (koszt,
  legalność, ToS) — implementowane jako `PriceSourceClient` w backendzie
  (ADR-005).
- Framework UI popupu: vanilla/lit vs React (waga bundla).
- Strategia matchingu produktów bez EAN (teraz: backend/AI service, nie
  rozszerzenie).
- Wybór dostawcy/modelu LLM dla `ProsConsGenerator` (ADR-006) — koszt,
  jakość, limity.

## Zasady bezpieczeństwa i uprawnień

- Minimalne `permissions` i `host_permissions` rozszerzenia; docelowo tylko
  domeny sklepów, nie `<all_urls>`.
- Śledzenie jest anonimowe (per-`install_id`), ale URL/tytuł/EAN śledzonego
  produktu trwale trafia do backendu i Postgres (ADR-005) — wymaga jasnej
  informacji dla użytkownika w UI rozszerzenia o tym, co i dlaczego zbieramy.
  To już nie jest model „zero danych na zewnątrz”.
- Treść produktu (nie dane osobowe) trafia dodatkowo do zewnętrznego
  dostawcy LLM przy generowaniu plusów/minusów (ADR-006).
- Content Security Policy rozszerzenia zgodne z MV3 (bez `eval`, bez
  zdalnego kodu).
