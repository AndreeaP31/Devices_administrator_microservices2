Dispozitive Administrator — arhitectură microservicii (Spring Boot + React)

Descriere pe scurt
- Monorepo pentru o aplicație de administrare a dispozitivelor, organizată în microservicii.
- Backend: Spring Boot (Java) împărțit în servicii: gateway, auth, user, device.
- Frontend: React (folderul `frontend`).
- Autentificare și autorizare pe bază de JWT, propagat prin API Gateway.

Cuprins
- Structura proiectului
- Arhitectură și flux de autentificare/autorizare
- Reguli de acces (roluri)
- Cerințe de rulare
- Pornire rapidă (development)
- Exemple de apeluri API
- Configurare JWT și CORS
- Bune practici și depanare rapidă
- Notă Traefik și roadmap

Structura proiectului (nivel 1–2)
```
api_gateaway_microservice/
  demo/
auth_microservice/
  demo/
user_microservice/
  demo/
device_microservice/
  demo/
frontend/
  src/
  public/
trefik_config/
  dynamic/
  logs/
```

Rolul fiecărui modul
- `api_gateaway_microservice` — API Gateway (Spring Boot). Aplica reguli CORS, autentifică și extrage atributele din JWT, aplică reguli de rol și rutează cererile către microservicii. Filtre cheie: `JwtAuthenticationFilter`, `RoleFilter`, `CachedBodyHttpServletRequest`.
- `auth_microservice` — autentificare, emitere token JWT (include secret/durată token configurabile).
- `user_microservice` — management utilizatori (creare, listare, update, etc.). Rutele încep cu `/users` prin gateway.
- `device_microservice` — management dispozitive; include rute filtrate per utilizator.
- `frontend` — aplicație React pentru interfața utilizator.
- `trefik_config` — fișiere pentru Traefik (dacă se rulează cu reverse-proxy; opțional în dev local).

Arhitectură și flux de autentificare/autorizare
1) Clientul apelează Auth Service prin Gateway: `/api/auth/...`.
2) Auth Service validează credentialele și returnează un JWT (conține cel puțin `sub`/`userId` și `role`).
3) La cererile ulterioare, clientul trimite `Authorization: Bearer <JWT>`.
4) În Gateway:
   - `JwtAuthenticationFilter` validează tokenul, atașează `role` și `userId` pe request.
   - `RoleFilter` aplică regulile de acces în funcție de cale și rol.
   - Preflight CORS (`OPTIONS`) este bypass (nu se aplică verificare de rol), pentru a permite apeluri browser cross-origin.

Reguli de acces (din `RoleFilter`)
- Public: toate rutele sub `/auth` sunt publice (ex.: `POST /api/auth/login`).
- Intern: `POST /api/users` este permis fără rol (apel intern pentru provisionare între servicii).
- Administrare utilizatori: rutele care încep cu `/users` necesită `role=ADMIN`.
- Dispozitive: rutele care încep cu `/device`:
  - `ADMIN` are acces complet.
  - `CLIENT` poate accesa doar resursa proprie cu pattern-ul:
    `/api/device/{userId}/for-user/devices`, unde `{userId}` trebuie să corespundă cu `userId` din JWT.

Cerințe de rulare
- Java 17+ (JDK) pentru backend (Spring Boot).
- Maven 3.9+ (sau wrapper-ul `mvnw` acolo unde este prezent).
- Node.js 18+ și npm/yarn pentru frontend.
- (Opțional) PostgreSQL sau alte baze de date, conform configurației fiecărui serviciu.

Pornire rapidă (development)
Rulați serviciile în terminale separate, din folderele `demo` ale fiecărui microserviciu.

- API Gateway:
  ```
  cd api_gateaway_microservice\demo
  mvn spring-boot:run
  ```

- Auth Service:
  ```
  cd auth_microservice\demo
  mvn spring-boot:run
  ```

- User Service:
  ```
  cd user_microservice\demo
  mvn spring-boot:run
  ```

- Device Service:
  ```
  cd device_microservice\demo
  mvn spring-boot:run
  ```

Porturile pot fi configurate în fișierele `application.properties`/`application.yml` din fiecare serviciu. Implicit, Gateway-ul expune rutele către clientul extern cu prefixul `/api`.

Frontend (React)
```
cd frontend
npm install
npm start
```

Exemple de apeluri API (prin Gateway)
- Login (public):
  ```
  POST /api/auth/login
  Content-Type: application/json
  {
    "email": "user@example.com",
    "password": "parola"
  }
  ```

- Listare utilizatori (ADMIN):
  ```
  GET /api/users
  Authorization: Bearer <JWT-ADMIN>
  ```

- Dispozitivele utilizatorului curent (CLIENT):
  ```
  GET /api/device/{userId}/for-user/devices
  Authorization: Bearer <JWT-CLIENT-cu-userId={userId}>
  ```

Configurare JWT și CORS
- Configurați secretul JWT și expirarea în Auth Service.
- Gateway-ul gestionează CORS; preflight-ul `OPTIONS` este permis automat (nu se blochează pe rol).
- Clientul trebuie să trimită `Authorization: Bearer <token>` la rutele protejate.

Bune practici
- Direcționați toate apelurile externe prin API Gateway (prefix `/api`).
- Pentru apeluri din browser, verificați antetele CORS (Origin, Methods, Headers) în răspuns.
- În dezvoltare, urmăriți log-urile Gateway-ului: filtrele scriu mesaje utile (ex.: rol, userId, calea solicitată, bypass OPTIONS).

Depanare rapidă
- 403 la rutele `/users`: verificați rolul — este necesar `ADMIN`.
- 403 la `/device/.../for-user/devices`: `{userId}` din URL trebuie să corespundă `userId` din JWT dacă rolul este `CLIENT`.
- 401/invalid token: verificați semnătura JWT, expirarea și prezența header-ului `Authorization`.
- Probleme CORS în browser: confirmați că preflight `OPTIONS` primește 200 și că metoda/antetele reale sunt permise.

Notă Traefik
- Folderul `trefik_config` conține fișiere utile dacă se rulează infrastructura cu Traefik (Docker/Kubernetes). Dacă nu utilizați Traefik, îl puteți ignora în dezvoltare locală.

Roadmap (scurt)
- Teste end-to-end pentru fluxurile de login și acces controlat la `/device`.
- Scripturi Docker Compose pentru rularea unificată a tuturor serviciilor.
- Documentarea completă a contractelor API (OpenAPI/Swagger) pentru fiecare microserviciu.

Contribuții
- Deschis contribuțiilor. Vă rugăm să creați issue-uri/PR-uri pentru bugfix-uri și îmbunătățiri.
