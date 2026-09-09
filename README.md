# Online Auction System

A Java Swing desktop application for managing sellers, customers, products, and auctions with a MySQL database.

## Features

- Seller and customer management
- Product registration
- Auction creation and bidding
- MySQL-backed persistence
- Shaded executable JAR packaging with Maven

## Requirements

- Java 17 or later
- Maven
- MySQL

## Configuration

Set these environment variables before starting the application:

```text
JDBC_URL=jdbc:mysql://localhost:3306/auction
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
```

Create the database schema expected by the SQL statements in `src/Main.java`, then build and run:

```bash
git clone https://github.com/reyansh12345678890/online-auction.git
cd online-auction
mvn clean package
java -jar target/auction-system-1.0.0.jar
```

Never commit real database credentials. Use environment variables or a local secrets manager.
