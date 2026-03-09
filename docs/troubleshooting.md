# Troubleshooting Guide

## Common Issues and Solutions

---

## Application Issues

### Application Won't Start

#### Symptoms
- Application fails to start
- Error messages in console
- Port binding errors

#### Solutions

**1. Port Already in Use**

```bash
# Check what's using port 8080
# Unix/macOS
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change the port
export SERVER_PORT=8081
./mvnw spring-boot:run
```

**2. Database Connection Failed**

Check if PostgreSQL is running:
```bash
docker-compose ps
docker-compose logs postgres
```

Verify connection settings in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/omnisolve
    username: postgres
    password: admin
```

**3. Missing Environment Variables**

Ensure required environment variables are set:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/omnisolve
export DB_USERNAME=postgres
export DB_PASSWORD=admin
export JWT_ENABLED=false
```

---

## Database Issues

### Flyway Migration Failed

#### Symptoms
- Application fails to start with Flyway errors
- "Migration checksum mismatch" error
- "Validate failed" error

#### Solutions

**1. Clean and Restart Database**

```bash
# Stop and remove containers
docker-compose down -v

# Start fresh database
docker-compose up -d postgres

# Restart application
./mvnw spring-boot:run
```

**2. Repair Flyway Metadata**

```bash
# Repair Flyway metadata
./mvnw flyway:repair

# Restart application
./mvnw spring-boot:run
```

**3. Manual Database Reset**

```bash
# Connect to database
docker exec -it omnisolve-postgres psql -U postgres -d omnisolve

# Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

# Exit and restart application
\q
./mvnw spring-boot:run
```

### Cannot Connect to Database

#### Symptoms
- "Connection refused" errors
- "Unknown host" errors
- Timeout errors

#### Solutions

**1. Verify PostgreSQL is Running**

```bash
# Check container status
docker-compose ps

# Check container logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

**2. Check Network Connectivity**

```bash
# Test connection
telnet localhost 5432

# Or using nc
nc -zv localhost 5432

# Check Docker network
docker network ls
docker network inspect omnisolve_default
```

**3. Verify Credentials**

```bash
# Test connection with psql
docker exec -it omnisolve-postgres psql -U postgres -d omnisolve

# If fails, check environment variables
docker-compose config
```

### Database Performance Issues

#### Symptoms
- Slow queries
- High CPU usage
- Connection pool exhausted

#### Solutions

**1. Check Connection Pool Settings**

Add to `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

**2. Enable Query Logging**

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**3. Analyze Slow Queries**

```sql
-- Enable slow query logging
ALTER DATABASE omnisolve SET log_min_duration_statement = 1000;

-- View active queries
SELECT pid, now() - query_start as duration, query 
FROM pg_stat_activity 
WHERE state = 'active';
```

---

## AWS Elastic Beanstalk Issues

### 502 Bad Gateway Error

#### Symptoms
- Application returns 502 error
- Health check fails
- Environment shows "Degraded" status

#### Solutions

**1. Check Application Logs**

```bash
# View logs via AWS CLI
aws logs tail /aws/elasticbeanstalk/prod-omnisolve-api/var/log/application.log --follow

# Or download logs
eb logs --all
```

**2. Verify Health Check Endpoint**

```bash
# Test health check locally
curl http://localhost:8080/api/health

# Test on Beanstalk
curl http://<environment-url>/api/health
```

**3. Check Port Configuration**

Ensure application listens on correct port:
```yaml
server:
  port: ${PORT:5000}  # Beanstalk uses PORT env var
```

**4. Increase Timeout**

Add to Beanstalk configuration:
```hcl
setting {
  namespace = "aws:elasticbeanstalk:command"
  name      = "Timeout"
  value     = "600"
}
```

### Environment Health Degraded

#### Symptoms
- Environment shows "Warning" or "Degraded"
- Frequent restarts
- High error rates

#### Solutions

**1. Check Environment Events**

```bash
# View recent events
aws elasticbeanstalk describe-events \
  --environment-name prod-omnisolve-api \
  --max-records 20
```

**2. Review CloudWatch Metrics**

- CPU utilization
- Memory usage
- Request count
- Error rate

**3. Check Instance Health**

```bash
# Describe environment health
aws elasticbeanstalk describe-environment-health \
  --environment-name prod-omnisolve-api \
  --attribute-names All
```

**4. Increase Instance Size**

Update Terraform configuration:
```hcl
beanstalk_instance_type = "t3.small"  # or larger
```

### Deployment Fails

