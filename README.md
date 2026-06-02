# online-auction-system
system of an auction that is handled online

## Run

Requirements:
- JDK 21
- Maven, or use the bundled Maven Wrapper
- MySQL configured via `src/main/resources/db.properties`

Start server from the IDE by running:

```text
com.auction.server.Server
```

Start JavaFX client:

```bash
./mvnw javafx:run
```

On Windows:

```powershell
.\mvnw.cmd javafx:run
```

If running from an IDE, run `com.auction.client.Main` instead of `com.auction.client.Launcher`.
Running `Launcher` directly may show `JavaFX runtime components are missing` when the IDE has not configured JavaFX runtime modules.
