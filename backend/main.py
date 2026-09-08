import json
import os
import uuid
from datetime import datetime, timezone
from typing import Optional
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from fastapi import Depends, FastAPI, Header, HTTPException, Request as FastAPIRequest
from pydantic import BaseModel, Field
from sqlalchemy import BigInteger, Boolean, DateTime, Integer, String, Text, create_engine, func, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./revenue_os.db")
if DATABASE_URL.startswith("postgresql://"):
    DATABASE_URL = DATABASE_URL.replace("postgresql://", "postgresql+psycopg://", 1)
API_KEY = os.getenv("REVENUE_API_KEY", "")
EXPECTED_GOOGLE_ACCOUNT = os.getenv("REVENUE_GOOGLE_ACCOUNT", "lakonyemmanuel92@gmail.com").strip().lower()
PUBLIC_BASE_URL = os.getenv("REVENUE_PUBLIC_BASE_URL", "https://revenue-os-api-04cu.onrender.com").rstrip("/")

connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(DATABASE_URL, pool_pre_ping=True, connect_args=connect_args)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    pass


class Lead(Base):
    __tablename__ = "revenue_leads"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    name: Mapped[str] = mapped_column(String(200))
    company: Mapped[str] = mapped_column(String(200), default="")
    contact: Mapped[str] = mapped_column(String(200), default="")
    stage: Mapped[str] = mapped_column(String(32), default="NEW")
    estimated_monthly_value_ugx: Mapped[int] = mapped_column(BigInteger, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class Customer(Base):
    __tablename__ = "revenue_customers"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    name: Mapped[str] = mapped_column(String(200))
    company: Mapped[str] = mapped_column(String(200), default="")
    contact: Mapped[str] = mapped_column(String(200), default="")
    plan: Mapped[str] = mapped_column(String(120), default="Custom")
    monthly_value_ugx: Mapped[int] = mapped_column(BigInteger, default=0)
    subscription_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class Payment(Base):
    __tablename__ = "revenue_payments"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    customer_id: Mapped[str] = mapped_column(String(64), index=True)
    amount_ugx: Mapped[int] = mapped_column(BigInteger)
    status: Mapped[str] = mapped_column(String(32), default="PAID")
    reference: Mapped[str] = mapped_column(String(200), default="")
    paid_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class Invoice(Base):
    __tablename__ = "revenue_invoices"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    customer_id: Mapped[str] = mapped_column(String(64), index=True)
    amount_ugx: Mapped[int] = mapped_column(BigInteger)
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    paid: Mapped[bool] = mapped_column(Boolean, default=False)


class PaymentRequest(Base):
    __tablename__ = "revenue_payment_requests"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    invoice_id: Mapped[str] = mapped_column(String(64), index=True)
    customer_id: Mapped[str] = mapped_column(String(64), index=True)
    provider: Mapped[str] = mapped_column(String(32), default="MARZPAY")
    payment_link_uuid: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    payment_url: Mapped[str] = mapped_column(Text)
    amount_ugx: Mapped[int] = mapped_column(BigInteger)
    status: Mapped[str] = mapped_column(String(32), default="PENDING")
    transaction_uuid: Mapped[str] = mapped_column(String(128), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class Opportunity(Base):
    __tablename__ = "smith_opportunities"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    fingerprint: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    source: Mapped[str] = mapped_column(String(120), default="")
    source_url: Mapped[str] = mapped_column(Text)
    title: Mapped[str] = mapped_column(String(300))
    company: Mapped[str] = mapped_column(String(200), default="")
    description: Mapped[str] = mapped_column(Text, default="")
    score: Mapped[int] = mapped_column(Integer, default=0)
    estimated_value_ugx: Mapped[int] = mapped_column(BigInteger, default=0)
    proposal_draft: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(32), default="NEW")
    approval_status: Mapped[str] = mapped_column(String(32), default="REVIEW_REQUIRED")
    discovered_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


Base.metadata.create_all(engine)

app = FastAPI(title="Taskly Revenue OS API", version="1.3.0")


def google_email_for_token(token: str) -> str:
    request = Request(
        "https://gmail.googleapis.com/gmail/v1/users/me/profile",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urlopen(request, timeout=10) as response:
            payload = json.loads(response.read().decode("utf-8"))
            return str(payload.get("emailAddress", "")).strip().lower()
    except (HTTPError, URLError, TimeoutError, ValueError):
        return ""


def require_api_key(
    x_api_key: Optional[str] = Header(default=None),
    authorization: Optional[str] = Header(default=None),
):
    if API_KEY and x_api_key == API_KEY:
        return

    if authorization and authorization.lower().startswith("bearer "):
        token = authorization.split(" ", 1)[1].strip()
        email = google_email_for_token(token)
        if email and email == EXPECTED_GOOGLE_ACCOUNT:
            return
        raise HTTPException(status_code=403, detail="Google account is not authorized for Revenue OS")

    if not API_KEY:
        raise HTTPException(status_code=503, detail="REVENUE_API_KEY is not configured")
    raise HTTPException(status_code=401, detail="Revenue OS authorization required")


def db_session():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


class LeadIn(BaseModel):
    name: str = Field(min_length=1, max_length=200)
    company: str = ""
    contact: str = ""
    estimated_monthly_value_ugx: int = Field(default=0, ge=0)


class LeadStageIn(BaseModel):
    stage: str


class CustomerIn(BaseModel):
    name: str = Field(min_length=1, max_length=200)
    company: str = ""
    contact: str = ""
    plan: str = "Custom"
    monthly_value_ugx: int = Field(gt=0)


class PaymentIn(BaseModel):
    customer_id: str
    amount_ugx: int = Field(gt=0)
    reference: str = ""
    status: str = "PAID"


class InvoiceIn(BaseModel):
    customer_id: str
    amount_ugx: int = Field(gt=0)
    due_at: datetime


class OpportunityApprovalIn(BaseModel):
    approval_status: str


@app.get("/health")
def health():
    from backend.marzpay import configured

    return {
        "ok": True,
        "service": "taskly-revenue-os",
        "version": "1.3.0",
        "marzpayConfigured": configured(),
    }


@app.get("/health/marzpay")
def marzpay_health():
    from backend.marzpay import MarzPayError, account_check, configured

    if not configured():
        return {"configured": False, "reachable": False}
    try:
        return {"configured": True, "reachable": bool(account_check())}
    except MarzPayError:
        return {"configured": True, "reachable": False}


@app.get("/v1/summary", dependencies=[Depends(require_api_key)])
def summary(db: Session = Depends(db_session)):
    active_customers = db.scalar(select(func.count(Customer.id)).where(Customer.subscription_active.is_(True))) or 0
    mrr = db.scalar(select(func.coalesce(func.sum(Customer.monthly_value_ugx), 0)).where(Customer.subscription_active.is_(True))) or 0
    collected = db.scalar(select(func.coalesce(func.sum(Payment.amount_ugx), 0)).where(Payment.status == "PAID")) or 0
    outstanding = db.scalar(select(func.coalesce(func.sum(Invoice.amount_ugx), 0)).where(Invoice.paid.is_(False))) or 0
    qualified = db.scalar(select(func.count(Lead.id)).where(Lead.stage.in_(["QUALIFIED", "PROPOSAL", "WON"]))) or 0
    won = db.scalar(select(func.count(Lead.id)).where(Lead.stage == "WON")) or 0
    lost = db.scalar(select(func.count(Lead.id)).where(Lead.stage == "LOST")) or 0
    closed = won + lost
    conversion = (won / closed * 100.0) if closed else 0.0
    return {
        "monthlyRecurringRevenueUgx": int(mrr),
        "collectedRevenueUgx": int(collected),
        "outstandingRevenueUgx": int(outstanding),
        "activeCustomers": int(active_customers),
        "activeSubscriptions": int(active_customers),
        "qualifiedLeads": int(qualified),
        "conversionRatePercent": conversion,
    }


@app.get("/v1/leads", dependencies=[Depends(require_api_key)])
def list_leads(db: Session = Depends(db_session)):
    rows = db.scalars(select(Lead).order_by(Lead.created_at.desc())).all()
    return [
        {
            "id": row.id,
            "name": row.name,
            "company": row.company,
            "contact": row.contact,
            "stage": row.stage,
            "estimatedMonthlyValueUgx": row.estimated_monthly_value_ugx,
            "createdAt": row.created_at,
        }
        for row in rows
    ]


@app.post("/v1/leads", dependencies=[Depends(require_api_key)])
def create_lead(payload: LeadIn, db: Session = Depends(db_session)):
    row = Lead(
        id=str(uuid.uuid4()),
        name=payload.name.strip(),
        company=payload.company.strip(),
        contact=payload.contact.strip(),
        estimated_monthly_value_ugx=payload.estimated_monthly_value_ugx,
    )
    db.add(row)
    db.commit()
    return {"id": row.id}


@app.patch("/v1/leads/{lead_id}/stage", dependencies=[Depends(require_api_key)])
def update_lead_stage(lead_id: str, payload: LeadStageIn, db: Session = Depends(db_session)):
    allowed = {"NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST"}
    stage = payload.stage.upper()
    if stage not in allowed:
        raise HTTPException(status_code=400, detail="Invalid lead stage")
    row = db.get(Lead, lead_id)
    if not row:
        raise HTTPException(status_code=404, detail="Lead not found")
    row.stage = stage
    db.commit()
    return {"ok": True}


@app.get("/v1/customers", dependencies=[Depends(require_api_key)])
def list_customers(db: Session = Depends(db_session)):
    rows = db.scalars(select(Customer).order_by(Customer.created_at.desc())).all()
    return [
        {
            "id": row.id,
            "name": row.name,
            "company": row.company,
            "contact": row.contact,
            "plan": row.plan,
            "monthlyValueUgx": row.monthly_value_ugx,
            "subscriptionActive": row.subscription_active,
            "createdAt": row.created_at,
        }
        for row in rows
    ]


@app.post("/v1/customers", dependencies=[Depends(require_api_key)])
def create_customer(payload: CustomerIn, db: Session = Depends(db_session)):
    row = Customer(
        id=str(uuid.uuid4()),
        name=payload.name.strip(),
        company=payload.company.strip(),
        contact=payload.contact.strip(),
        plan=payload.plan.strip() or "Custom",
        monthly_value_ugx=payload.monthly_value_ugx,
    )
    db.add(row)
    db.commit()
    return {"id": row.id}


@app.get("/v1/payments", dependencies=[Depends(require_api_key)])
def list_payments(db: Session = Depends(db_session)):
    rows = db.scalars(select(Payment).order_by(Payment.paid_at.desc())).all()
    return [
        {
            "id": row.id,
            "customerId": row.customer_id,
            "amountUgx": row.amount_ugx,
            "status": row.status,
            "reference": row.reference,
            "paidAt": row.paid_at,
        }
        for row in rows
    ]


@app.post("/v1/payments", dependencies=[Depends(require_api_key)])
def create_payment(payload: PaymentIn, db: Session = Depends(db_session)):
    if not db.get(Customer, payload.customer_id):
        raise HTTPException(status_code=404, detail="Customer not found")
    row = Payment(
        id=str(uuid.uuid4()),
        customer_id=payload.customer_id,
        amount_ugx=payload.amount_ugx,
        reference=payload.reference.strip(),
        status=payload.status.upper(),
    )
    db.add(row)
    db.commit()
    return {"id": row.id}


@app.post("/v1/invoices", dependencies=[Depends(require_api_key)])
def create_invoice(payload: InvoiceIn, db: Session = Depends(db_session)):
    if not db.get(Customer, payload.customer_id):
        raise HTTPException(status_code=404, detail="Customer not found")
    row = Invoice(
        id=str(uuid.uuid4()),
        customer_id=payload.customer_id,
        amount_ugx=payload.amount_ugx,
        due_at=payload.due_at,
    )
    db.add(row)
    db.commit()
    return {"id": row.id}


@app.get("/v1/payment-requests", dependencies=[Depends(require_api_key)])
def list_payment_requests(db: Session = Depends(db_session)):
    rows = db.scalars(select(PaymentRequest).order_by(PaymentRequest.created_at.desc()).limit(200)).all()
    return [
        {
            "id": row.id,
            "invoiceId": row.invoice_id,
            "customerId": row.customer_id,
            "provider": row.provider,
            "paymentUrl": row.payment_url,
            "amountUgx": row.amount_ugx,
            "status": row.status,
            "transactionUuid": row.transaction_uuid,
            "createdAt": row.created_at,
            "updatedAt": row.updated_at,
        }
        for row in rows
    ]


@app.post("/v1/invoices/{invoice_id}/payment-link", dependencies=[Depends(require_api_key)])
def create_invoice_payment_link(invoice_id: str, db: Session = Depends(db_session)):
    from backend.marzpay import MarzPayError, configured, create_payment_link

    invoice = db.get(Invoice, invoice_id)
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    if invoice.paid:
        raise HTTPException(status_code=409, detail="Invoice is already paid")
    if not configured():
        raise HTTPException(status_code=503, detail="MarzPay credentials are not configured")

    existing = db.scalar(
        select(PaymentRequest)
        .where(PaymentRequest.invoice_id == invoice.id)
        .where(PaymentRequest.status.in_(["PENDING", "CREATED"]))
        .order_by(PaymentRequest.created_at.desc())
    )
    if existing:
        return {
            "id": existing.id,
            "invoiceId": existing.invoice_id,
            "paymentUrl": existing.payment_url,
            "amountUgx": existing.amount_ugx,
            "status": existing.status,
        }

    customer = db.get(Customer, invoice.customer_id)
    customer_label = (customer.company or customer.name) if customer else "Client"
    callback_url = f"{PUBLIC_BASE_URL}/webhooks/marzpay"
    try:
        data = create_payment_link(
            title=f"Smith Revenue OS invoice - {customer_label}",
            amount_ugx=invoice.amount_ugx,
            description=f"Smith Revenue OS invoice {invoice.id}",
            callback_url=callback_url,
        )
    except MarzPayError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    row = PaymentRequest(
        id=str(uuid.uuid4()),
        invoice_id=invoice.id,
        customer_id=invoice.customer_id,
        provider="MARZPAY",
        payment_link_uuid=str(data["uuid"]),
        payment_url=str(data["payment_url"]),
        amount_ugx=invoice.amount_ugx,
        status="PENDING",
    )
    db.add(row)
    db.commit()
    return {
        "id": row.id,
        "invoiceId": row.invoice_id,
        "paymentUrl": row.payment_url,
        "amountUgx": row.amount_ugx,
        "status": row.status,
    }


@app.post("/webhooks/marzpay")
async def marzpay_webhook(request: FastAPIRequest, db: Session = Depends(db_session)):
    from backend.marzpay import MarzPayError, extract_transaction, find_payment_link_uuid, transaction_details

    raw = await request.body()
    try:
        callback = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, ValueError):
        raise HTTPException(status_code=400, detail="Invalid JSON")

    callback_tx = extract_transaction(callback)
    transaction_uuid = str(callback_tx.get("uuid") or "").strip()
    if not transaction_uuid:
        return {"ok": True, "ignored": "missing transaction uuid"}

    # Do not trust an unsigned callback by itself. Re-fetch the transaction from
    # MarzPay using the server-side API credentials and act only on verified data.
    try:
        verified = transaction_details(transaction_uuid)
    except MarzPayError:
        return {"ok": True, "ignored": "transaction could not be verified"}

    verified_tx = extract_transaction(verified)
    if str(verified_tx.get("uuid") or "") != transaction_uuid:
        return {"ok": True, "ignored": "transaction verification mismatch"}

    payment_link_uuid = find_payment_link_uuid(verified) or find_payment_link_uuid(callback)
    payment_request = None
    if payment_link_uuid:
        payment_request = db.scalar(
            select(PaymentRequest).where(PaymentRequest.payment_link_uuid == payment_link_uuid)
        )

    if payment_request is None:
        description = str(verified_tx.get("description") or callback_tx.get("description") or "")
        marker = "Smith Revenue OS invoice "
        if marker in description:
            invoice_id = description.split(marker, 1)[1].strip().split()[0]
            payment_request = db.scalar(
                select(PaymentRequest)
                .where(PaymentRequest.invoice_id == invoice_id)
                .order_by(PaymentRequest.created_at.desc())
            )

    if payment_request is None:
        return {"ok": True, "ignored": "payment request not found"}

    status = str(verified_tx.get("status") or "").lower()
    amount = verified_tx.get("amount") or {}
    raw_amount = amount.get("raw") if isinstance(amount, dict) else None
    currency = str(amount.get("currency") or "") if isinstance(amount, dict) else ""

    if status in {"failed", "cancelled", "canceled"}:
        payment_request.status = status.upper()
        payment_request.transaction_uuid = transaction_uuid
        payment_request.updated_at = datetime.now(timezone.utc)
        db.commit()
        return {"ok": True, "status": payment_request.status}

    if status not in {"completed", "successful", "success"}:
        return {"ok": True, "ignored": f"non-final status {status}"}

    try:
        verified_amount = int(float(raw_amount))
    except (TypeError, ValueError):
        return {"ok": True, "ignored": "invalid verified amount"}

    if currency.upper() != "UGX" or verified_amount < payment_request.amount_ugx:
        return {"ok": True, "ignored": "amount or currency mismatch"}

    invoice = db.get(Invoice, payment_request.invoice_id)
    if not invoice:
        return {"ok": True, "ignored": "invoice not found"}

    payment_id = f"marzpay-{transaction_uuid}"[:64]
    existing_payment = db.get(Payment, payment_id)
    if existing_payment is None:
        db.add(
            Payment(
                id=payment_id,
                customer_id=payment_request.customer_id,
                amount_ugx=verified_amount,
                status="PAID",
                reference=f"MarzPay {transaction_uuid}"[:200],
                paid_at=datetime.now(timezone.utc),
            )
        )

    invoice.paid = True
    payment_request.status = "PAID"
    payment_request.transaction_uuid = transaction_uuid
    payment_request.updated_at = datetime.now(timezone.utc)
    db.commit()
    return {"ok": True, "status": "PAID"}


@app.get("/v1/opportunities", dependencies=[Depends(require_api_key)])
def list_opportunities(db: Session = Depends(db_session)):
    rows = db.scalars(select(Opportunity).order_by(Opportunity.score.desc(), Opportunity.discovered_at.desc()).limit(200)).all()
    return [
        {
            "id": row.id,
            "source": row.source,
            "sourceUrl": row.source_url,
            "title": row.title,
            "company": row.company,
            "description": row.description,
            "score": row.score,
            "estimatedValueUgx": row.estimated_value_ugx,
            "proposalDraft": row.proposal_draft,
            "status": row.status,
            "approvalStatus": row.approval_status,
            "discoveredAt": row.discovered_at,
            "lastSeenAt": row.last_seen_at,
        }
        for row in rows
    ]


@app.patch("/v1/opportunities/{opportunity_id}/approval", dependencies=[Depends(require_api_key)])
def update_opportunity_approval(opportunity_id: str, payload: OpportunityApprovalIn, db: Session = Depends(db_session)):
    allowed = {"REVIEW_REQUIRED", "APPROVED", "REJECTED"}
    value = payload.approval_status.upper()
    if value not in allowed:
        raise HTTPException(status_code=400, detail="Invalid approval status")
    row = db.get(Opportunity, opportunity_id)
    if not row:
        raise HTTPException(status_code=404, detail="Opportunity not found")
    row.approval_status = value
    db.commit()
    return {"ok": True, "approvalStatus": value}


@app.post("/v1/opportunities/scan", dependencies=[Depends(require_api_key)])
def scan_opportunities():
    from backend.hunter import run_scan

    return run_scan()