#### Symptoms
- Deployment times out
- Application version fails to deploy
- Environment stuck in "Updating" state

#### Solutions

**1. Check Deployment Logs**

```bash
# View deployment logs
eb logs --all

# Or via CloudWatch
aws logs tail /aws/elasticbeanstalk/prod-omnisolve-api/var/log/eb-engine.log
```

**2. Verify Artifact**

```bash
# Check artifact exists
aws s3 ls s3://prod-omnisolve-deployments/

# Verify artifact size
ls -lh target/omnisolve-api-0.0.1.jar
```

**3. Rollback to Previous Version**

```bash
# List versions
aws elasticbeanstalk describe-application-versions \
  --application-name prod-omnisolve-api

# Deploy previous version
aws elasticbeanstalk update-environment \
  --environment-name prod-omnisolve-api \
  --version-label <previous-version>
```

**4. Terminate and Rebuild**

```bash
# Terminate environment
aws elasticbeanstalk terminate-environment \
  --environment-name prod-omnisolve-api

# Rebuild with Terraform
cd infrastructure/prod
terraform apply
```

---

## S3 Issues

### Access Denied Errors

#### Symptoms
- "Access Denied" when uploading files
- 403 errors from S3
- Cannot list bucket contents

#### Solutions

**1. Verify IAM Permissions**

Check EC2 instance profile has S3 permissions:
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:GetObject",
    "s3:PutObject",
    "s3:DeleteObject",
    "s3:ListBucket"
  ],
  "Resource": [
    "arn:aws:s3:::prod-omnisolve-documents",
    "arn:aws:s3:::prod-omnisolve-documents/*"
  ]
}
```

**2. Check Bucket Policy**

```bash
# View bucket policy
aws s3api get-bucket-policy --bucket prod-omnisolve-documents

# Verify bucket exists
aws s3 ls s3://prod-omnisolve-documents/
```

**3. Verify Region Configuration**

Ensure application uses correct region:
```yaml
app:
  s3:
    region: us-east-1
    bucket: prod-omnisolve-documents
```

**4. Test S3 Access**

```bash
# Test upload
aws s3 cp test.txt s3://prod-omnisolve-documents/test.txt

# Test download
aws s3 cp s3://prod-omnisolve-documents/test.txt downloaded.txt

# Test delete
aws s3 rm s3://prod-omnisolve-documents/test.txt
```

### File Upload Fails

#### Symptoms
- File upload returns error
- Timeout during upload
- Incomplete uploads

#### Solutions

**1. Check File Size Limits**

Add to `application.yml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

**2. Verify Content Type**

Ensure correct MIME type is set:
```java
MultipartFile file = ...;
String contentType = file.getContentType();
```

**3. Check Network Connectivity**

```bash
# Test S3 endpoint
curl -I https://s3.us-east-1.amazonaws.com

# Check DNS resolution
nslookup s3.us-east-1.amazonaws.com
```

**4. Enable S3 Transfer Acceleration**

```hcl
resource "aws_s3_bucket_accelerate_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id
  status = "Enabled"
}
```

---

## Authentication Issues

### JWT Validation Failed

#### Symptoms
- "Invalid token" errors
- 401 Unauthorized responses
- Token expired errors

#### Solutions

**1. Verify JWT Configuration**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_xxxxx

app:
  security:
    jwt:
      enabled: true
    cognito:
      audience: your-client-id
```

**2. Check Token Expiration**

Decode JWT at [jwt.io](https://jwt.io) and verify:
- `exp` (expiration) is in the future
- `iss` (issuer) matches configuration
- `aud` (audience) matches client ID

**3. Disable JWT for Local Development**

```yaml
app:
  security:
    jwt:
      enabled: false
```

**4. Verify Cognito Configuration**

```bash
# Describe user pool
aws cognito-idp describe-user-pool --user-pool-id us-east-1_xxxxx

# List user pool clients
aws cognito-idp list-user-pool-clients --user-pool-id us-east-1_xxxxx
```

### CORS Errors

#### Symptoms
- "CORS policy" errors in browser
- Preflight request fails
- Cross-origin requests blocked

#### Solutions

**1. Configure CORS**

Add CORS configuration:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

**2. Check Preflight Requests**

```bash
# Test preflight
curl -X OPTIONS http://localhost:8080/api/documents \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST"
```

---

## CI/CD Pipeline Issues

### Build Fails

#### Symptoms
- GitHub Actions build fails
- Compilation errors
- Test failures

#### Solutions

**1. Check Build Logs**

View GitHub Actions logs:
```
https://github.com/<org>/<repo>/actions
```

**2. Reproduce Locally**

```bash
# Clean build
./mvnw clean install

