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

## Milestone 2 — Warstwa cen
- [ ] Interfejs `PriceProvider` + typy `PriceResult`
- [ ] Pierwszy provider (mock/lokalny) do testów E2E
- [ ] Service worker: odbiór wiadomości, orkiestracja providerów
- [ ] Cache wyników w `chrome.storage`
- [ ] Matching produktów (EAN → fuzzy nazwa)

## Milestone 3 — UI
- [ ] Badge/overlay wstrzykiwany przez content script
- [ ] Popup z listą ofert (sortowanie po cenie)
- [ ] Strona opcji (źródła, waluta, próg alertu)

## Milestone 4 — Utwardzenie
- [ ] Minimalizacja permissions w manifeście
- [ ] Throttling/kolejka zapytań do źródeł
- [ ] Obsługa błędów i stany puste w UI
- [ ] Build produkcyjny + paczka do Chrome Web Store / AMO

## Backlog (niezaplanowane)
- [ ] Historia cen produktu
- [ ] Alerty o spadku ceny
- [ ] Wsparcie wielu walut / regionów
- [ ] Firefox (weryfikacja różnic MV3)
