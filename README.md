### Spring Boot on EC2 — Deployment Guide

This guide summarizes the approaches and practical steps we discussed for deploying your Spring Boot application on Amazon EC2, with clear commands, environment-variable-based configuration, and operational tips.

---

### What you’ll deploy
- A Spring Boot fat JAR built via Gradle (`bootJar`) and started with `java -jar`.
- Configuration supplied via environment variables (no profiles required), aligned with your guideline to prefer environment variables over profiles.

---

### Deployment options overview

#### Option A — Build locally/CI, copy the JAR to EC2 (recommended)
- Build artifact once in a clean environment, then ship only the JAR to servers.
- Run under `systemd` for resilience (restart on failure) and easy log access via `journalctl`.

#### Option B — Build on EC2
- SSH to the instance, install Java and Git, clone the repo, and build using the Gradle wrapper.
- Simpler to start with, but less ideal for reproducibility.

#### Option C — Containerized deployment (optional)
- Build a Docker image and run on EC2 directly or via ECS/Fargate.
- Useful if you plan to scale horizontally or standardize runtime.

This document details Options A and B since they match your current setup.

---

### Prerequisites
- EC2 instance (e.g., Amazon Linux 2023).
- Security group configured to allow inbound TCP to your `server.port` (default `8080`) from your client IP or load balancer.
- Outbound access from EC2 to your database host/port (e.g., PostgreSQL `5432`).
- Java 21 installed on EC2.

---

### Build the application (local/CI)
- Build an executable JAR:
```
./gradlew clean bootJar
# or
./gradlew clean build
```
- Resulting artifact: `build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar` (name may vary by version).

---

### Configure with environment variables (no profiles needed)
You can configure Spring Boot properties via OS environment variables (preferred):
- `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- `SPRING_DATASOURCE_USERNAME` → `spring.datasource.username`
- `SPRING_DATASOURCE_PASSWORD` → `spring.datasource.password`
- `SERVER_PORT` → `server.port`

Optionally, keep placeholders in `application.yml` to document expected env vars:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    open-in-view: false
server:
  port: ${SERVER_PORT:8080}
```

---

### Option A — Deploy the JAR to EC2 and run as a service

#### 1) Install Java 21 on EC2
```
sudo dnf install -y java-21-amazon-corretto
java -version
```

#### 2) Copy the JAR to EC2
From your machine:
```
scp -i <your-key>.pem build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar \
  ec2-user@<ec2-public-dns>:/home/ec2-user/
```

#### 3) Create an environment file for runtime configuration
Create `/etc/sysconfig/springboot-devops-app` with required variables:
```
sudo tee /etc/sysconfig/springboot-devops-app > /dev/null <<'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db-name>
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-pass>
SERVER_PORT=8080
JAVA_OPTS="-Xms256m -Xmx512m"
EOF
sudo chmod 600 /etc/sysconfig/springboot-devops-app
```
- What this does: `tee` writes the here-document’s content to the file; the quoted `EOF` prevents variable expansion while writing.
- `JAVA_OPTS` is a conventional variable you can use to pass JVM arguments (heap, GC, system properties). It isn’t special to Java unless you include it in the `ExecStart` command.

#### 4) Create a `systemd` service unit
```
sudo tee /etc/systemd/system/springboot-devops-app.service > /dev/null <<'EOF'
[Unit]
Description=Spring Boot App
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user
EnvironmentFile=/etc/sysconfig/springboot-devops-app
ExecStart=/usr/bin/java $JAVA_OPTS -jar /home/ec2-user/springboot-devops-app-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
```
Enable and start:
```
sudo systemctl daemon-reload
sudo systemctl enable --now springboot-devops-app
sudo systemctl status springboot-devops-app --no-pager
```

#### 5) Verify the app is running
```
curl -i http://localhost:8080/actuator/health || true
```
- `-i` prints headers. `|| true` ensures the shell does not fail if `curl` returns a non-zero exit code (e.g., not yet up).

#### 6) View logs
- Spring Boot logs to stdout; under `systemd` they’re captured by the journal. View and follow logs with:
```
journalctl -u springboot-devops-app -f
```
- Variants:
    - `journalctl -u springboot-devops-app --since "10 min ago" --no-pager`
    - `journalctl -u springboot-devops-app -b` (current boot)