# Run tests
./mvnw test

# Check for errors
./mvnw verify
```

**3. Update Dependencies**

```bash
# Update dependencies
./mvnw versions:display-dependency-updates

# Update parent version
./mvnw versions:update-parent
```

### Test Failures

#### Symptoms
- Tests pass locally but fail in CI
- Intermittent test failures
- Database connection errors in tests

#### Solutions

**1. Check Test Configuration**

Ensure tests use correct profile:
```java
@SpringBootTest
@ActiveProfiles("test")
class DocumentServiceTest {
    // ...
}
```

**2. Use Embedded Database**

```xml
<dependency>
  <groupId>io.zonky.test</groupId>
  <artifactId>embedded-postgres</artifactId>
  <scope>test</scope>
</dependency>
```

**3. Fix Flaky Tests**

- Add proper test isolation
- Use `@DirtiesContext` when needed
- Avoid time-dependent assertions
- Use test containers for integration tests

### Deployment Timeout

#### Symptoms
- Deployment exceeds timeout
- Environment stuck in "Updating"
- No response from application

#### Solutions

**1. Increase Timeout**

Update GitHub Actions workflow:
```yaml
- name: Deploy to Elastic Beanstalk
  uses: einaregilsson/beanstalk-deploy@v22
  with:
    wait_for_deployment: true
    wait_for_environment_recovery: 600  # 10 minutes
```

**2. Check Application Startup Time**

Add logging to measure startup:
```java
@SpringBootApplication
public class OmnisolveApiApplication {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication.run(OmnisolveApiApplication.class, args);
        long duration = System.currentTimeMillis() - start;
        System.out.println("Startup time: " + duration + "ms");
    }
}
```

**3. Optimize Startup**

- Reduce number of beans
- Lazy initialize beans
- Disable unnecessary auto-configuration
- Use Spring Boot 3 native compilation

---

## Performance Issues

### Slow API Response

#### Symptoms
- API requests take too long
- Timeout errors
- High latency

#### Solutions

**1. Enable Query Logging**

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

**2. Add Database Indexes**

```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_documents_status ON documents(status_id);
CREATE INDEX idx_documents_department ON documents(department_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);
```

**3. Enable Caching**

```java
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("documents", "clauses");
    }
}
```

**4. Use Pagination**

```java
@GetMapping
public Page<DocumentResponse> getAll(Pageable pageable) {
    return documentService.listDocuments(pageable);
}
```

### High Memory Usage

#### Symptoms
- OutOfMemoryError
- Frequent garbage collection
- Application crashes

#### Solutions

**1. Increase Heap Size**

```bash
# Local development
export JAVA_OPTS="-Xmx1024m -Xms512m"
./mvnw spring-boot:run

# Elastic Beanstalk
# Add to environment variables
JAVA_OPTS=-Xmx1024m -Xms512m
```

**2. Profile Memory Usage**

```bash
# Enable heap dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# Analyze with VisualVM or Eclipse MAT
```

**3. Fix Memory Leaks**

- Close database connections
- Clear caches periodically
- Avoid storing large objects in memory
- Use streaming for large files

---

## Monitoring and Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    root: INFO
    com.omnisolve: DEBUG
    org.springframework.web: DEBUG
    org.hibernate: DEBUG
```

### Health Check Endpoint

```bash
# Check application health
curl http://localhost:8080/api/health

# Expected response
{"status":"ok"}
```

### Actuator Endpoints

Add Spring Boot Actuator:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Enable endpoints:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
```

Access endpoints:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Environment: `http://localhost:8080/actuator/env`

---

## Getting Help

### Check Logs

**Local Development**:
```bash
# Application logs
./mvnw spring-boot:run

# Docker logs
docker-compose logs -f api
```

**AWS Elastic Beanstalk**:
```bash
# CloudWatch logs
aws logs tail /aws/elasticbeanstalk/prod-omnisolve-api/var/log/application.log --follow

# Download all logs
eb logs --all
```

### Community Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **AWS Elastic Beanstalk**: https://docs.aws.amazon.com/elasticbeanstalk/
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/
- **Stack Overflow**: Tag questions with `spring-boot`, `aws-elastic-beanstalk`

### Contact Support

For project-specific issues:
1. Check existing GitHub issues
2. Create new issue with:
   - Description of problem
   - Steps to reproduce
   - Error messages and logs
   - Environment details (OS, Java version, etc.)
