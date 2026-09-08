import base64
import json
import os
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

MARZPAY_BASE_URL = os.getenv("MARZPAY_BASE_URL", "https://wallet.wearemarz.com/api/v1").rstrip("/")
MARZPAY_API_KEY = os.getenv("MARZPAY_API_KEY", "").strip()
MARZPAY_API_SECRET = os.getenv("MARZPAY_API_SECRET", "").strip()


class MarzPayError(RuntimeError):
    pass


def configured() -> bool:
    return bool(MARZPAY_API_KEY and MARZPAY_API_SECRET)


def _authorization_header() -> str:
    if not configured():
        raise MarzPayError("MarzPay credentials are not configured")
    raw = f"{MARZPAY_API_KEY}:{MARZPAY_API_SECRET}".encode("utf-8")
    return "Basic " + base64.b64encode(raw).decode("ascii")


def request_json(method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    body = json.dumps(payload).encode("utf-8") if payload is not None else None
    request = Request(
        f"{MARZPAY_BASE_URL}{path}",
        data=body,
        method=method,
        headers={
            "Authorization": _authorization_header(),
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": "Smith-Revenue-OS/1.0",
        },
    )
    try:
        with urlopen(request, timeout=25) as response:
            text = response.read().decode("utf-8", errors="replace")
            if not text.strip():
                return {}
            return json.loads(text)
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(detail)
            message = parsed.get("message") or parsed.get("error") or detail
        except ValueError:
            message = detail or str(exc)
        raise MarzPayError(f"MarzPay API error {exc.code}: {message}") from exc
    except (URLError, TimeoutError, ValueError) as exc:
        raise MarzPayError(f"MarzPay API unavailable: {exc}") from exc


def account_check() -> bool:
    response = request_json("GET", "/balance")
    return bool(response)


def create_payment_link(
    *,
    title: str,
    amount_ugx: int,
    description: str,
    callback_url: str,
) -> dict[str, Any]:
    response = request_json(
        "POST",
        "/payment-links",
        {
            "title": title[:200],
            "type": "payment",
            "amount": int(amount_ugx),
            "is_fixed": True,
            "currency": "UGX",
            "description": description[:255],
            "callback_url": callback_url,
            "country": "UG",
            "collection_methods": ["mobile_money", "card"],
        },
    )
    if not response.get("success"):
        raise MarzPayError(str(response.get("message") or "MarzPay did not create a payment link"))
    data = response.get("data") or {}
    if not data.get("uuid") or not data.get("payment_url"):
        raise MarzPayError("MarzPay returned an incomplete payment-link response")
    return data


def transaction_details(identifier: str) -> dict[str, Any]:
    if not identifier:
        raise MarzPayError("Missing MarzPay transaction identifier")
    return request_json("GET", f"/transactions/{identifier}")


def find_payment_link_uuid(payload: dict[str, Any]) -> str:
    candidates = [
        payload.get("payment_link"),
        (payload.get("transaction") or {}).get("payment_link") if isinstance(payload.get("transaction"), dict) else None,
        (payload.get("data") or {}).get("payment_link") if isinstance(payload.get("data"), dict) else None,
    ]
    data = payload.get("data")
    if isinstance(data, dict) and isinstance(data.get("transaction"), dict):
        candidates.append(data["transaction"].get("payment_link"))
    for value in candidates:
        if isinstance(value, dict) and value.get("uuid"):
            return str(value["uuid"])
        if isinstance(value, str) and value:
            return value
    return ""


def extract_transaction(payload: dict[str, Any]) -> dict[str, Any]:
    transaction = payload.get("transaction")
    if isinstance(transaction, dict):
        return transaction
    data = payload.get("data")
    if isinstance(data, dict) and isinstance(data.get("transaction"), dict):
        return data["transaction"]
    return {}
