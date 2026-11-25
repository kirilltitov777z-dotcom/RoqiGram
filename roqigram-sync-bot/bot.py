#!/usr/bin/env python3
"""@RoqiGramSyncBot — profile data sync for RoqiGram users.

Stores profile JSON (nick colors, banner, fake NFT, music wave) keyed by
Telegram user id and serves it to other RoqiGram clients. Returns an empty
object for users who are not in RoqiGram, so their profile renders normally
in every client.

Requires: pip install aiogram==3.*
Environment: BOT_TOKEN
"""
import asyncio
import json
import os
import sqlite3
from datetime import datetime, timezone

from aiogram import Bot, Dispatcher, F, types
from aiogram.filters import Command, CommandObject
from aiogram.types import ChatAction

DB = os.environ.get("ROQI_SYNC_DB", "roqigram_sync.db")
CACHE_HOURS = 24


def db():
    con = sqlite3.connect(DB)
    con.execute(
        "CREATE TABLE IF NOT EXISTS profiles ("
        "tg_id INTEGER PRIMARY KEY, data TEXT NOT NULL, updated TEXT NOT NULL)"
    )
    con.execute(
        "CREATE TABLE IF NOT EXISTS assets ("
        "tg_id INTEGER, kind TEXT, file_id TEXT NOT NULL, PRIMARY KEY (tg_id, kind))"
    )
    con.commit()
    return con


def save_profile(tg_id: int, data: dict):
    with db() as con:
        con.execute(
            "INSERT INTO profiles (tg_id, data, updated) VALUES (?, ?, ?) "
            "ON CONFLICT(tg_id) DO UPDATE SET data=excluded.data, updated=excluded.updated",
            (tg_id, json.dumps(data, ensure_ascii=False), datetime.now(timezone.utc).isoformat()),
        )


def load_profile(tg_id: int):
    with db() as con:
        row = con.execute("SELECT data FROM profiles WHERE tg_id=?", (tg_id,)).fetchone()
    return json.loads(row[0]) if row else {}


bot = Bot(os.environ["BOT_TOKEN"])
dp = Dispatcher()


@dp.message(Command("start"))
async def start(message: types.Message):
    await message.answer(
        "RoqiGram Sync Bot.\n"
        "/set <json> — save your profile styling\n"
        "/get <tg_id> — read a profile (empty if the user is not in RoqiGram)\n"
        "/banner — send a photo to use as your profile banner\n"
        "/nft — send an image for your local decorative badge\n"
        "/me — show your saved profile"
    )


@dp.message(Command("set"))
async def set_profile(message: types.Message, command: CommandObject):
    raw = (command.args or "").strip()
    if not raw:
        await message.answer("Send: /set {\"nick\": {...}}")
        return
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as exc:
        await message.answer(f"Invalid JSON: {exc}")
        return
    data["tg_id"] = message.from_user.id
    save_profile(message.from_user.id, data)
    await message.answer("Saved. RoqiGram clients will cache it for 24 hours.")


@dp.message(Command("get"))
async def get_profile(message: types.Message, command: CommandObject):
    try:
        tg_id = int((command.args or "").strip())
    except ValueError:
        await message.answer("Usage: /get 123456789")
        return
    await bot.send_chat_action(message.chat.id, ChatAction.TYPING)
    await message.answer(json.dumps(load_profile(tg_id), ensure_ascii=False))


@dp.message(Command("me"))
async def me(message: types.Message):
    await message.answer(json.dumps(load_profile(message.from_user.id), ensure_ascii=False))


@dp.message(F.photo)
async def photo(message: types.Message, command: CommandObject = None):
    text = (message.caption or "").strip().lower()
    kind = "banner" if text.startswith("/banner") else "nft" if text.startswith("/nft") else None
    if kind is None:
        return
    file_id = message.photo[-1].file_id
    with db() as con:
        con.execute(
            "INSERT INTO assets (tg_id, kind, file_id) VALUES (?, ?, ?) "
            "ON CONFLICT(tg_id, kind) DO UPDATE SET file_id=excluded.file_id",
            (message.from_user.id, kind, file_id),
        )
    await message.answer(f"{kind} saved as a Telegram file_id.")


async def main():
    await dp.start_polling(bot)


if __name__ == "__main__":
    asyncio.run(main())
