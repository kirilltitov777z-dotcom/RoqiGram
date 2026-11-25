# RoqiGram

Fork of [exteraGram](https://github.com/exteraSquad/exteraGram), GPLv3.
Package name and `applicationId` are intentionally unchanged so plugins and
signing stay compatible.

**Repository:** https://github.com/kirilltitov777z-dotcom/RoqiGram

## What is in this tree now

- Rebranding: app name, preferences title and About string.
- 10 Roqi icons added to the exteraGram icon generator
  (`TMessagesProj/appicons/icons.json` + `appicons/roqi/`). Icon 1 is the
  default; icons 4–10 are hidden in the picker unless Premium is active.
- Premium core (`RoqiPremium.java`): offline HMAC key validation, `RF`/`RM`
  prefixes, activation binding to Telegram id + HWID, monthly expiry,
  revocation list, admin-only key generation, status text.
- Settings entry: Appearance → RoqiGram Premium with activation dialog.
  Admin id `8835641983` opens a key-issuing dialog.
- 3 plugins under `roqigram-plugins/` (New Year, Fake NFT, Nick FX).
- Profile sync bot skeleton under `roqigram-sync-bot/` (aiogram + SQLite).
- Premium badge artwork in `roqigram-assets/`.
- CI: `.github/workflows/build-roqigram.yml`.

## Build

Secrets in the repository: `API_ID`, `API_HASH`, `MAPS_V2_API`.
If the release keystore is not provided, CI generates a temporary one so the
build is reproducible. For a production build set `SIGNING_KEY_STORE_BASE64`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_KEY_STORE_PASSWORD`.

```bash
./gradlew :TMessagesProj:assembleFullAfatRelease
```

## Premium keys

`RF-XXXX-XXXX-XXXX-XXXX` — Forever, 50 ₽, no expiry.
`RM-XXXX-XXXX-XXXX-XXXX` — Monthly, 10 ₽/month, 1/3/6/12 months.

12 random chars from a 32-character alphabet without `0/O/1/I`, plus 4 hex
chars of `HMAC-SHA256(body12 + prefix, ROQI_SECRET)`. Validation is offline.
Both tiers unlock the same set of features immediately; there is no staged
rollout.

Keys are issued only by admin id `8835641983`, in Settings → Appearance →
RoqiGram Premium → Admin. Purchase contact: @Cafulu.

## Honest status

The 136-item specification is a multi-iteration task. This commit contains
the foundation, rebranding, all 10 icons, Premium core and three plugins —
not all 136 features. See `docs/PREMIUM.md` for the security model and the
work that remains.

## License

GPLv3, same as exteraGram. Keep this repository public.
