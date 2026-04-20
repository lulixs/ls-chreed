# CHREED Bible App

A desktop Bible reading application built with JavaFX.

## Requirements

- Java 21+
- Maven

## Compile

```bash
mvn clean compile
```

## Run

```bash
mvn javafx:run
```

Or open in IntelliJ IDEA and run `Main.java`.

## Project Structure

```
src/main/java/com/bibleapp/
  Main.java               # Application entry point
  App.java                # Main layout with sidebar and content area
  pages/
    ReadingPage.java      # Bible reading view
    ReadingPlansPage.java # Reading plans management
    StatisticsPage.java   # User statistics and badges
    MemorizationPage.java # Verse memorization
  data/
    DataStore.java        # JSON file persistence

src/main/resources/com/bibleapp/
  styles.css              # Application styling
  chreed.png              # Application logo
```

## Architecture

**App.java** creates the main window with:
- Left sidebar: CHREED logo, navigation tabs (Stats, Read, Plans, Memorize), user profile section
- Content area: Displays the currently selected page

**Pages** extend VBox and are swapped in the content area when tabs are clicked.

**DataStore.java** handles persistence via JSON file at `~/bible-app-data.json`.

## Key Components

### Sidebar (App.java)
- `TABS` array defines navigation labels
- `showPage()` switches content based on tab selection
- `highlightTab()` manages active tab styling
- User section at bottom opens a popup when clicked

### Pages
Each page follows a similar pattern:
- Title label with `page-title` style class
- Layout containers (HBox for rows, VBox for columns)
- ScrollPane for scrollable content areas
- Popup system for adding items (Plans, Memorize tabs)

### Styling
All visual styles are in `styles.css` using JavaFX CSS:
- `.sidebar` - Blue navigation panel
- `.nav-btn` / `.nav-btn--active` - Tab buttons
- `.content-area` - Main content background
- Page-specific classes like `.reading-plans-left-column`, `.stats-scroll`, etc.

## Data Storage

```java
// Load data
JSONObject data = DataStore.load();

// Modify data...

// Save data
DataStore.save(data);
```

Default data structure includes `memorized_verses`, `reading_plans`, and `statistics`.

## Cross-Platform

JavaFX dependencies are platform-agnostic. Maven downloads the correct native libraries for the target OS during build.
