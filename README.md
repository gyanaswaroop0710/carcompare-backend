# CarCompare

Car comparison + recommendation platform. Java Spring Boot REST API, MySQL, plain HTML/CSS/JS frontend (served by Spring Boot).

## Run (zero setup — in-memory DB)
    mvn spring-boot:run
Open http://localhost:8080

## Run with MySQL
    mysql -e "CREATE DATABASE carcompare;"
    DB_USER=root DB_PASS=yourpass mvn spring-boot:run -Dspring-boot.run.profiles=mysql
Tables are auto-created and 29 sample cars are seeded on first run.

## API
| Method | Path | Purpose |
|---|---|---|
| GET | /api/cars?q=&brand=&fuelType=&transmission=&bodyType=&minPrice=&maxPrice=&minSeats=&sort=&dir= | Browse/search/filter |
| GET | /api/cars/{id} | Car details |
| GET | /api/filters | Filter dropdown values |
| GET | /api/compare?ids=1,2,3 | Compare 2–4 cars |
| POST | /api/recommendations | `{budgetLakh, fuelType, minSeats, bodyType, profile, weights{price,mileage,performance,safety,space}}` → ranked cars with 0–100 score + reasons |
| GET/POST/DELETE | /api/favorites[/{carId}] | Saved cars (header `X-Client-Id`) |

## Login
Accounts live in the `app_user` table (BCrypt-hashed passwords). Log-ins are bearer tokens (7 days, stored hashed in `auth_token`).

| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register | `{name, email, password}` → `{token, user}` |
| POST | /api/auth/login | `{email, password}` → `{token, user}` |
| POST | /api/auth/logout | Revoke current token (needs `Authorization: Bearer …`) |
| GET | /api/auth/me | Current user |

Favorites now require login (`Authorization: Bearer <token>` instead of `X-Client-Id`). Browsing, compare and recommendations stay public.
