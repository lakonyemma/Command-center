import hashlib
import html
import json
import os
import re
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from xml.etree import ElementTree

from backend.main import Lead, Opportunity, SessionLocal
from sqlalchemy import select

USER_AGENT = "Lakony-Smith-Revenue-Hunter/1.1"
DEFAULT_FEEDS = [
    ("We Work Remotely", "https://weworkremotely.com/categories/remote-programming-jobs.rss"),
    ("Remote OK", "https://remoteok.com/remote-jobs.rss"),
]

HIGH_VALUE = {
    "automation": 18,
    "ai": 14,
    "artificial intelligence": 18,
    "android": 14,
    "mobile app": 18,
    "web app": 18,
    "full stack": 15,
    "backend": 12,
    "frontend": 12,
    "python": 10,
    "django": 12,
    "fastapi": 12,
    "kotlin": 12,
    "api": 10,
    "integration": 12,
    "saas": 14,
    "software engineer": 15,
    "developer": 10,
    "contract": 9,
    "freelance": 12,
    "consultant": 10,
    "project": 8,
    "part-time": 6,
}

NEGATIVE = {
    "senior director": -10,
    "vp ": -12,
    "vice president": -12,
    "chief ": -12,
    "principal": -5,
}

CLIENT_INTENT = (
    "contract",
    "freelance",
    "consultant",
    "consulting",
    "project",
    "part-time",
    "temporary",
    "short term",
    "short-term",
    "agency",
)


def fetch_text(url: str) -> str:
    request = Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/rss+xml, application/xml, text/xml, application/json",
        },
    )
    with urlopen(request, timeout=20) as response:
        return response.read().decode("utf-8", errors="replace")


def strip_markup(value: str) -> str:
    clean = re.sub(r"<[^>]+>", " ", value or "")
    clean = html.unescape(clean)
    return re.sub(r"\s+", " ", clean).strip()


def score(title: str, description: str) -> int:
    text = f"{title} {description}".lower()
    points = 20
    for phrase, value in HIGH_VALUE.items():
        if phrase in text:
            points += value
    for phrase, value in NEGATIVE.items():
        if phrase in text:
            points += value
    if "remote" in text or "worldwide" in text:
        points += 8
    return max(0, min(100, points))


def estimate_value_ugx(opportunity_score: int) -> int:
    if opportunity_score >= 85:
        return 4_000_000
    if opportunity_score >= 70:
        return 2_500_000
    if opportunity_score >= 55:
        return 1_500_000
    return 750_000


def is_client_candidate(title: str, description: str, opportunity_score: int) -> bool:
    text = f"{title} {description}".lower()
    explicit_client_intent = any(phrase in text for phrase in CLIENT_INTENT)
    return opportunity_score >= 60 and (explicit_client_intent or opportunity_score >= 85)


def proposal_for(title: str, company: str, description: str) -> str:
    company_name = company.strip() or "your team"
    focus = strip_markup(description)[:220]
    return (
        f"Hello {company_name},\n\n"
        f"I saw your need for {title}. I build practical software and automation systems focused on reducing manual work and getting usable results quickly. "
        f"From the brief, the immediate need appears to be: {focus}\n\n"
        "I can review the requirement, propose a small first milestone, and give you a clear delivery plan before any commitment. "
        "If useful, I can send a short scope and estimate tailored to this project.\n\n"
        "Regards,\nLakony Emmanuel"
    )


def parse_feed(source: str, url: str) -> list[dict]:
    text = fetch_text(url)
    root = ElementTree.fromstring(text)
    rows = []
    for item in root.findall(".//item")[:80]:
        title = strip_markup(item.findtext("title") or "")
        link = strip_markup(item.findtext("link") or "")
        description = strip_markup(item.findtext("description") or "")
        company = ""
        if ":" in title:
            company, possible_title = title.split(":", 1)
            if len(company) < 120 and possible_title.strip():
                title = possible_title.strip()
            else:
                company = ""
        if not title or not link:
            continue
        opportunity_score = score(title, description)
        if opportunity_score < 45:
            continue
        rows.append(
            {
                "source": source,
                "source_url": link,
                "title": title,
                "company": company.strip(),
                "description": description[:3000],
                "score": opportunity_score,
                "estimated_value_ugx": estimate_value_ugx(opportunity_score),
                "client_candidate": is_client_candidate(title, description, opportunity_score),
            }
        )
    return rows


def run_scan() -> dict:
    feeds = list(DEFAULT_FEEDS)
    extra = os.getenv("SMITH_OPPORTUNITY_FEEDS", "").strip()
    if extra:
        for entry in extra.split(","):
            url = entry.strip()
            if url:
                feeds.append(("Custom feed", url))

    discovered = []
    errors = []
    for source, url in feeds:
        try:
            discovered.extend(parse_feed(source, url))
        except (HTTPError, URLError, TimeoutError, ValueError, ElementTree.ParseError) as exc:
            errors.append(f"{source}: {exc}")

    added = 0
    updated = 0
    leads_added = 0
    db = SessionLocal()
    try:
        for item in discovered:
            fingerprint = hashlib.sha256(item["source_url"].encode("utf-8")).hexdigest()[:32]
            opportunity_id = f"opp-{fingerprint}"
            existing = db.scalar(select(Opportunity).where(Opportunity.fingerprint == fingerprint))
            if existing:
                existing.title = item["title"]
                existing.company = item["company"]
                existing.description = item["description"]
                existing.score = item["score"]
                existing.estimated_value_ugx = item["estimated_value_ugx"]
                existing.last_seen_at = datetime.now(timezone.utc)
                updated += 1
            else:
                db.add(
                    Opportunity(
                        id=opportunity_id,
                        fingerprint=fingerprint,
                        source=item["source"],
                        source_url=item["source_url"],
                        title=item["title"],
                        company=item["company"],
                        description=item["description"],
                        score=item["score"],
                        estimated_value_ugx=item["estimated_value_ugx"],
                        proposal_draft=proposal_for(item["title"], item["company"], item["description"]),
                        status="NEW",
                        approval_status="REVIEW_REQUIRED",
                    )
                )
                added += 1

            if item["client_candidate"]:
                lead_id = f"smith-{fingerprint}"
                if db.get(Lead, lead_id) is None:
                    display_name = item["company"].strip() or item["title"]
                    db.add(
                        Lead(
                            id=lead_id,
                            name=display_name[:200],
                            company=item["company"].strip()[:200],
                            contact=item["source_url"][:200],
                            stage="NEW",
                            estimated_monthly_value_ugx=item["estimated_value_ugx"],
                        )
                    )
                    leads_added += 1
        db.commit()
    finally:
        db.close()

    return {
        "scanned": len(discovered),
        "added": added,
        "updated": updated,
        "leadsAdded": leads_added,
        "errors": errors,
        "sources": [name for name, _ in feeds],
    }


if __name__ == "__main__":
    print(json.dumps(run_scan(), indent=2))
