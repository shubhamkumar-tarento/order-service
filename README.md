# order-service

Simple order management REST API on Spring Boot 4 / Java 21. Orders live in an
**in-memory `ConcurrentHashMap`** - no database, no external dependency - so the
service is easy to build, test, containerise and deploy. State resets on restart.

## API

Base path `/api/orders`.

| Method | Path                              | Purpose                            |
| ------ | --------------------------------- | ---------------------------------- |
| GET    | `/api/orders`                     | List all orders                    |
| GET    | `/api/orders?status=CREATED`      | Filter by status                   |
| GET    | `/api/orders/{id}`                | Fetch one order (404 if missing)   |
| POST   | `/api/orders`                     | Create an order (201 + `Location`) |
| PUT    | `/api/orders/{id}`                | Update the editable fields         |
| PATCH  | `/api/orders/{id}/status?value=X` | Move the order to a new status     |
| DELETE | `/api/orders/{id}`                | Delete an order (204)              |
| GET    | `/api/orders/stats`               | Order count + available statuses   |

Actuator: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/info`, `/actuator/metrics`.

### Status flow

```
CREATED ──> CONFIRMED ──> SHIPPED ──> DELIVERED
   │            │
   └──────> CANCELLED <──┘
```

Anything else returns **409 Conflict**. `DELIVERED` and `CANCELLED` are terminal -
they cannot be edited or moved on.

### Example

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Shubham","product":"Monitor","quantity":3,"price":249.99}'

curl -X PATCH "http://localhost:8080/api/orders/1/status?value=CONFIRMED"
```

`id`, `status`, `createdAt`, `updatedAt` and `totalAmount` are server-managed and
ignored on input. `customerName`, `product`, `quantity` (>= 1) and `price` (> 0)
are validated - a bad body returns 400.

## Run locally

```bash
mvn clean package
java -jar target/order-service.jar

# port 8080 busy? pick another:
java -jar target/order-service.jar --server.port=18080
```

Set `order.seed-sample-data=false` (or env `ORDER_SEEDSAMPLEDATA=false`) to start empty.

## Docker

```bash
docker build -t order-service:latest .
docker run --rm -p 8080:8080 order-service:latest
```

Multi-stage build: Maven image compiles the jar, `eclipse-temurin:21-jre-alpine`
runs it as non-root uid 1001.

## Kubernetes

```bash
kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml
kubectl rollout status deployment/order-service
curl http://localhost:30080/api/orders          # NodePort 30080
```

Two replicas, probes wired to the actuator liveness/readiness groups, read-only
root filesystem with an `emptyDir` at `/tmp` for Tomcat. On a local cluster build
the image into the node's daemon first (`eval $(minikube docker-env)` or
`kind load docker-image order-service:latest`) since `imagePullPolicy: IfNotPresent`.

Because the store is in-memory, **each replica has its own orders** - an order
created on pod A is invisible on pod B. That is fine for smoke-testing a pipeline;
scale to 1 replica if you want consistent reads, or swap `OrderRepository` for a
JPA repository later.

## Jenkins

`Jenkinsfile` is a declarative pipeline: checkout -> `mvn clean verify` (JUnit
results + jar archived) -> `docker build` -> optional `docker push` -> `kubectl
apply` + `set image` + `rollout status` -> in-cluster curl smoke test. It uses
`sh` or `bat` automatically depending on the agent OS.

Before the first run:

1. Set `REGISTRY` in the `environment` block (e.g. `docker.io/shubham`); leave
   blank for a local-only image.
2. Add credentials in Jenkins:
   - `docker-registry-credentials` - username/password for the registry
   - `kubeconfig-credentials` - Secret file containing your kubeconfig
3. Make sure `mvn`, `docker` and `kubectl` are on the agent's PATH.
4. Tick **PUSH_IMAGE** when deploying to a remote cluster; leave it off locally.
