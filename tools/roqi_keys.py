#!/usr/bin/env python3
"""RoqiGram Premium key generation/validation.

Key layout:
  PREFIX-RRRR-RRRR-RRRR-CCCC
  PREFIX = RF (Forever) or RM (Monthly)
  RRRR   = three groups from ALPHABET (no 0/O/1/I)
  CCCC   = first 4 hex chars of HMAC-SHA256

The checksum payload is:
  body12 + prefix + months
where months is 0 for RF and 1/3/6/12 for RM. The month is carried by the key,
so the client never has to trust a caller-supplied duration.

Secret:
  ROQI_SECRET       - final HMAC key, used as-is if set
  ROQI_BASE_SECRET  - base value; the HMAC key becomes
                      sha256("roqi.premium.hmac.v1" + base).hexdigest()

The Android client uses ROQI_BASE_SECRET = the Telegram API hash that is already
compiled into the fork. Keep it out of public issue reports.
"""

import hashlib
import hmac
import os
import re
import sys

ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
KEY_RE = re.compile(r"^(RF|RM)(-[A-HJ-NP-Z2-9]{4}){3}-[0-9A-F]{4}$")
PREFIXES = {"RF": "FOREVER", "RM": "MONTHLY"}
MONTHS = (1, 3, 6, 12)


def secret() -> bytes:
    direct = os.environ.get("ROQI_SECRET")
    if direct:
        return direct.encode("utf-8")
    base = os.environ.get("ROQI_BASE_SECRET")
    if not base:
        raise SystemExit("Set ROQI_SECRET or ROQI_BASE_SECRET")
    return hashlib.sha256(("roqi.premium.hmac.v1" + base).encode("utf-8")).hexdigest().encode("ascii")


def checksum(body12: str, prefix: str, months: int, key: bytes) -> str:
    payload = body12 + prefix + str(0 if prefix == "RF" else months)
    return hmac.new(key, payload.encode("ascii"), hashlib.sha256).hexdigest()[:4].lower()


def generate(prefix: str, key: bytes, months: int = 1) -> str:
    if prefix not in PREFIXES:
        raise ValueError("prefix must be RF or RM")
    if prefix == "RM" and months not in MONTHS:
        raise ValueError("monthly duration must be 1, 3, 6 or 12")
    body = "".join(ALPHABET[b % 32] for b in os.urandom(12))
    groups = [body[i:i + 4] for i in range(0, 12, 4)]
    groups.append(checksum(body, prefix, months, key))
    return prefix + "-" + "-".join(groups)


def validate(raw: str, key: bytes) -> dict:
    raw = raw.strip().upper()
    if not KEY_RE.match(raw):
        return {"valid": False, "reason": "bad format"}
    compact = raw.replace("-", "")
    prefix = compact[:2]
    body = compact[2:14]
    supplied = compact[14:].lower()
    candidates = (0,) if prefix == "RF" else MONTHS
    for months in candidates:
        if hmac.compare_digest(supplied, checksum(body, prefix, months, key)):
            return {
                "valid": True,
                "prefix": prefix,
                "type": PREFIXES[prefix],
                "months": 0 if prefix == "RF" else months,
                "key": raw,
            }
    return {"valid": False, "reason": "bad checksum"}


def main() -> None:
    key = secret()
    mode = sys.argv[1] if len(sys.argv) > 1 else "gen"
    if mode == "gen":
        prefix = sys.argv[2] if len(sys.argv) > 2 else "RF"
        months = int(sys.argv[3]) if len(sys.argv) > 3 else 1
        print(generate(prefix, key, months))
    elif mode == "check":
        print(validate(sys.argv[2], key))
    elif mode == "selftest":
        for prefix, months in (("RF", 0), ("RM", 1), ("RM", 3), ("RM", 6), ("RM", 12)):
            k = generate(prefix, key, months)
            result = validate(k, key)
            assert result["valid"], k
            assert result["months"] == months, (k, result)
            broken = k[:-1] + ("A" if k[-1] != "A" else "B")
            assert validate(broken, key)["valid"] is False, broken
            print(k, "->", result)
    else:
        raise SystemExit("usage: roqi_keys.py [gen RF|RM [months] | check KEY | selftest]")


if __name__ == "__main__":
    main()
