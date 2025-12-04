# Energy Management System - Microservices Architecture

Acest proiect reprezintă un sistem distribuit de gestionare a energiei, construit pe o arhitectură de microservicii, utilizând containere Docker pentru orchestrare.

---

## 🏗️ Arhitectură & Tehnologii

Sistemul este compus din următoarele module interconectate:

### 1. Frontend
* **Tehnologie:** React + Vite
* **Rol:** Interfața cu utilizatorul (Client & Administrator).
* **Port Acces:** `http://localhost:5174`

### 2. Backend (Microservicii Java Spring Boot)
Fiecare microserviciu are propria bază de date și responsabilități distincte.

| Serviciu | Port Local (Host) | Port Intern (Container) | Descriere |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `8088` | `8084` | Punctul unic de intrare. Redirecționează cererile și gestionează securitatea. |
| **User Service** | `8085` | `8082` | CRUD utilizatori, gestionare administratori/clienți. |
| **Device Service** | `8087` | `8081` | CRUD dispozitive, mapare dispozitiv-utilizator. |
| **Auth Service** | `8086` | `8083` | Autentificare, generare și validare token JWT. |
| **Monitoring Service**| `8090` | `8090` | Monitorizare consum, grafice, consumator RabbitMQ. |

### 3. Baze de Date (PostgreSQL)
Fiecare serviciu are o instanță dedicată (sau bază de date separată) pentru izolare.

| Bază de Date | Port Local (Host) | Serviciu Asociat |
| :--- | :--- | :--- |
| `user_db` | `5436` | User Service |
| `device_db` | `5435` | Device Service |
| `auth_db` | `5437` | Auth Service |
| `monitoring_db`| `5438` | Monitoring Service |

### 4. Messaging & Infrastructure
* **RabbitMQ** (Port `5672` / `15672` UI): Broker de mesaje pentru comunicarea asincronă a datelor de la senzor către serviciul de monitorizare.
* **Traefik** (Port `8080`): Reverse proxy / Load balancer (opțional, configurat în stack).

---

## 🚀 Instalare și Rulare

Sistemul este complet containerizat. Pentru a-l porni, ai nevoie de **Docker Desktop** instalat.

1.  **Navighează în folderul cu configurația Docker:**
    ```bash
    cd trefik_config
    ```

2.  **Pornirea stack-ului:**
    ```bash
    docker-compose up -d --build
    ```
    *Comanda va construi imaginile pentru fiecare microserviciu și va porni containerele în ordinea corectă (așteptând bazele de date și RabbitMQ).*

3.  **Verificare status:**
    ```bash
    docker-compose ps
    ```

---

## 🌐 API Gateway & Rutare

Toate cererile din Frontend trebuie trimise către **API Gateway** pe portul **8088**. Acesta le rutează intern către serviciile corespunzătoare.

| Rută (Path) | Destinație Internă | Exemplu URL Acces |
| :--- | :--- | :--- |
| `/auth/**` | `auth-service:8083` | `POST http://localhost:8088/auth/login` |
| `/users/**` | `user-service:8082` | `GET http://localhost:8088/users` |
| `/device/**` | `device-service:8081` | `GET http://localhost:8088/device` |
| `/monitoring/**`| `monitoring-service:8090`| `GET http://localhost:8088/monitoring/{id}` |

**Notă:** Gateway-ul se ocupă de forward-area header-ului `Authorization` și a parametrilor de query (ex: `?date=...`).

---

## 📡 Fluxul de Date (Senzori & Monitorizare)

1.  **Simulatorul** (aplicație separată) citește fișierul `sensor.csv`.
2.  Datele sunt trimise către coada **RabbitMQ**.
3.  **Monitoring Service** ascultă coada, preia datele și le salvează în `monitoring_db`.
4.  Dacă un consum depășește limita maximă setată pentru dispozitiv, se calculează un warning (logica de business).
5.  **Frontend-ul** apelează `/monitoring` prin Gateway pentru a afișa graficul de consum în timp real.

## 🛠️ Configurare Variabile de Mediu

Configurările principale se află în `docker-compose.yml`. 
Dacă dorești să schimbi porturile sau credențialele, modifică secțiunea `environment` a serviciului vizat.

Exemplu (Gateway):
```yaml
  gateway-service:
    environment:
      AUTH_URL: http://auth-service:8083
      USER_URL: http://user-service:8082
      DEVICE_URL: http://device-service:8081