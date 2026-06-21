# Kubernetes Manifest - gwanak-bnb-server

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: gwanak-bnb-server-sa
  namespace: <NAMESPACE>
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::<ACCOUNT_ID>:role/gwanak-bnb-server-OnPodProd
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gwanak-bnb-server
  namespace: <NAMESPACE>
  labels:
    app: gwanak-bnb-server
spec:
  replicas: 1
  revisionHistoryLimit: 4
  selector:
    matchLabels:
      app: gwanak-bnb-server
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
  template:
    metadata:
      labels:
        app: gwanak-bnb-server
    spec:
      serviceAccountName: gwanak-bnb-server-sa
      nodeSelector:
        kubernetes.io/arch: arm64
        phase: prod
      tolerations:
        - effect: NoSchedule
          key: phase
          operator: Equal
          value: prod
      containers:
        - name: gwanak-bnb-server
          image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/gwanak-bnb-server-prod/server:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              value: "<REPLACE_ME>"
            - name: SPRING_DATASOURCE_USERNAME
              value: "<REPLACE_ME>"
            - name: SPRING_DATASOURCE_PASSWORD
              value: "<REPLACE_ME>"
            - name: JWT_SECRET
              value: "<REPLACE_ME>"
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 500m
              memory: 1Gi
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            failureThreshold: 18
            timeoutSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 30
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: gwanak-bnb-server
  namespace: <NAMESPACE>
spec:
  type: ClusterIP
  selector:
    app: gwanak-bnb-server
  ports:
    - port: 80
      targetPort: 8080
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: gwanak-bnb-server
  namespace: <NAMESPACE>
spec:
  gateways:
    - <NAMESPACE>/istio-gateway
    - mesh
  hosts:
    - gwanak-bnb-server-api.wafflestudio.com
  http:
    - route:
        - destination:
            host: gwanak-bnb-server
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: gwanak-bnb-server-secrets
  namespace: <NAMESPACE>
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: ClusterSecretStore
  target:
    name: gwanak-bnb-server-secrets
  dataFrom:
    - extract:
        key: gwanak-bnb-server/prod/<SECRET_NAME>
```