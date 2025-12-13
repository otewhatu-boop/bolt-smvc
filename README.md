# Monitor Centre Application

A Spring MVC web application for monitoring and management, built with Java 17 and deployed as a WAR file. For CI/CD see [Environments](https://v8lust.atlassian.net/wiki/spaces/HDC/pages/933685/SMVC+Monitor+Centre#Environments) in Confluence.

## Prerequisites

Before running this application locally, ensure you have the following installed:

- **Java 17** or higher
- **Apache Maven 3.6+**
- **Apache Tomcat 10** or any Jakarta EE 9+ compatible web container

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── hdc/company/monitor/
│   │       ├── config/          # Spring configuration classes
│   │       └── controller/      # MVC controllers
│   ├── resources/
│   │   └── version.properties   # Application version
│   └── webapp/
│       └── WEB-INF/
│           └── templates/       # Thymeleaf templates
└── test/
    ├── java/                    # Unit tests
    └── resources/               # Test configuration
```

## Building the Application

### 1. Clean and Package
```bash
mvn clean package
```

This will:
- Compile the Java source code
- Run unit tests
- Generate a WAR file in the `target/` directory named `monitor-centre.war`

### 2. Skip Tests (if needed)
```bash
mvn clean package -DskipTests
```

## Running Locally

### Option 1: Using Maven Tomcat Plugin (Recommended for Development)

Add the Tomcat plugin to your `pom.xml` if not already present, then run:

```bash
mvn tomcat7:run
```

The application will be available at: `http://localhost:8080/monitor-centre`

### Option 2: Deploy to Standalone Tomcat

1. **Start Tomcat server**
   ```bash
   # Navigate to your Tomcat installation directory
   cd /path/to/tomcat
   ./bin/startup.sh  # On Linux/Mac
   # or
   bin\startup.bat   # On Windows
   ```

2. **Deploy the WAR file**
   ```bash
   # Copy the generated WAR to Tomcat's webapps directory
   cp target/monitor-centre.war /path/to/tomcat/webapps/
   ```

3. **Access the application**
   - URL: `http://localhost:8080/monitor-centre`
   - Tomcat Manager: `http://localhost:8080/manager` (if configured)

### Option 3: Using IDE

Most IDEs (IntelliJ IDEA, Eclipse) support direct deployment to Tomcat:

1. Import the project as a Maven project
2. Configure a Tomcat server in your IDE
3. Deploy the project to the server
4. Start the server from within the IDE

## Development Workflow

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=HomeControllerTest

# Run tests with detailed output
mvn test -X
```

### Code Compilation Only
```bash
mvn compile
```

### Clean Build Directory
```bash
mvn clean
```

## Application Features

- **Login Interface**: Clean, responsive login form (placeholder functionality)
- **Version Display**: Shows application version from `version.properties`
- **Responsive Design**: Mobile-friendly interface
- **Spring MVC**: RESTful controller architecture
- **Thymeleaf Templates**: Server-side rendering

## Configuration

### Application Properties
- Version information is managed in `src/main/resources/version.properties`
- Maven filtering is enabled to inject build-time properties

### Spring Configuration
- Java-based configuration in `hdc.company.monitor.config` package
- No XML configuration files required (except for tests)

## Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Find process using port 8080
   lsof -i :8080
   # Kill the process or use a different port
   ```

2. **Java version mismatch**
   ```bash
   # Check Java version
   java -version
   javac -version
   ```

3. **Maven compilation errors**
   ```bash
   # Clean and reinstall dependencies
   mvn clean install -U
   ```

### Logs Location
- Tomcat logs: `$TOMCAT_HOME/logs/catalina.out`
- Application logs: Check Tomcat's `localhost.log`

## Contributing

1. Follow Java coding standards
2. Write unit tests for new functionality
3. Update documentation as needed
4. Test locally before committing

## Technology Stack

- **Java 17**
- **Spring MVC 6.0.13**
- **Thymeleaf 3.1.2**
- **Maven 3.x**
- **JUnit 5** (for testing)
- **Jakarta Servlet API 6.0**