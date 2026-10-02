"""Seed the database with demo data. Safe to run repeatedly (idempotent).

Usage (from the backend/ folder, with .env configured):
    python seed.py

The demo password is read from SEED_DEMO_PASSWORD (never hard-coded).
"""
import sys
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.hashing import hash_password
from app.config import settings
from app.database import SessionLocal, init_db
from app.enums import Sector, UserRole
from app.models import EnergyPlan, Product, Service, User, UserSettings
from app.services import energy_service
from app.utils.validation import validate_password_strength

# (email, full name, phone, role, energy plan name)
DEMO_USERS = (
    ("demo.customer@example.com", "Demo Customer", "0821110001", UserRole.CUSTOMER, "Home Standard"),
    ("demo.provider@example.com", "Demo Provider", "0821110002", UserRole.PROVIDER, "Home Standard"),
    ("demo.business@example.com", "Maumela Magnum Demo Business", "0821110003", UserRole.BUSINESS, "Business Power"),
)

# (name, description, price ZAR, category, sector, stock, rating, image seed)
DEMO_PRODUCTS = (
    ("Business Website Template Pack", "Ten responsive website templates for small businesses, with source files.", "899.00", "Software", Sector.DIGITAL, 999, 4.6, "website-templates"),
    ("Cloud Backup Licence (1 Year)", "Encrypted cloud backup for up to 500 GB of business data.", "1199.00", "Software", Sector.DIGITAL, 500, 4.4, "cloud-backup"),
    ("Inventory Manager Pro Licence", "Stock control and reporting software for growing businesses.", "2499.00", "Software", Sector.DIGITAL, 200, 4.7, "inventory-manager"),
    ("Cybersecurity Starter Toolkit", "Password manager, endpoint protection and staff training material.", "1750.00", "Security", Sector.DIGITAL, 150, 4.3, "cyber-toolkit"),
    ("Analytics Dashboard Add-on", "Sales and customer dashboards that plug into common accounting tools.", "650.00", "Software", Sector.DIGITAL, 300, 4.1, "analytics-addon"),
    ("Digital Marketing Asset Bundle", "Social media templates, banners and email layouts.", "450.00", "Design", Sector.DIGITAL, 400, 4.5, "marketing-bundle"),
    ("550W Monocrystalline Solar Panel", "High-efficiency panel suitable for home and small-business rooftops.", "2899.00", "Solar", Sector.ENERGY, 40, 4.8, "solar-panel"),
    ("5 kWh LiFePO4 Battery", "Wall-mounted lithium iron phosphate battery for backup power.", "14999.00", "Storage", Sector.ENERGY, 12, 4.9, "lifepo4-battery"),
    ("5 kW Hybrid Inverter", "Grid-tie and off-grid capable hybrid inverter with Wi-Fi monitoring.", "12499.00", "Inverters", Sector.ENERGY, 8, 4.6, "hybrid-inverter"),
    ("1 kWh Portable Power Station", "Portable battery station for outages, camping and site work.", "9999.00", "Storage", Sector.ENERGY, 15, 4.5, "power-station"),
    ("LED Lighting Retrofit Kit", "Twenty energy-saving LED downlights with drivers.", "349.00", "Efficiency", Sector.ENERGY, 120, 4.2, "led-kit"),
    ("Solar Geyser Controller", "Timer and temperature controller for solar water heating. Currently out of stock.", "1299.00", "Efficiency", Sector.ENERGY, 0, 4.0, "geyser-controller"),
)