---

### Option B — Build on EC2 and run

#### 1) Install prerequisites
```
sudo dnf install -y java-21-amazon-corretto git
```

#### 2) Clone and build
```
git clone <repo-url>
cd springboot-devops-app
chmod +x gradlew
./gradlew clean bootJar
```

#### 3) Run with environment variables
- One-off inline run:
```
SPRING_DATASOURCE_URL="jdbc:postgresql://<db-host>:5432/<db-name>" \
SPRING_DATASOURCE_USERNAME="<db-user>" \
SPRING_DATASOURCE_PASSWORD="<db-pass>" \
SERVER_PORT=8080 \
JAVA_OPTS="-Xms256m -Xmx512m" \
java $JAVA_OPTS -jar build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar
```
- Or export into the shell, then run:
```
export SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db-name>
export SPRING_DATASOURCE_USERNAME=<db-user>
export SPRING_DATASOURCE_PASSWORD=<db-pass>
export SERVER_PORT=8080
export JAVA_OPTS="-Xms256m -Xmx512m"

java $JAVA_OPTS -jar build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar
```
- For production, prefer the `systemd` service approach from Option A.

---

### Networking and database checklist
- Security group allows inbound `SERVER_PORT` (default `8080`) from your client/IP or load balancer.
- EC2 outbound rules permit reaching the DB host:port (e.g., `5432`).
- If using AWS RDS:
    - RDS security group must allow inbound from your EC2 security group.
    - Ensure the DB subnet group and routing allow connectivity.
- Verify `spring.jpa.open-in-view=false` (already set per your guidelines).
- Ensure credentials are not logged; keep secrets in env files with restricted permissions (`chmod 600`).

---

### Commands reference (quick copy-paste)

- Build locally:
```
./gradlew clean bootJar
```
- Run locally from the JAR:
```
java -jar build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar
```
- Start via Gradle (dev only):
```
./gradlew bootRun
```
- Health check:
```
curl -i http://localhost:8080/actuator/health || true
```
- View logs under `systemd`:
```
journalctl -u springboot-devops-app -f
```

---

### FAQs and clarifications
- How do I use environment variables without profiles?
    - Set OS env vars (e.g., `SPRING_DATASOURCE_URL`) and Spring Boot maps them to properties automatically. Profiles are optional; you do not need them for value injection.
- Do I need to create a separate env file?
    - Not required, but recommended for servers. With `systemd`, use `EnvironmentFile=/etc/sysconfig/springboot-devops-app` to centralize settings.
- What does `sudo tee ... <<'EOF'` do?
    - Writes a multi-line literal block into a root-owned file. The quoted `EOF` avoids variable expansion during write.
- What is `JAVA_OPTS`?
    - A conventional environment variable you define to hold JVM flags (heap size, GC, system properties). Used in `ExecStart` as `java $JAVA_OPTS -jar ...`.
- Why `|| true` with `curl`?
    - Prevents the shell from treating a non-zero exit code as a failure during provisioning or quick checks.
- Where do logs go under `systemd`?
    - To the journal, viewable with `journalctl -u <service-name>`. Use `-f` to follow in real time.

---

### Troubleshooting tips
- App not reachable externally:
    - Confirm EC2 security group inbound rule on port `8080` (or your port).
    - Check if NACLs and routing allow traffic.
- App fails to start:
    - Check `journalctl -u springboot-devops-app -f` for stack traces.
    - Verify `SPRING_DATASOURCE_URL`, user, password are correct.
    - Confirm Java version: `java -version` (should be 21 as per your build toolchain).
- DB connection errors:
    - Ensure the DB is reachable from EC2 (test with `telnet <db-host> 5432` or `nc -zv <db-host> 5432`).
    - Validate RDS security group allows inbound from EC2’s security group.

---

### Alignment with your Spring Boot guidelines
- Configuration via YAML and env vars (no profiles required for values).
- Clear transaction boundaries and disabled Open Session in View are retained.
- Separation of layers: controllers use DTOs; entities not exposed directly.
- Global exception handling with ProblemDetail.
- Logging to stdout; no `System.out.println()`.

If you share your exact JAR name, EC2 username/home path, and database connection details, I can generate a ready-to-paste `systemd` unit and env file tailored to your environment.