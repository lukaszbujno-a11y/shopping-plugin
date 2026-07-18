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
