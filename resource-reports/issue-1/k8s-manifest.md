# Kubernetes Manifest for gwanak-bnb-server

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: <NAMESPACE>
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: gwanak-bnb-server-sa
  namespace: <NAMESPACE>
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::<ACCOUNT_ID>:role/gwanak-bnb-server-irsa-role
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gwanak-bnb-server
  namespace: <NAMESPACE>
  labels:
    app: gwanak-bnb-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gwanak-bnb-server
  template:
    metadata:
      labels:
        app: gwanak-bnb-server
    spec:
      serviceAccountName: gwanak-bnb-server-sa
      containers:
      - name: gwanak-bnb-server
        image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/gwanak-bnb-server:latest
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "<REPLACE_ME>"
        - name: SPRING_DATASOURCE_USERNAME
          value: "<REPLACE_ME>"
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: gwanak-bnb-server-secrets
              key: db-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: gwanak-bnb-server-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 30
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: gwanak-bnb-server-service
  namespace: <NAMESPACE>
  labels:
    app: gwanak-bnb-server
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: gwanak-bnb-server
---
apiVersion: v1
kind: Secret
metadata:
  name: gwanak-bnb-server-secrets
  namespace: <NAMESPACE>
type: Opaque
data:
  db-password: "<REPLACE_ME_BASE64_ENCODED>"
  jwt-secret: "<REPLACE_ME_BASE64_ENCODED>"
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: gwanak-bnb-server-vs
  namespace: <NAMESPACE>
spec:
  hosts:
  - "<YOUR_DOMAIN>"
  gateways:
  - <NAMESPACE>/istio-gateway
  http:
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: gwanak-bnb-server-service
        port:
          number: 8080
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gwanak-bnb-server-ingress
  namespace: <NAMESPACE>
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
spec:
  rules:
  - host: <YOUR_DOMAIN>
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gwanak-bnb-server-service
            port:
              number: 8080
```