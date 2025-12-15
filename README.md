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

The application will be available at: `http://localhost:8080/smvc`

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
   - URL: `http://localhost:8080/smvc`
   - Tomcat Manager: `http://localhost:8080/manager` (if configured)

### Option 3: Using IDE

Most IDEs (IntelliJ IDEA, Eclipse) support direct deployment to Tomcat:

1. Import the project as a Maven project
2. Configure a Tomcat server in your IDE
3. Deploy the project to the server
4. Start the server from within the IDE

## Eclipse Development Setup (Recommended for Interactive Development)

Eclipse provides excellent support for running Spring MVC applications directly without WAR deployment:

### Prerequisites for Eclipse
- **Eclipse IDE for Enterprise Java Developers** (includes Web Tools Platform)
- **Apache Tomcat 10.x** installed locally (does not recognise Choco install, recommend using embedded installer)
- **Java 17** configured in Eclipse

### Step-by-Step Eclipse Setup

#### 1. Import the Project
```
File → Import → Existing Maven Projects
Browse to your project directory
Select the pom.xml file
Click Finish
```

#### 2. Configure Tomcat Server in Eclipse
```
Window → Show View → Servers (or Window → Show View → Other → Server → Servers)
In Servers view: Right-click → New → Server
Select: Apache → Tomcat v10.0 Server
Browse to your Tomcat installation directory
Click Finish
```

#### 3. Configure Project for Server
```
Right-click on your Tomcat server in Servers view
Select "Add and Remove..."
Move your project from Available to Configured
Click Finish
```

#### 4. Run the Application
```
Right-click on Tomcat server → Start
Or click the green "Start" button in Servers view
```

#### 5. Access the Application
- URL: `http://localhost:8080/smvc/`
- The context path is automatically set to the project name

### Eclipse Development Benefits

- **Hot Reload**: Changes to Java files are automatically recompiled and reloaded
- **Template Updates**: Thymeleaf template changes are reflected immediately
- **Debugging**: Full debugging support with breakpoints
- **No WAR Building**: Direct deployment from workspace
- **Fast Iteration**: Immediate feedback on code changes

### Eclipse Project Configuration

#### Project Facets (should be automatically configured)
```
Right-click project → Properties → Project Facets
Ensure these are enabled:
- Java 17
- Dynamic Web Module 6.0
- JavaScript 1.0 (optional)
```

#### Deployment Assembly
```
Right-click project → Properties → Deployment Assembly
Should include:
- Maven Dependencies → WEB-INF/lib
- src/main/webapp → /
- Java Build Path Libraries → WEB-INF/lib
```

### Troubleshooting Eclipse Issues

#### Server Won't Start
```
1. Check Servers view for error messages
2. Verify Tomcat installation path
3. Ensure port 8080 is not in use
4. Clean and refresh project (F5)
```

#### Changes Not Reflected
```
1. Project → Clean → Clean all projects
2. Right-click server → Clean...
3. Right-click server → Publish
4. Restart server if needed
```

#### Maven Dependencies Issues
```
1. Right-click project → Maven → Reload Projects
2. Right-click project → Maven → Update Project
3. Check "Force Update of Snapshots/Releases"
```

### Eclipse Development Workflow

1. **Make Code Changes**: Edit Java files, templates, or resources
2. **Auto-Compile**: Eclipse automatically compiles Java changes
3. **Hot Deploy**: Server automatically picks up changes
4. **Test**: Refresh browser to see changes
5. **Debug**: Set breakpoints and debug as needed

This setup provides the fastest development cycle without the need to build and deploy WAR files manually.

## Development Workflow

## Active Directory authentication

This application can authenticate users against an Active Directory (AD) server using LDAP.

Configuration
- The AD domain and LDAP URL can be configured via Spring `@Value` properties (defaults shown):

```properties
ad.domain=hdc.webhop.net
ad.url=ldap://hdc.webhop.net/
```

If your AD requires TLS, change the URL to `ldaps://hdc.webhop.net:636/` and ensure the container trusts the AD server certificate.

Login page
- The application uses Spring Security form login at `/login` (Thymeleaf template at `WEB-INF/templates/login.html`).

Notes
- The app uses `ActiveDirectoryLdapAuthenticationProvider` which performs a direct bind with the provided username and password.
- Username formats accepted by AD commonly include `DOMAIN\\username` or `username@domain` depending on your AD configuration.

Development
-----------

If you are developing locally without AD access, the project provides a `dev` Spring profile that uses an in-memory user store.

- Start the app with the `dev` profile:

```powershell
mvn -Dspring.profiles.active=dev -DskipTests=false clean verify
```

- Default dev credentials (only use locally): `devuser` / `password`

To switch back to Active Directory authentication, run without the `dev` profile (or with a profile that enables AD) and set `ad.domain` / `ad.url` appropriately.

Run tests with the `dev` profile
--------------------------------

When running unit tests locally and you want them to use the in-memory dev user (no AD required), run Maven with the `dev` profile. In PowerShell it's easiest to set an environment variable first:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn clean test
```

You can also run a single test or use the Maven property directly (Bash/CMD may accept it more reliably):

```powershell
# Run a single test (PowerShell)
$env:SPRING_PROFILES_ACTIVE = "dev"; mvn -Dtest=DevSecurityConfigTest test

# Or (POSIX shells):
mvn -Dspring.profiles.active=dev -Dtest=DevSecurityConfigTest test
```

Note: setting the environment variable in PowerShell avoids issues with argument ordering and ensures the Spring `dev` profile is active during test execution.

JUnit XML Test Reports
----------------------

The Maven Surefire plugin is configured to emit JUnit-compatible XML reports into the `target/junit-reports` directory.

To generate the XML reports locally (using the `dev` profile):

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn -DskipTests=false test
```

After the run, look for files matching `target/junit-reports/TEST-*.xml`. These can be consumed by CI systems (e.g., Jenkins, GitHub Actions) to display test results and trends.


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