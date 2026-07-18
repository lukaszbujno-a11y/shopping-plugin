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
