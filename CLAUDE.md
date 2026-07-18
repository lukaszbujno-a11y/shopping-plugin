# CLAUDE.md — reguły pracy nad projektem

> Ten plik jest ładowany automatycznie do kontekstu Claude Code na starcie każdej sesji.
> Zawiera zasady, których **zawsze** musisz przestrzegać. Szczegóły są w `docs/`.

## Czym jest projekt

**shopping-plugin** — rozszerzenie przeglądarki (Chrome/Firefox, Manifest V3), które
wykrywa produkt na aktualnie oglądanej stronie sklepu i automatycznie wyszukuje jego
najniższą cenę w innych sklepach, prezentując wynik użytkownikowi.

- Stack: **TypeScript + Node.js** (toolchain), bundler + Manifest V3.
- Szczegóły architektury: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Konwencje kodu: [docs/CONVENTIONS.md](docs/CONVENTIONS.md)
- Co robimy teraz / roadmapa: [docs/TODO.md](docs/TODO.md)
- Log decyzji technicznych (ADR): [docs/DECISIONS.md](docs/DECISIONS.md)

## Złote zasady (zawsze przestrzegaj)

1. **Najpierw przeczytaj, potem pisz.** Przed zmianą pliku zapoznaj się z jego
   zawartością i sąsiednim kodem. Dopasuj styl, nazewnictwo i idiomy do otoczenia.
2. **Małe, atomowe zmiany.** Jedna zmiana = jeden cel. Nie mieszaj refaktoru
   z nową funkcją w jednym kroku.
3. **Zawsze aktualizuj `docs/TODO.md`.** Gdy zaczynasz/kończysz zadanie z listy —
   odznacz je. Nowe pomysły dopisuj do sekcji Backlog, nie gub ich.
4. **Decyzje architektoniczne trafiają do `docs/DECISIONS.md`** (format ADR).
   Jeśli wybierasz między opcjami i wybór jest trwały — zapisz go z uzasadnieniem.
5. **Nie commituj i nie pushuj bez wyraźnej prośby.** Gdy proszę o commit —
   pracuj na branchu, nie na `main` bezpośrednio.
6. **Bezpieczeństwo danych użytkownika.** Rozszerzenie ma dostęp do stron sklepów.
   Nie zbieraj danych osobowych, nie wysyłaj nic na zewnętrzne serwery bez zgody
   i wpisu w `DECISIONS.md`. Minimalizuj `permissions` w manifeście.
7. **Weryfikuj zanim ogłosisz „gotowe”.** Uruchom lint, typecheck i build.
   Jeśli coś nie przeszło — powiedz o tym wprost, z outputem.

## Reguły techniczne

- **TypeScript strict.** Zero `any` bez komentarza uzasadniającego. Włączony
  `strict: true`.
- **Manifest V3.** Żadnych API z MV2 (np. persistent background pages) —
  używamy service workera.
- **Bez sekretów w repo.** Klucze API (jeśli będą) tylko przez `.env` /
  zmienne środowiskowe buildu, nigdy w kodzie źródłowym.
- **Warstwa scrapingu / źródeł cen jest odseparowana** za interfejsem
  (`PriceProvider`) — patrz architektura. Logika sklepów nie przecieka do UI.
- **Testy dla logiki biznesowej.** Parsery, matching produktów i providery cen
  mają testy jednostkowe. UI — smoke testy.

## Workflow każdego zadania

1. Sprawdź `docs/TODO.md` — co jest w toku / następne.
2. Zaplanuj (przy większych zadaniach — rozpisz kroki, użyj planu).
3. Zaimplementuj minimalną działającą wersję.
4. `npm run lint && npm run typecheck && npm run build` (i testy, jeśli dotyczy).
5. Zaktualizuj TODO / DECISIONS.
6. Podsumuj co zrobione i co dalej.

## Komendy (uzupełniaj w miarę powstawania toolchainu)

```
npm run dev        # build w trybie watch + hot reload rozszerzenia
npm run build      # produkcyjny build (dist/)
npm run lint       # ESLint
npm run typecheck  # tsc --noEmit
npm test           # testy jednostkowe
```

## Czego NIE robić

- Nie dodawaj zależności „na zapas” — każda zależność to koszt i ryzyko bezpieczeństwa.
- Nie wprowadzaj szerokich `host_permissions` (`<all_urls>`) bez uzasadnienia w ADR.
- Nie refaktoruj dużych obszarów bez pytania, gdy zadanie było wąskie.
- Nie zostawiaj `console.log` w kodzie produkcyjnym — używaj warstwy logowania.
