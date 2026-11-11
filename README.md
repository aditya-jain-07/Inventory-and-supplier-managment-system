# DVMS Inventory and Supplier Management System

Java Swing desktop application connected to MySQL via JDBC. The GUI provides a synchronized two-panel layout for managing inventory items and suppliers with real-time MySQL database persistence, client-side data validation, non-blocking asynchronous database operations via `SwingWorker`, and an offline local MySQL Connector/J driver.

---

## Requirements

- **JDK 17** or newer
- **Apache Maven 3.6+**
- **MySQL Server 8.0+** running locally on port `3306`
- **IntelliJ IDEA** (or any standard Java IDE)
- **Local JDBC Driver**: `lib/mysql-connector-j-26.10.0.jar` (included in repository)

---

## 1. Local MySQL Connector/J Driver

The project uses a manually installed, offline MySQL Connector/J JDBC driver located in the `lib` folder:

- **File Path**: `lib/mysql-connector-j-26.10.0.jar`
- **Driver Class**: `com.mysql.cj.jdbc.Driver`
- **Maven Configuration**: Configured in `pom.xml` via system scope (`${project.basedir}/lib/mysql-connector-j-26.10.0.jar`), eliminating internet downloads during Maven builds.

---

## 2. Database Setup & Configuration

### Step 1: Initialize Database Schema

1. Open **MySQL Workbench** (or MySQL CLI) and connect to your local MySQL instance.
2. Open and execute the script located at:
   ```
   database/dvms_db.sql
   ```
3. Confirm that schema `dvms_db` and tables `inventory` and `suppliers` are created.

### Step 2: Configure Database Credentials

The system evaluates credentials in the following priority order:

1. **Environment Variables** (Highest precedence):
   - `DB_URL` (e.g., `jdbc:mysql://localhost:3306/dvms_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata`)
   - `DB_USER` (e.g., `root`)
   - `DB_PASSWORD` (e.g., `MyPASSWORD`)

2. **Local Properties File (`db.properties`)**:
   Copy the provided `db.properties.example` template:
   ```bash
   cp db.properties.example db.properties
   ```
   Edit `db.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/dvms_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
   db.user=root
   db.password=MyPASSWORD
   ```
   *(Note: `db.properties` is registered in `.gitignore` to prevent committing secrets to source control).*

3. **Built-in Defaults (Fallback)**:
   If no environment variables or `db.properties` are found, `DBConnection.java` connects to:
   - Host: `localhost:3306`
   - Database: `dvms_db`
   - User: `root`
   - Password: `MyPASSWORD` (with local compatibility fallback for existing environments)

---

## 3. IntelliJ IDEA Configuration

When opening the project in IntelliJ IDEA:

1. Select **File → Open** and choose the `DVMS-Inventory-System` folder.
2. When prompted, open as a **Maven project** (IntelliJ automatically imports dependencies from `pom.xml`).
3. Set the Project SDK: **File → Project Structure → Project → SDK** → select **JDK 17+**.
4. Confirm/Configure the local JAR dependency:
   - Navigate to **File → Project Structure → Modules → Dependencies**.
   - If not already recognized, click the `+` icon → **JARs or Directories...**.
   - Select `lib/mysql-connector-j-26.10.0.jar` from the project folder.
   - Set Scope to **Compile** or **Runtime**.
   - Click **Apply** and **OK**.
5. In the Project Explorer, navigate to:
   `src/main/java/InventoryAndSupplierSystem.java`
6. Right-click and choose **Run 'InventoryAndSupplierSystem.main()'**.

---

## 4. Running the Application from Terminal

### Option A: Using Maven

1. **Clean and run automated tests**:
   ```bash
   mvn clean test
   ```
2. **Package into executable JAR**:
   ```bash
   mvn package
   ```
3. **Run the application**:
   ```bash
   mvn exec:java
   ```

### Option B: Running the Packaged JAR Directly

```bash
# Run with bundled manifest classpath
java -jar target/dvms-inventory-system-1.0.0.jar

# Or explicitly specifying the local JAR in the classpath
java -cp "target/dvms-inventory-system-1.0.0.jar;lib/mysql-connector-j-26.10.0.jar" InventoryAndSupplierSystem
```

---

## 5. Troubleshooting Guide

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| **`ClassNotFoundException: com.mysql.cj.jdbc.Driver`** | Local JAR is missing from `lib/` or classpath. | Confirm `lib/mysql-connector-j-26.10.0.jar` exists and re-run `mvn clean test`. |
| **`Access denied for user 'root'@'localhost'`** | Password mismatch. | Check `db.properties` and verify your MySQL root password (`MySql@2008` or local password). |
| **`Communications link failure / Connection refused`** | MySQL service is stopped or port is not 3306. | Start MySQL Server (`net start MySQL80` or Services panel) and confirm port 3306 is listening. |
| **`Unknown database 'dvms_db'`** | Schema has not been created. | Execute `database/dvms_db.sql` in MySQL Workbench. |
| **`Check constraint violated`** | Quantity or price is negative. | Ensure quantity $\ge 0$ and price $\ge 0.00$. |
| **`Duplicate entry for key 'PRIMARY'`** | Item or Supplier ID already exists. | Use a unique identifier for new records. |
