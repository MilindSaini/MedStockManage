# MedStock Phase 1 Build Status

This workspace now contains:
- `medstock-backend/` (Spring Boot 3 + Java 21 + Maven)
- `medstock-frontend/` (React 18 + Vite + Tailwind)
- Flyway migrations V1 to V6 in `medstock-backend/src/main/resources/db/migration/`
- Root `.gitignore`, `.env`, `.env.example`

## AWS actions still requiring account access (Step 1.3)
These cannot be executed from local workspace without your AWS account credentials and confirmations:
1. Launch EC2 `t3.micro` Ubuntu 24 with ports 22, 80, 443, 8080 open.
2. Create RDS PostgreSQL 16 `db.t3.micro` with database `medstock`.
3. Create S3 bucket `medstock-photos` in `ap-south-1` and configure access policy.
4. Create IAM user for backend and issue access key + secret key.

After provisioning, update values in `.env`.
