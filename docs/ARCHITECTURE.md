# Architektura — shopping-plugin

Rozszerzenie przeglądarki (Manifest V3) w TypeScript, które na stronie sklepu
wykrywa produkt i wyszukuje jego najniższą cenę w innych źródłach.

## Widok z lotu ptaka

```
┌─────────────────────────────────────────────────────────────┐
│                     Przeglądarka użytkownika                  │
│                                                              │
│  ┌──────────────┐   msg   ┌──────────────────────────────┐  │
│  │ Content       │◀──────▶│ Service Worker (background)    │  │
│  │ Script        │        │  - orkiestracja zapytań        │  │
│  │ - wykrycie    │        │  - cache wyników               │  │
│  │   produktu    │        │  - wywołania PriceProvider'ów  │  │
│  │ - wstrzyknięcie│       └───────────────┬──────────────┘  │
│  │   UI (badge)  │                        │                  │
│  └──────────────┘                         ▼                  │
│  ┌──────────────┐              ┌────────────────────────┐    │
│  │ Popup (UI)   │◀────────────▶│  Core (logika domenowa) │    │
│  │ - lista cen  │              │  - matching produktów   │    │
│  │ - ustawienia │              │  - PriceProvider (iface)│    │
│  └──────────────┘              └────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                                          │
                                          ▼  (fetch)
                            Zewnętrzne źródła cen / API
```

## Komponenty

### 1. Content Script (`src/content/`)
- Uruchamiany na stronach sklepów.
- **Ekstrakcja produktu**: tytuł, cena, EAN/GTIN, obrazek — przez adaptery per
  sklep + heurystyki (schema.org / JSON-LD `Product`, Open Graph).
- Wstrzykuje lekki UI (badge/overlay) informujący, że znaleziono tańszą ofertę.
- Komunikuje się z service workerem przez `chrome.runtime.sendMessage`.

### 2. Service Worker (`src/background/`)
- Bezstanowy z natury MV3 → stan trwały w `chrome.storage`.
- Odbiera „znaleziono produkt X”, odpytuje `PriceProvider`'y, agreguje i cache'uje.
- Zarządza throttlingiem/kolejką zapytań, żeby nie zalewać źródeł.

### 3. Popup / Options UI (`src/ui/`)
- Popup: lista znalezionych ofert dla bieżącego produktu, sortowanie po cenie.
- Options: konfiguracja (które źródła, waluta, próg oszczędności do alertu).

### 4. Core / domena (`src/core/`)
- **`PriceProvider`** — interfejs źródła cen:
  ```ts
  interface PriceProvider {
    readonly id: string;
    search(query: ProductQuery): Promise<PriceResult[]>;
  }
  ```
- **Product matching** — dopasowanie ofert do produktu (po EAN gdy jest, inaczej
  fuzzy po nazwie + normalizacja).
- Czyste funkcje, w pełni testowalne, bez zależności od API przeglądarki.

### 5. Shared (`src/shared/`)
- Typy (`ProductQuery`, `PriceResult`, wiadomości między kontekstami), stałe,
  warstwa logowania, helpery `chrome.storage`.

## Proponowana struktura katalogów

```
shopping-plugin/
├── CLAUDE.md
├── docs/
│   ├── ARCHITECTURE.md
│   ├── CONVENTIONS.md
│   ├── TODO.md
│   └── DECISIONS.md
├── public/
│   ├── manifest.json
│   └── icons/
├── src/
│   ├── background/      # service worker
│   ├── content/         # content script + adaptery sklepów
│   │   └── extractors/  # per-sklep i generyczne (JSON-LD, OG)
│   ├── ui/
│   │   ├── popup/
│   │   └── options/
│   ├── core/
│   │   ├── providers/   # implementacje PriceProvider
│   │   └── matching/
│   └── shared/
├── tests/
├── package.json
├── tsconfig.json
└── (bundler config: vite / esbuild / webpack)
```

## Przepływ danych (happy path)

1. Użytkownik otwiera stronę produktu → **content script** ekstrahuje `ProductQuery`.
2. Content script → `sendMessage` → **service worker**.
3. Service worker sprawdza **cache** (`chrome.storage`); jeśli brak — odpytuje
   wszystkie aktywne **`PriceProvider`** równolegle.
4. **Core matching** filtruje i sortuje `PriceResult[]`.
5. Wynik wraca do content scriptu (badge) i jest dostępny w popupie.

## Kluczowe decyzje do podjęcia (patrz DECISIONS.md)

- Bundler: Vite vs esbuild vs webpack.
- Źródła cen: własny scraping vs publiczne API porównywarek (koszt, legalność, ToS).
- Framework UI popupu: vanilla/lit vs React (waga bundla).
- Strategia matchingu produktów bez EAN.

## Zasady bezpieczeństwa i uprawnień

- Minimalne `permissions` i `host_permissions`; docelowo tylko domeny sklepów,
  nie `<all_urls>`.
- Brak wysyłki danych o użytkowniku na zewnątrz bez zgody i wpisu w ADR.
- Content Security Policy zgodne z MV3 (bez `eval`, bez zdalnego kodu).
