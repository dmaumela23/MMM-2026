# Demonstration checklist

**Before recording**
- [ ] PostgreSQL running; `python seed.py` done; `uvicorn app.main:app --host 0.0.0.0 --port 8000` running
- [ ] Phone: same Wi-Fi as the PC, **or** USB plus `adb reverse tcp:8000 tcp:8000` (see README)
- [ ] `/health` opens in the phone browser
- [ ] Firebase project created and `google-services.json` in `android/app/`
- [ ] Phone notifications allowed for the app (Android 13+ asks inside Settings)

**Recording script**
1. [ ] Launch the app: Splash, then Login
2. [ ] **Register** a new Business account (shows validation errors first: empty fields, weak password)
3. [ ] **Home** dashboard: sector tiles, search, featured items
4. [ ] **Marketplace**: products, services, search, filters (price, rating, category)
5. [ ] Open a **product** and a **service** (details)
6. [ ] **Place an order**: add a product (quantity 2) and a service, add notes, submit
7. [ ] **Orders** and **Order details**: server-calculated total
8. [ ] Show the **database**: new user (hashed password), order, order items, reduced stock
9. [ ] **Energy** dashboard: "Prototype: simulated data" banner, usage, plan, estimated cost, **chart**; Usage (7/30/90 days) and Reports
10. [ ] **Profile** and **Settings**: edit the name; switch to **dark mode**; change password
11. [ ] Log in as the **Provider/Business** seed account: **Incoming** tab, advance a line (Accept, Start work, Mark complete)
12. [ ] As the **Customer**, show that the order status changed
13. [ ] **Firebase**: Settings, "Copy device token" (debug build), send a Console test message, show it arriving
14. [ ] **Error handling**: stop the server and pull to refresh (friendly message, "Showing saved data"); wrong password; Customer tries to create a product (403 message)
15. [ ] **Tests**: run `.\gradlew.bat testDebugUnitTest` and `pytest -v` (both green)
16. [ ] Show Swagger at `/docs` and the Retrofit calls in Logcat
