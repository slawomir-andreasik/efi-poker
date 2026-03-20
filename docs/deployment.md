# Deployment Guide

## Docker Compose (quickstart)

The root `compose.yaml` runs EFI Poker with pre-built images from GHCR:

```bash
# Optional: create .env with custom passwords
cp .env.example .env
# Edit .env to set JWT_SECRET, POSTGRES_PASSWORD, ADMIN_PASSWORD

docker compose up -d
```

Open [http://localhost:8080](http://localhost:8080). Default login: `admin` / `changeme`.

To use a different port:

```bash
PORT=3000 docker compose up -d
```

## Environment Variables

### Required for production

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | JWT signing secret (minimum 64 characters for HS512) |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `ADMIN_PASSWORD` | Initial admin user password |

### Optional

| Variable | Default | Description |
|----------|---------|-------------|
| `ADMIN_USERNAME` | `admin` | Admin username |
| `POSTGRES_DB` | `efipoker` | Database name |
| `POSTGRES_USER` | `efipoker` | Database user |
| `APP_URL` | `http://localhost:8080` | Public URL (used for OAuth callbacks) |
| `CORS_ORIGINS` | `http://localhost:8080` | Allowed CORS origins |
| `REGISTRATION_ENABLED` | `true` | Allow public user registration |
| `AUTH0_ENABLED` | `false` | Enable Auth0 OAuth2 login |
| `AUTH0_DOMAIN` | - | Auth0 tenant domain |
| `AUTH0_CLIENT_ID` | - | Auth0 client ID |
| `AUTH0_CLIENT_SECRET` | - | Auth0 client secret |
| `LDAP_ENABLED` | `false` | Enable corporate LDAP authentication |
| `LDAP_URL` | - | LDAP server URL (e.g. `ldap://ldap.example.com:389`) |
| `LDAP_BASE_DN` | - | Base DN (e.g. `dc=example,dc=com`) |
| `LDAP_USERS_DN` | `ou=users` | Users organizational unit (relative to base DN) |
| `LDAP_BIND_DN` | - | Service account DN for search operations |
| `LDAP_BIND_PASSWORD` | - | Service account password |
| `LDAP_USER_FILTER` | `(&(uid={0})(description=userInternal)(description=active))` | LDAP search filter (`{0}` = username) |
| `LDAP_UID_ATTRIBUTE` | `uid` | Username attribute name |
| `LDAP_MAIL_ATTRIBUTE` | `mail` | Email attribute name |
| `LDAP_ADMIN_GROUP` | - | POSIX group name for admin role detection |

## Kubernetes

### Namespace and Secret

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: efi-poker
---
apiVersion: v1
kind: Secret
metadata:
  name: efi-poker-secrets
  namespace: efi-poker
type: Opaque
stringData:
  POSTGRES_PASSWORD: "your-strong-password"
  JWT_SECRET: "your-64-char-minimum-random-string-for-hs512-jwt-signing-key"
  ADMIN_PASSWORD: "your-admin-password"
  # LDAP (optional - remove if not using corporate LDAP)
  LDAP_BIND_DN: "uid=serviceaccount,ou=accounts,dc=example,dc=com"
  LDAP_BIND_PASSWORD: "your-ldap-service-password"
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: efi-poker-config
  namespace: efi-poker
data:
  POSTGRES_HOST: "postgres"
  POSTGRES_PORT: "5432"
  POSTGRES_DB: "efipoker"
  POSTGRES_USER: "efipoker"
  ADMIN_USERNAME: "admin"
  APP_URL: "https://poker.example.com"
  CORS_ORIGINS: "https://poker.example.com"
  REGISTRATION_ENABLED: "true"
  # LDAP (optional - remove if not using corporate LDAP)
  LDAP_ENABLED: "true"
  LDAP_URL: "ldap://ldap.example.com:389"
  LDAP_BASE_DN: "dc=example,dc=com"
  LDAP_USERS_DN: "ou=users"
  LDAP_USER_FILTER: "(&(uid={0})(description=userInternal)(description=active))"
  LDAP_UID_ATTRIBUTE: "uid"
  LDAP_MAIL_ATTRIBUTE: "mail"
  LDAP_ADMIN_GROUP: "efipoker-admins"
```

### PostgreSQL StatefulSet

For production, consider using a managed database (AWS RDS, Google Cloud SQL, etc.) instead.

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: efi-poker
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:17-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: efi-poker-config
                  key: POSTGRES_DB
            - name: POSTGRES_USER
              valueFrom:
                configMapKeyRef:
                  name: efi-poker-config
                  key: POSTGRES_USER
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: efi-poker-secrets
                  key: POSTGRES_PASSWORD
          volumeMounts:
            - name: pgdata
              mountPath: /var/lib/postgresql/data
          resources:
            limits:
              memory: 512Mi
            requests:
              memory: 256Mi
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "efipoker"]
            initialDelaySeconds: 5
            periodSeconds: 10
  volumeClaimTemplates:
    - metadata:
        name: pgdata
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: efi-poker
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
  clusterIP: None
```

### Backend Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: efi-poker
spec:
  replicas: 1
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
        - name: backend
          image: ghcr.io/slawomir-andreasik/efi-poker/backend:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: efi-poker-config
            - secretRef:
                name: efi-poker-secrets
          resources:
            limits:
              memory: 1Gi
            requests:
              memory: 512Mi
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080    # use 9091 with prod profile (see Notes below)
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080    # use 9091 with prod profile (see Notes below)
            initialDelaySeconds: 60
            periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: efi-poker
spec:
  selector:
    app: backend
  ports:
    - port: 8080
```

### Frontend Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: efi-poker
spec:
  replicas: 1
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
        - name: frontend
          image: ghcr.io/slawomir-andreasik/efi-poker/frontend:latest
          ports:
            - containerPort: 8080
          resources:
            limits:
              memory: 128Mi
            requests:
              memory: 64Mi
          readinessProbe:
            httpGet:
              path: /
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: efi-poker
spec:
  selector:
    app: frontend
  ports:
    - port: 8080
```

### Ingress

The backend uses `forward-headers-strategy: NATIVE`, so the reverse proxy **must** pass `X-Forwarded-Proto: https` to backend pods - otherwise Spring Boot thinks requests are HTTP and generates a redirect loop.

#### SSL redirect and external load balancers

If an external load balancer (e.g. AWS ALB, Cloudflare, HAProxy) terminates TLS before the ingress controller, set `ssl-redirect` to `"false"` - otherwise you get a redirect loop (LB strips TLS, ingress sees HTTP, redirects to HTTPS, LB strips TLS again, repeat).

**How to diagnose:** `curl -D - https://your-domain/` returns `302` with `location: https://your-domain:443/`.

#### Nginx

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: efi-poker
  namespace: efi-poker
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  ingressClassName: nginx
  rules:
    - host: poker.example.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend
                port:
                  number: 8080
  tls:
    - hosts:
        - poker.example.com
      secretName: efi-poker-tls
```

> Nginx ingress controller passes `X-Forwarded-Proto` to backends by default - no extra annotation needed.

#### HAProxy

Same ingress spec as above, but with HAProxy-specific annotations:

```yaml
annotations:
  haproxy.org/ssl-redirect: "true"  # "false" if external LB terminates TLS
  ingress.kubernetes.io/config-backend: "http-request set-header X-Forwarded-Proto https"
```

Use `ingressClassName: haproxy`. Note: `ssl-redirect` fires in the HAProxy frontend (before backend), so `config-backend` alone does not prevent the redirect loop.

## Notes

### Actuator / management endpoints

The backend exposes Spring Boot Actuator at `/actuator/health` for health checks. In the default profile, actuator runs on the same port as the application (8080).

When using the `prod` profile (`SPRING_PROFILES_ACTIVE=prod`), the management server runs on a **separate port 9091**. Update your Kubernetes probes accordingly:

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 9091    # management port in prod profile
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 9091
```

Never expose `/actuator` through Ingress - Kubernetes probes access pods directly and don't need Ingress routing.

### Custom domain

Update these variables to match your domain:
- `APP_URL` - full URL including protocol (e.g. `https://poker.example.com`)
- `CORS_ORIGINS` - same as APP_URL

### Auth0 integration

1. Create an Auth0 application (Regular Web Application)
2. Set callback URL to `{APP_URL}/api/v1/auth/oauth2/callback`
3. Configure environment variables: `AUTH0_ENABLED=true`, `AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, `AUTH0_CLIENT_SECRET`

### LDAP integration

1. Set `LDAP_ENABLED=true` and provide LDAP server details
2. The service account (`LDAP_BIND_DN`) needs search access to the users OU
3. User authentication flow: search by filter -> bind with user credentials -> check admin group
4. LDAP users are auto-provisioned on first login (authProvider=LDAP, no local password)
5. Admin role syncs bidirectionally on each login based on LDAP group membership
6. LDAP users cannot change their password through the app
7. LDAP can run alongside password and Auth0 authentication

### Changing admin password

The admin user is created on first startup with the configured `ADMIN_PASSWORD`. To change it later, use the admin API or update the user directly in the database.
