# Premium model

## Tiers

| Tier | Price | Key prefix | Expiry | Access |
|------|-------|-----------|--------|--------|
| Forever | 50 ₽ once | `RF` | none | everything |
| Monthly | 10 ₽/month | `RM` | 1/3/6/12 months | everything |

Both tiers unlock the same feature set at activation time.

## Key format and validation

```
RF-AB3D-K7M2-9PQS-4f1c
|  |    |    |    |
|  random 12 chars  |  HMAC4
|  prefix RF/RM     |
```

- Alphabet: `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no `0`, `O`, `1`, `I`).
- `HMAC4 = first 4 hex chars of HMAC-SHA256(body12 + prefix, ROQI_SECRET)`.
- Client recomputes HMAC and compares the last 4 characters. No server needed.
- `tools/roqi_keys.py` implements and self-tests the same algorithm.

`ROQI_SECRET` must not be committed. It is derived from a salted digest of
the application hash at runtime in `RoqiPremium.java` and the Java call sites
are R8-obfuscated. Moving the secret into a native `.so` is still pending.

## Binding and revocation

Activation stores an obfuscated key, tier, activation time, monthly expiry,
HWID (`Build.FINGERPRINT` + `ANDROID_ID`) and Telegram id in private
`SharedPreferences`. A key is rejected if the current Telegram id differs,
if it is in the local revoked set, or (for Monthly) if the expiry has passed.

## Admin

Only Telegram id `8835641983` can issue keys. The id is assembled from parts
in Java so it does not appear as one literal; R8 folds it back at build time.
Generation uses `SecureRandom`. The current implementation issues keys and
shows status; the issued-key table, counters and per-key revoke buttons are
still to be connected to the encrypted store.

## Remaining work

- Native `.so` secret storage instead of a Java-derived secret.
- EncryptedSharedPreferences / encrypted issued-key database.
- 3-day expiry notification.
- Profile data transport via the sync bot inside the app (bot skeleton exists).
- The rest of the 136-feature specification, verified against upstream hooks.
