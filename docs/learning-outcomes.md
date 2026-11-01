# How MMM meets the learning outcomes

| Learning outcome | Implementation | Evidence to show |
|---|---|---|
| Use a RESTful API in an Android application | Retrofit `ApiService` calls the FastAPI backend: correct HTTP verbs, JSON, status codes (200/201/400/401/403/404/409), JWT bearer authentication | Marketplace and orders load from the API; Swagger page at `/docs`; the app's request log (Logcat, tag `OkHttp`) |
| The REST API connects to a database | FastAPI, SQLAlchemy and PostgreSQL: 8 tables with keys, indexes, constraints | Register in the app, then show the new row in `users` (hash only) and 90 rows in `energy_usage` |
| Use an external library | **Retrofit + OkHttp** (networking), **MPAndroidChart** (energy charts), Coil (image URLs) | `ApiService.kt`, `RetrofitClient.kt`, `EnergyChart.kt`; the charts on screen |
| Connect to an appropriate SDK | **Firebase Cloud Messaging**: device token registered with the server, Console test notification received | Token stored in `user_settings.fcm_token`; notification on the emulator |
| Detailed unit testing | JUnit 4 and MockK on Android (validators, calculators, repositories, ViewModels, mappers, token storage); pytest on the backend | Green test runs in Android Studio and in the terminal |
| Fully working prototype that compiles and runs | One build for emulator and phone; setup guide in the README | Clean clone, build, run |
| Demonstrated on a physical phone | LAN IP or `adb reverse` (see the README) | Screen recording on the phone |
| User registration, login, secure password storage | BCrypt hashes (never plain text); JWT; the token on the device is encrypted with an Android Keystore key | `password_hash` column; backend security tests |
| User settings | Name, phone, email, password, notification preference, theme (light/dark/system) | Switch to dark mode and show it persists |
| Features from the Part 1 research | Shopify: product catalogue, product CRUD, orders, sector modularity. Fiverr: service listings, categories, ratings, delivery time. Upwork: provider requests and a per-line status workflow | Provider or Business account advancing an incoming request |

## Why each technology was chosen

| Technology | Purpose in MMM |
|---|---|
| Kotlin | The Android language; null-safety and coroutines keep the code short and safe |
| Android Studio | IDE, emulator and build tooling |
| Jetpack Compose + Material 3 | Declarative UI with one reusable component library and a light/dark theme |
| Navigation Compose | One `NavHost` with typed routes, back stack handling and a bottom bar |
| ViewModel + StateFlow | Screen state survives rotation; the UI only observes state |
| Retrofit + OkHttp | Turns a Kotlin interface into HTTP calls; the interceptor adds the JWT and detects expired sessions |
| kotlinx.serialization | JSON parsing; money travels as decimal strings and is held as `BigDecimal` |
| Room | A small read-only cache so the Marketplace and Orders still show data when offline. PostgreSQL is always the source of truth |
| DataStore + Android Keystore | Preferences, and the JWT stored encrypted with a key that never leaves the device's secure storage |
| MPAndroidChart | Line and bar charts for the simulated energy data |
| Coil | Loads product images from URLs |
| Firebase Cloud Messaging | Push notification infrastructure (token registration and Console test messages) |
| JUnit, MockK | Unit tests with mocked dependencies |
| FastAPI | REST framework with automatic Swagger documentation |
| PostgreSQL + SQLAlchemy | Relational database and ORM with parameterised queries |
| Pydantic | Input validation on the server |
| JWT (PyJWT) | Stateless authentication with an expiry |
| BCrypt | One-way, salted password hashing (preferred over reversible encryption: nobody, including the server, can recover a password) |
| pytest | Backend tests against a separate test database |
