# TODO - FX constant / FXML errors fix

## Step 1: Fix `fx:constant` invalid element errors
- ✅ Fixed in `src/main/resources/fxml/AdminUsersView.fxml` by replacing invalid `fx:constant fx:value` under `ChoiceBox` items with a proper `FXCollections` + `String fx:value`.
- ✅ Fixed in `src/main/resources/fxml/AuctionDetailView.fxml` by removing invalid `TableView fx:constant="CONSTRAINED_RESIZE_POLICY"`.

## Step 2: Re-run search to ensure no remaining `fx:constant` issues
- ✅ Confirmed `search_files` found 0 occurrences of `fx:constant` in `src/**/*.fxml`.

## Step 3: Build / run compile validation
- ⛔ Not verified yet due to local command execution issues (no `mvn` in PATH; `mvnw` invocation hit PowerShell parsing with `&&`).
- Next attempt: run `mvnw -q test -DskipTests=false` without `&&` separator.

## Step 4: Validate FXML loading at runtime
- Next: run the JavaFX app and ensure FXML loads without runtime FXML warnings/errors.


