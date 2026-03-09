# OmniSolve API

Backend API for OmniSolve document control system.

## 🚀 Quick Start

### Prerequisites
- Java 21 (Corretto)
- Maven 3.8+
- PostgreSQL 15
- AWS Account (for deployment)

### Local Development

```bash
# Clone repository
git clone <repository-url>
cd omnisolve-api

# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run
```

## 📦 Deployment

This project uses a trunk-based deployment model with automated CI/CD.

### Deployment Flow

```
Feature Branch → PR → Main → DEV (automatic) → Approval → PROD
```

### Quick Deployment Guide

1. **Merge to main** - Triggers automatic deployment to DEV
2. **Verify in DEV** - Test the application
3. **Approve production** - Manual approval required
4. **Deploy to PROD** - Same artifact promoted

### Documentation

- **[Quick Reference](.github/QUICK_REFERENCE.md)** - Fast lookup for deployments
- **[Deployment Workflow](.github/DEPLOYMENT_WORKFLOW.md)** - Complete guide
- **[Environment Setup](.github/ENVIRONMENT_SETUP.md)** - Configure GitHub & AWS
- **[Deployment Diagram](.github/DEPLOYMENT_DIAGRAM.md)** - Visual guide
- **[CI/CD Documentation](.github/README.md)** - All CI/CD docs

### First Time Setup

See [TRUNK_BASED_DEPLOYMENT_COMPLETE.md](TRUNK_BASED_DEPLOYMENT_COMPLETE.md) for complete setup instructions.

## 🏗️ Infrastructure

Infrastructure is managed with Terraform in the `infrastructure/` directory.

### Environments

- **DEV:** http://dev-omnisolve-api.eba-n3eav3gy.us-east-1.elasticbeanstalk.com
- **PROD:** http://prod-omnisolve-api.{region}.elasticbeanstalk.com

### Resources

- Elastic Beanstalk (Java/Corretto 21)
- RDS PostgreSQL
- S3 Document Storage
- IAM Roles & Policies

See [infrastructure/README.md](infrastructure/README.md) for details.

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=DocumentControllerIT
```

## 📚 API Documentation

API documentation is available at:
- **DEV:** http://dev-omnisolve-api.eba-n3eav3gy.us-east-1.elasticbeanstalk.com/swagger-ui.html
- **PROD:** http://prod-omnisolve-api.{region}.elasticbeanstalk.com/swagger-ui.html

## 🔧 Configuration

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev`, `prod` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://...` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `omnisolve` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `***` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `AWS_S3_BUCKET` | S3 bucket name | `omnisolve-documents-dev` |

## 🛠️ Development

### Branch Strategy

- **main** - Production-ready code
- **feature/** - New features
- **bugfix/** - Bug fixes
- **hotfix/** - Emergency fixes

### Workflow

1. Create feature branch from main
2. Make changes and commit
3. Open PR to main
4. Wait for CI checks
5. Get code review
6. Merge to main
7. Automatic deployment to DEV

## 📊 Monitoring

### Health Checks

```bash
# DEV
curl http://dev-omnisolve-api.eba-n3eav3gy.us-east-1.elasticbeanstalk.com/api/health

# PROD
curl http://prod-omnisolve-api.{region}.elasticbeanstalk.com/api/health
```

### Logs

- **GitHub Actions:** Repository → Actions tab
- **CloudWatch:** AWS Console → CloudWatch → Log Groups
- **Beanstalk:** AWS Console → Elastic Beanstalk → Environment

## 🆘 Troubleshooting

### Common Issues

**Deployment fails:**
- Check GitHub Actions logs
- Review Beanstalk events in AWS Console
- Check CloudWatch logs

**Health check fails:**
- Wait 1-2 minutes for startup
- Check application logs
- Verify database connection

**Cannot approve PROD:**
- Ensure you're a required reviewer
- Check repository write access
- Verify workflow is at approval stage

See [.github/DEPLOYMENT_WORKFLOW.md](.github/DEPLOYMENT_WORKFLOW.md#troubleshooting) for more.

## 📖 Documentation

### CI/CD
- [CI/CD Overview](.github/README.md)
- [Deployment Workflow](.github/DEPLOYMENT_WORKFLOW.md)
- [Environment Setup](.github/ENVIRONMENT_SETUP.md)
- [Quick Reference](.github/QUICK_REFERENCE.md)

### Infrastructure
- [Infrastructure Overview](infrastructure/README.md)
- [Elastic Beanstalk Setup](infrastructure/ELASTIC_BEANSTALK_SETUP.md)
- [Deployment Guide](infrastructure/DEPLOYMENT_GUIDE.md)

### Getting Started
- [Trunk-Based Deployment Complete](TRUNK_BASED_DEPLOYMENT_COMPLETE.md)

## 🤝 Contributing

1. Create feature branch
2. Make changes
3. Write tests
4. Open PR
5. Get review
6. Merge to main

## 📝 License

[Add license information]

## 👥 Team

[Add team information]

## 🔗 Links

- [GitHub Repository](../../)
- [AWS Console](https://console.aws.amazon.com/)
- [CI/CD Pipeline](../../actions)
- [Infrastructure](infrastructure/)

---

**Last Updated:** March 9, 2026  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Active Development
