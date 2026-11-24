#!/usr/bin/env python3
"""Telegram notifications for the Android APK GitHub Actions workflow."""

from __future__ import annotations

import argparse
import asyncio
import html
import os
from pathlib import Path

from aiogram import Bot
from aiogram.client.default import DefaultBotProperties
from aiogram.exceptions import TelegramBadRequest
from aiogram.types import FSInputFile

ROOT = Path.cwd()
MESSAGE_ID_FILE = ROOT / ".telegram_msg_id"


def context() -> tuple[str, str, str, str]:
    return (
        os.getenv("GITHUB_REPOSITORY", "unknown/unknown"),
        os.getenv("GITHUB_REF_NAME", "unknown"),
        os.getenv("GITHUB_SHA", "unknown")[:7],
        os.getenv("GITHUB_RUN_NUMBER", "unknown"),
    )


def progress_text(status: str) -> str:
    repository, branch, commit, run_number = context()
    build_mark = "✅" if status in {"built", "uploaded"} else "❌" if status == "failed" else "⏳"
    upload_mark = "✅" if status == "uploaded" else "❌" if status == "upload_failed" else "⬜"
    overall = {
        "building": "⏳ Building",
        "built": "⏳ APK built; sending to Telegram",
        "uploaded": "✅ Successful",
        "failed": "❌ Build failed",
        "upload_failed": "❌ Telegram upload failed",
    }.get(status, "⏳ In progress")
    return (
        "<b>Android Release Build</b>\n\n"
        f"Repository: <code>{html.escape(repository)}</code>\n"
        f"Branch: <code>{html.escape(branch)}</code>\n"
        f"Commit: <code>{html.escape(commit)}</code>\n"
        f"Run: <code>#{html.escape(run_number)}</code>\n\n"
        "<b>Progress</b>\n"
        f"{build_mark} Build release APK\n"
        f"{upload_mark} Upload APK to Telegram\n\n"
        f"<b>Status:</b> {overall}"
    )


def get_config() -> tuple[str, str] | None:
    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    chat_id = os.getenv("TELEGRAM_CHAT_ID", "").strip()
    if not token or not chat_id:
        print("Telegram credentials are unavailable; notification skipped.")
        return None
    return token, chat_id


def save_message_id(message_id: int) -> None:
    MESSAGE_ID_FILE.write_text(str(message_id), encoding="utf-8")


def load_message_id() -> int | None:
    try:
        return int(MESSAGE_ID_FILE.read_text(encoding="utf-8").strip())
    except (OSError, ValueError):
        return None


async def send_or_edit(bot: Bot, chat_id: str, status: str) -> int:
    message_id = load_message_id()
    if message_id is not None:
        try:
            await bot.edit_message_text(
                chat_id=chat_id,
                message_id=message_id,
                text=progress_text(status),
            )
            return message_id
        except TelegramBadRequest as exc:
            error = str(exc).lower()
            if "message is not modified" in error:
                return message_id
            if "message to edit not found" not in error:
                raise
            print("Stored Telegram message was deleted; creating a replacement.")

    message = await bot.send_message(chat_id=chat_id, text=progress_text(status))
    save_message_id(message.message_id)
    return message.message_id


async def run(action: str, apk: str | None, log: str | None) -> None:
    config = get_config()
    if config is None:
        return

    token, chat_id = config
    async with Bot(
        token=token,
        default=DefaultBotProperties(parse_mode="HTML"),
    ) as bot:
        if action == "start":
            await send_or_edit(bot, chat_id, "building")
        elif action == "built":
            await send_or_edit(bot, chat_id, "built")
        elif action == "upload":
            if not apk or not Path(apk).is_file():
                raise FileNotFoundError(f"APK does not exist: {apk}")
            repository, branch, commit, run_number = context()
            caption = (
                "✅ <b>Android build successful</b>\n"
                f"Repository: <code>{html.escape(repository)}</code>\n"
                f"Branch: <code>{html.escape(branch)}</code>\n"
                f"Commit: <code>{html.escape(commit)}</code>\n"
                f"Run: <code>#{html.escape(run_number)}</code>"
            )
            await bot.send_document(
                chat_id=chat_id,
                document=FSInputFile(apk),
                caption=caption,
            )
            await send_or_edit(bot, chat_id, "uploaded")
        elif action == "failure":
            await send_or_edit(bot, chat_id, "failed")
            if log and Path(log).is_file() and Path(log).stat().st_size:
                await bot.send_document(
                    chat_id=chat_id,
                    document=FSInputFile(log),
                    caption="❌ <b>Gradle build log</b>",
                )
        elif action == "upload-failure":
            await send_or_edit(bot, chat_id, "upload_failed")
        else:
            raise ValueError(f"Unknown action: {action}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("start", "built", "upload", "failure", "upload-failure"))
    parser.add_argument("--apk")
    parser.add_argument("--log")
    args = parser.parse_args()
    asyncio.run(run(args.action, args.apk, args.log))


if __name__ == "__main__":
    main()
