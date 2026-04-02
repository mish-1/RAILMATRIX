# RailMatrix Booking Console

RailMatrix is a Java + MySQL train booking project with:
- CLI app for terminal-based flow
- Web UI for search and booking management
- REST API layer between frontend and database

## Features

- Search direct and connecting trains
- Create bookings with journey date and seat count
- View only user-specific bookings (privacy-safe)
- Booking limits:
  - Seat count per booking: 1 to 6
  - Seat capacity per train per date: 120
- Automatic database name detection from [railmatrix.sql](railmatrix.sql)

## Project Structure

- [main/RailMatrixApp.java](main/RailMatrixApp.java): CLI entry point
- [main/RailMatrixWebServer.java](main/RailMatrixWebServer.java): HTTP server + API endpoints
- [service/DatabaseService.java](service/DatabaseService.java): DB config, schema init, seed
- [service/BookingService.java](service/BookingService.java): CLI booking flow
- [service/TrainService.java](service/TrainService.java): train search and train fetch logic
- [service/dao/](service/dao): DAO layer
- [output-viewer.html](output-viewer.html): frontend UI
- [railmatrix.sql](railmatrix.sql): schema + seed script
- [run-web.ps1](run-web.ps1): one-command compile + run

## Requirements

- Java 17+ (or a JDK that supports jdk.httpserver)
- MySQL 8+
- MySQL JDBC driver JAR (already present in [main/](main/))
- Windows PowerShell

## Database Setup

1. Open MySQL and run [railmatrix.sql](railmatrix.sql).
2. Ensure MySQL user/password match your environment.

Optional environment variables (if you want custom values):
- RAILMATRIX_DB_URL
- RAILMATRIX_DB_USER
- RAILMATRIX_DB_PASSWORD

If RAILMATRIX_DB_URL is not set, app builds default URL from the CREATE DATABASE line in [railmatrix.sql](railmatrix.sql).

## Run Backend + Frontend

From project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-web.ps1
```

Then open:

- http://localhost:8080

## Run CLI App (Optional)

```powershell
$jarPaths = (Get-ChildItem . -Recurse -File -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
if ([string]::IsNullOrWhiteSpace($jarPaths)) { $cp='.' } else { $cp='.;' + $jarPaths }
javac -cp $cp main\RailMatrixApp.java model\*.java service\*.java service\dao\*.java
java -cp $cp main.RailMatrixApp
```

## API Endpoints

Base URL: http://localhost:8080

- GET /api/health
  - Checks server status

- GET /api/trains?source={source}&destination={destination}
  - Returns direct and connecting trains

- POST /api/bookings
  - Creates booking
  - Body example:

```json
{
  "userId": 10,
  "userName": "Demo User",
  "trainId": 901,
  "journeyDate": "2026-05-01",
  "seatCount": 2
}
```

- GET /api/bookings?userId={id}
  - Returns bookings for that user only
  - userId is required for privacy

## Notes

- [output-viewer.html](output-viewer.html) only shows user-scoped bookings.
- .class files are ignored via [.gitignore](.gitignore).
