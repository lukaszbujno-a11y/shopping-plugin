# Konwencje kodu — shopping-plugin

## Język i styl
- **TypeScript strict** (`strict: true`, `noUncheckedIndexedAccess: true`).
- Bez `any` (chyba że z komentarzem `// any: <powód>`).
- Preferuj `type`/`interface` dla kontraktów, unikaj `enum` — używaj union stringów.
- Funkcje czyste tam, gdzie to możliwe; efekty uboczne izolowane w warstwie API.

## Nazewnictwo
- Pliki: `kebab-case.ts` (np. `price-provider.ts`).
- Typy/interfejsy/klasy: `PascalCase`. Zmienne/funkcje: `camelCase`.
- Stałe modułowe: `UPPER_SNAKE_CASE`.
- Testy: `*.test.ts` obok kodu lub w `tests/`.

## Struktura importów
- Kolejność: zależności zewnętrzne → aliasy wewnętrzne (`@/…`) → względne.
- Bez importów z `background` do `ui` i odwrotnie — komunikacja tylko przez wiadomości/typy z `shared`.

## Komunikacja między kontekstami (content ↔ background ↔ ui)
- Wszystkie wiadomości mają dyskryminowany typ:
  ```ts
  type Message =
    | { type: "PRODUCT_DETECTED"; payload: ProductQuery }
    | { type: "PRICES_RESULT"; payload: PriceResult[] };
  ```
- Definicje wiadomości żyją w `src/shared/messages.ts`.

## Obsługa błędów
- `PriceProvider.search` nigdy nie rzuca „w górę” bez opakowania — zwraca pustą
  listę lub `Result`-owy typ; błąd logowany przez warstwę logowania.
- Brak cichego połykania błędów — zawsze log z kontekstem.

## Logowanie
- Warstwa `src/shared/logger.ts`. Zero `console.log` w kodzie produkcyjnym.
- Poziomy: `debug` (tylko dev build), `info`, `warn`, `error`.

## Testy
- Logika domenowa (`core/`) — testy jednostkowe obowiązkowe.
- Extractory — testy na zapisanych fixture'ach HTML/JSON-LD.
- UI — smoke testy renderowania.

## Commity
- Konwencja: `type(scope): opis` (np. `feat(core): add EAN-based matching`).
- Typy: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.
- **Opis zawsze po angielsku.** Commit tylko na wyraźną prośbę użytkownika,
  na branchu (nie na `main`).
- Sprawdzanie gramatyki: `.githooks/commit-msg` wysyła treść commita do
  LanguageTool i wypisuje sugestie (patrz ADR-004 w `DECISIONS.md`).
  **Domyślnie blokuje commit**, jeśli znajdzie błąd (samo ostrzeżenie łatwo
  przeoczyć np. w panelu Source Control w VSCode). Hook trzeba aktywować raz
  per klon:
  ```
  git config core.hooksPath .githooks
  ```
  Zejście do trybu tylko-ostrzegawczego dla jednego commita:
  `COMMIT_MSG_STRICT=0 git commit …`.
  Pominięcie sprawdzania dla pojedynczego commita: `git commit --no-verify`.
