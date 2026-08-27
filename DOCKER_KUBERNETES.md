# Docker and Kubernetes guide

This project produces one container image for each Spring Boot microservice:

| Service | Image | Application port |
|---|---|---:|
| Authentication | `ecommerce/auth-service:local` | 8083 |
| Orders | `ecommerce/order-service:local` | 8080 |
| Payments | `ecommerce/payment-service:local` | 8081 |

Each image contains the compiled Java application and a Java 17 runtime. The multi-stage Dockerfiles use Maven only in the build stage, so Maven and the source tree are not included in the final runtime image. The application runs as a non-root user.

## 1. Run the complete application with Docker Compose

From the repository root:

```bash
docker compose build
docker compose up -d
docker compose ps
```

The three containers share the Compose `backend` network. Containers can resolve one another using the service names `auth-service`, `order-service`, and `payment-service`.

The host endpoints remain:

- Authentication: `http://localhost:8083`
- Orders: `http://localhost:8090` (host port 8090 forwards to container port 8080)
- Payments: `http://localhost:8081`

Inspect the application:

```bash
docker compose logs -f
docker compose ps
docker image ls 'ecommerce/*'
```

Stop it without deleting images:

```bash
docker compose down
```

Rebuild one changed service:

```bash
docker compose build order-service
docker compose up -d --no-deps order-service
```

## 2. Verify health endpoints

Spring Boot Actuator supplies operational endpoints for Compose and Kubernetes:

```bash
curl http://localhost:8083/actuator/health/readiness
curl http://localhost:8090/actuator/health/readiness
curl http://localhost:8081/actuator/health/readiness
```

Expected response:

```json
{"status":"UP"}
```

## 3. Deploy to Docker Desktop Kubernetes

Build the three local images first:

```bash
docker compose build
```

Confirm the Docker Desktop cluster:

```bash
kubectl config use-context docker-desktop
kubectl get nodes
```

Create all Kubernetes resources:

```bash
kubectl apply -k k8s
kubectl get deployments,pods,services -n ecommerce
kubectl rollout status deployment/auth-service -n ecommerce
kubectl rollout status deployment/order-service -n ecommerce
kubectl rollout status deployment/payment-service -n ecommerce
```

The manifest creates three Deployments. Each Deployment maintains one Pod. A ClusterIP Service gives every workload a stable internal Domain Name System address:

- `auth-service.ecommerce.svc.cluster.local:8083`
- `order-service.ecommerce.svc.cluster.local:8080`
- `payment-service.ecommerce.svc.cluster.local:8081`

The current applications use isolated in-memory H2 databases. Keep `replicas: 1` until the databases are moved to persistent external storage; otherwise each replica would have different data.

### Access the services from the Mac

ClusterIP Services are internal, so use port forwarding during local development. Run these in separate terminals:

```bash
kubectl port-forward service/auth-service 8083:8083 -n ecommerce
kubectl port-forward service/order-service 8090:8080 -n ecommerce
kubectl port-forward service/payment-service 8081:8081 -n ecommerce
```

The same localhost endpoints and integration test flow can then be used.

### If a Pod reports `ImagePullBackOff`

Inspect it first:

```bash
kubectl describe pod -n ecommerce -l app.kubernetes.io/name=auth-service
```

The manifests use `imagePullPolicy: IfNotPresent`, which asks the Kubernetes node to use a matching local image. Docker Desktop cluster provisioning and image-store combinations differ. If the cluster cannot see the Docker image store, use one of these approaches:

1. Select a Docker Desktop Kubernetes configuration that shares the chosen image store. In Docker Desktop, a `kind` cluster can use local images from the containerd image store, but not from the classic Docker image store. A `kubeadm` cluster works with either image store. Open **Kubernetes**, choose **Edit cluster**, and select **kubeadm** if you want to keep using the classic image store.
2. Tag and push the three images to a registry, then change the manifest image names.

Docker Desktop keeps the classic and containerd image stores separately. Switching image stores hides the images and containers in the inactive store until you switch back, so changing the Kubernetes provisioner to `kubeadm` is generally the less surprising choice for an existing classic-store setup.

Docker's current compatibility table is documented at <https://docs.docker.com/desktop/use-desktop/kubernetes/#cluster-provisioning-method>.

For a registry called `YOUR_DOCKER_ID`:

```bash
docker tag ecommerce/auth-service:local YOUR_DOCKER_ID/auth-service:v1
docker tag ecommerce/order-service:local YOUR_DOCKER_ID/order-service:v1
docker tag ecommerce/payment-service:local YOUR_DOCKER_ID/payment-service:v1

docker push YOUR_DOCKER_ID/auth-service:v1
docker push YOUR_DOCKER_ID/order-service:v1
docker push YOUR_DOCKER_ID/payment-service:v1
```

## 4. Useful Kubernetes diagnostics

```bash
kubectl get all -n ecommerce
kubectl get pods -n ecommerce -o wide
kubectl logs deployment/auth-service -n ecommerce
kubectl logs deployment/order-service -n ecommerce
kubectl logs deployment/payment-service -n ecommerce
kubectl describe deployment/auth-service -n ecommerce
kubectl get events -n ecommerce --sort-by=.metadata.creationTimestamp
```

Delete only this local Kubernetes environment:

```bash
kubectl delete -k k8s
```

## 5. What the Kubernetes resources do

| Resource | Responsibility |
|---|---|
| Namespace | Isolates this application's Kubernetes objects under `ecommerce`. |
| Secret | Supplies the shared JSON Web Token signing key without placing it in a Deployment. The included value is only for local development. |
| Deployment | Declares the desired Pod template, replica count, update behavior, probes, resources, and security settings. |
| Pod | Runs one instance of a microservice image. Pods are created and replaced by their Deployment. |
| Service | Gives changing Pods a stable internal address and routes traffic only to ready Pods. |
| Startup probe | Gives Spring Boot time to start before liveness checking begins. |
| Readiness probe | Prevents traffic from reaching a Pod that is not ready to serve requests. |
| Liveness probe | Restarts a Pod whose application has become unhealthy. |

## 6. Deliberate limitations of this learning deployment

- H2 is in-memory and loses data when a container or Pod is replaced.
- There is no Application Programming Interface gateway; each service is accessed separately.
- There is no external Ingress or Gateway route.
- The development JSON Web Token secret must be replaced outside a local environment.
- There is no distributed tracing or centralized log aggregation yet.

The next production-oriented step would be to move each service's state to PostgreSQL, keep database ownership separated by service, and expose public routes through a gateway while leaving the internal Services private.
