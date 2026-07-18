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
  self-hosted (Docker) dla prostoty setupu. Hook jest domyślnie
  ostrzegawczy (nie blokuje commita) — blokowanie włącza się przez
  `COMMIT_MSG_STRICT=1`. Hook aktywuje się lokalnie przez
  `git config core.hooksPath .githooks` (nie jest globalny ani wymuszony
  przez CI).
- Konsekwencje: Treść commit message (nie kod, nie dane ze stron sklepów)
  jest wysyłana do zewnętrznego serwisu przy każdym commicie na maszynach,
  które włączyły ten hook. Wymaga sieci — offline hook po prostu pomija
  sprawdzanie (fail-open). Jeśli w przyszłości prywatność treści commitów
  stanie się problemem, można przejść na self-hosted LanguageTool (Docker)
  bez zmiany interfejsu hooka.
