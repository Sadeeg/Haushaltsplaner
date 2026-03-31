# Haushaltsplaner

> Faire Aufgabenverteilung im Haushalt

## 🎯 Überblick

Haushaltsplaner ist eine PWA (Progressive Web App) zur fairen Verteilung von Haushaltsaufgaben. Die App erinnert an Aufgaben, berücksichtigt Exclusion Rules (z.B. "Wer kocht, macht nicht Abwasch") und motiviert durch ein Punktesystem.

## ✨ Features

- ✅ Aufgaben mit Häufigkeit (täglich, wöchentlich, zwei-wöchentlich, monatlich)
- ✅ Exclusion Rules ("Kochen ≠ Abwasch" - same person)
- ✅ Fairer Rotations-Algorithmus
- ✅ Telegram Benachrichtigungen (tägliche Erinnerung um 08:00 Uhr)
- ✅ Punktesystem & Leaderboard
- ✅ Skip & Verschieben von Aufgaben
- ✅ PWA - funktioniert offline
- ✅ Nextcloud OAuth Anmeldung

## 🛠 Tech Stack

| Komponente | Technologie |
|-----------|-------------|
| Frontend | Angular 17 (PWA) |
| Backend | Spring Boot 3.2 (Java 17) |
| Datenbank | PostgreSQL 15 |
| Proxy | Nginx |
| Bot | Node.js + Telegram API |
| Auth | Nextcloud OAuth / OpenID Connect |

## 🚀 Quick Start

### Voraussetzungen

- Docker & Docker Compose
- Node.js 20+ (für lokale Entwicklung)
- Java 17+ (für Backend Entwicklung)

### 1. Repository klonen

```bash
git clone git@github.com:Sadeeg/Haushaltsplaner.git
cd Haushaltsplaner
```

### 2. Umgebungsvariablen

Erstelle eine `.env` Datei:

```bash
# Database
DB_PASSWORD=your_secure_password

# Telegram Bot (von @BotFather)
TELEGRAM_BOT_TOKEN=your_telegram_bot_token

# Nextcloud OAuth (optional)
OAUTH_CLIENT_ID=your_client_id
OAUTH_CLIENT_SECRET=your_client_secret
NEXTCLOUD_ISSUER_URI=https://your-nextcloud.com
```

### 3. Mit Docker Compose starten

```bash
# Frontend bauen
cd frontend
npm install
npm run build
cd ..

# Alle Services starten
docker-compose up -d
```

Die App ist dann erreichbar unter:
- **Web:** http://localhost
- **API:** http://localhost/api

### 4. Lokale Entwicklung

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm start
```

**Telegram Bot (lokal):**
```bash
cd telegram-bot
npm start
```

## 📁 Projektstruktur

```
Haushaltsplaner/
├── backend/                 # Spring Boot Backend
│   ├── src/
│   │   ├── main/java/      # Java Quellcode
│   │   └── test/           # Tests (inkl. Cucumber BDD)
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Angular PWA
│   ├── src/
│   │   ├── app/            # Angular Komponenten
│   │   └── environments/
│   ├── package.json
│   └── angular.json
├── telegram-bot/           # Telegram Notification Bot
│   └── src/
├── docker-compose.yml      # Alle Services
├── nginx.conf              # Reverse Proxy Config
└── README.md
```

## 🧪 Tests

### Backend Tests

```bash
cd backend

# Unit & Integration Tests
./mvnw test

# Cucumber BDD Tests
./mvnw test -Dcucumber.filter.tags="@bdd"

# Coverage Report
./mvnw test jacoco:report
```

### Frontend Tests

```bash
cd frontend
npm test
```

## 🔐 Sicherheit

- Alle API Endpoints erfordern Authentifizierung (außer `/api/health`)
- OAuth 2.0 mit Nextcloud als Identity Provider
- Telegram Verknüpfung via Einmal-Code
- Keine Passwörter gespeichert

## 📱 PWA Installation

1. Öffne die App im Browser
2. Bei Chrome: Klick auf "Installieren" in der Adressleiste
3. Bei iOS: "Teilen" → "Zum Home Screen hinzufügen"

## 🔔 Telegram Integration

1. Öffne die App → Profil
2. Klicke auf "Telegram verknüpfen"
3. Öffne den Bot &#64;HaushaltsplanerBot in Telegram
4. Sende `/start` und den Code aus der App

## 📊 API Dokumentation

### Endpoints

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/api/health` | Health Check |
| GET | `/api/users/me?username=X` | Aktuellen User abrufen |
| POST | `/api/users/{id}/telegram/generate-code` | Telegram Code generieren |
| POST | `/api/users/telegram/link?code=X&telegramChatId=Y` | Telegram verknüpfen |
| GET | `/api/tasks/user/{id}` | Aufgaben für User |
| GET | `/api/tasks/household/{id}/today` | Heutige Aufgaben |
| POST | `/api/tasks/{id}/complete` | Aufgabe erledigen |
| POST | `/api/tasks/{id}/skip` | Aufgabe überspringen |
| POST | `/api/tasks/{id}/move` | Aufgabe verschieben |
| GET | `/api/tasks/household/{id}/leaderboard` | Punktestand |
| GET | `/api/templates/household/{id}` | Aufgaben-Vorlagen |
| POST | `/api/templates/household/{id}` | Vorlage erstellen |
| GET | `/api/rules/household/{id}` | Exclusion Rules |
| POST | `/api/rules/household/{id}` | Rule erstellen |

## 🧩 Exclusion Rules

Beispiel: "Wer kocht, macht nicht Abwasch"

```json
{
  "taskATemplateId": 1,
  "taskBTemplateId": 2,
  "ruleType": "MUTUAL"
}
```

## 📈 Punkte System

- Jede erledigte Aufgabe gibt Punkte (standard: 1 Punkt)
- Übersprungene Aufgaben geben 0 Punkte
- Leaderboard zeigt Rangliste nach Punkten

## 🔄 Rotations-Algorithmus

1. Aufgaben werden basierend auf Häufigkeit geplant
2. Exclusion Rules werden berücksichtigt
3. Round-Robin zwischen Haushaltsmitgliedern
4. Bei Skip → nächste Person übernimmt

## 📄 Lizenz

MIT License

## 🤝 Beitrag

Pull Requests willkommen! Bitte erstelle Issues für Bugs und Feature-Requests.

---

Entwickelt mit ❤️ für faire Haushaltsarbeit