# (owner key, name, description, price ZAR, category, sector, delivery days, rating)
DEMO_SERVICES = (
    ("provider", "Website Design & Development", "Custom responsive website designed and built for your business.", "7500.00", "Web Development", Sector.SERVICE, 14, 4.8),
    ("provider", "Mobile App Consultation", "Two-hour workshop to scope and plan your mobile app.", "1800.00", "Consulting", Sector.SERVICE, 3, 4.6),
    ("provider", "IT Support Retainer (Monthly)", "Remote IT support with a guaranteed response time.", "2200.00", "IT Support", Sector.SERVICE, 2, 4.5),
    ("provider", "Brand Identity Package", "Logo, colour palette and brand guidelines.", "4200.00", "Design", Sector.SERVICE, 10, 4.7),
    ("provider", "Solar Installation Assessment", "On-site assessment and written recommendation for a solar system.", "1200.00", "Installation", Sector.ENERGY, 5, 4.9),
    ("provider", "Solar Panel Installation Labour", "Professional mounting and wiring of a solar array.", "6500.00", "Installation", Sector.ENERGY, 7, 4.7),
    ("business", "Business Process Audit", "Review of your workflows with a prioritised improvement plan.", "5500.00", "Consulting", Sector.SERVICE, 10, 4.4),
    ("business", "Data Analytics Setup", "Connect your data sources and build management dashboards.", "3800.00", "Data", Sector.SERVICE, 8, 4.5),
    ("business", "Energy Efficiency Audit", "Site audit that identifies where a building wastes electricity.", "1500.00", "Energy Consulting", Sector.ENERGY, 4, 4.6),
    ("business", "Battery Backup Sizing Consultation", "Calculate the right battery capacity for your load profile.", "950.00", "Energy Consulting", Sector.ENERGY, 2, 4.3),
)


def load_demo_password() -> str:
    password = (settings.seed_demo_password or "").strip()
    if not password:
        sys.exit("SEED_DEMO_PASSWORD is not set. Add it to backend/.env (see .env.example) and retry.")
    try:
        return validate_password_strength(password)
    except ValueError as exc:
        sys.exit(f"SEED_DEMO_PASSWORD is not acceptable: {exc}")


def seed_users(db: Session, password: str) -> tuple[dict[str, User], int]:
    plans = {plan.name: plan for plan in db.scalars(select(EnergyPlan))}
    users: dict[str, User] = {}
    created = 0
    for email, full_name, phone, role, plan_name in DEMO_USERS:
        user = db.scalar(select(User).where(User.email == email))
        if user is None:
            user = User(
                full_name=full_name,
                email=email,
                phone=phone,
                password_hash=hash_password(password),  # only the hash is stored
                role=role.value,
                energy_plan_id=plans[plan_name].id,
                settings=UserSettings(),
            )
            db.add(user)
            created += 1
        db.flush()
        # 90 days of SIMULATED usage; ON CONFLICT DO NOTHING keeps repeated runs harmless.
        energy_service.generate_usage_history(db, user.id)
        users[role.value.lower()] = user
    return users, created


def seed_products(db: Session, seller: User) -> int:
    created = 0
    for name, description, price, category, sector, stock, rating, image_seed in DEMO_PRODUCTS:
        exists = db.scalar(select(Product.id).where(Product.seller_id == seller.id, Product.name == name))
        if exists is not None:
            continue
        db.add(
            Product(
                seller_id=seller.id,
                name=name,
                description=description,
                price=Decimal(price),
                category=category,
                sector=sector.value,
                image_url=f"https://picsum.photos/seed/mmm-{image_seed}/600/400",
                stock_quantity=stock,
                availability=True,
                rating=rating,
            )
        )
        created += 1
    return created


def seed_services(db: Session, owners: dict[str, User]) -> int:
    created = 0
    for owner_key, name, description, price, category, sector, days, rating in DEMO_SERVICES:
        owner = owners[owner_key]
        exists = db.scalar(select(Service.id).where(Service.provider_id == owner.id, Service.name == name))
        if exists is not None:
            continue
        db.add(
            Service(
                provider_id=owner.id,
                name=name,
                description=description,
                price=Decimal(price),
                category=category,
                sector=sector.value,
                availability=True,
                rating=rating,
                delivery_days=days,
            )
        )
        created += 1
    return created


def main() -> None:
    password = load_demo_password()
    init_db()
    with SessionLocal() as db:
        energy_service.ensure_default_plans(db)
        users, users_created = seed_users(db, password)
        products_created = seed_products(db, users["business"])
        services_created = seed_services(db, users)
        db.commit()

    print("Seed complete.")
    print(f"  users created:    {users_created} (existing users are left unchanged)")
    print(f"  products created: {products_created}")
    print(f"  services created: {services_created}")
    print("  energy plans:     3 (simulated data, 90 days per user)")
    print("Demo accounts (password = the SEED_DEMO_PASSWORD you configured):")
    for email, _name, _phone, role, _plan in DEMO_USERS:
        print(f"  {role.value:<9} {email}")


if __name__ == "__main__":
    main()