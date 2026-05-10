# nexora-office

nexora-office is a professional modern Android office suite built with **100% Kotlin** and **Jetpack Compose**, designed with a Pro desktop-like experience inspired by WPS Office, Microsoft Office, Google Docs, and VS Code aesthetics.

## Step-by-step foundation delivered

### 1) Full project architecture
- Clean Architecture with modular boundaries:
  - `feature` layer for UI workflows
  - `core` layer for shared contracts, models, data, database, design, and navigation
  - `app` layer for composition root and runtime wiring
- MVVM-ready state handling with Flow-based state containers.
- Offline-first foundation via repository abstractions and local-first data flow.

### 2) Module structure
- `app`: Android entry point, root Compose host.
- `core:common`: shared constants/utilities (extension point).
- `core:model`: domain models (`WorkspaceFile`, `DocumentType`, `EditorTab`).
- `core:designsystem`: theme tokens and Material 3 theming.
- `core:navigation`: typed route definitions + NavHost.
- `core:database`: Room integration module foundation.
- `core:data`: repository contracts and in-memory implementation.
- `feature:dashboard`: workspace home and quick actions.
- `feature:filemanager`: file browser foundation.
- `feature:editor`: editor engine and editor workspace shell.

### 3) Dependency setup
- Version catalog in `gradle/libs.versions.toml`.
- Core stack configured: Kotlin, Compose, Navigation Compose, Coroutines/Flow, Room, Hilt, DataStore, Firebase BOM, Coil.
- Ready to extend with Apache POI and MuPDF in next iteration.

### 4) Navigation system
- Typed destination model in `core:navigation`.
- Centralized graph with:
  - Dashboard
  - File Manager
  - Editor

### 5) Theme system
- `NexoraTheme` with dark/light color schemes and custom palette.
- Pro surface/contrast baseline for desktop-like UX.

### 6) Main dashboard UI
- Quick action cards for document creation and workspace actions.
- Recent file list section with navigation to editor.

### 7) File manager
- Unified file list UI foundation with typed document metadata.
- Click-to-open file route ready for local/cloud integration.

### 8) Editor engine foundation
- `EditorEngine` with state model and tab lifecycle APIs:
  - open tab
  - mark dirty
  - autosave-ready state
- `EditorScreen` shell with multi-mode toolbar chips and editing canvas placeholder.

## Architecture principles implemented
- Multi-module Gradle design
- Separation of concerns
- Repository pattern
- Dependency inversion via interfaces
- Reusable Compose components
- State-driven UI
- Offline-first-ready approach

## Run (next step)
1. Open in Android Studio or Cursor Android tooling.
2. Sync Gradle.
3. Run `:app` on API 26+ emulator/device.

## Recommended next milestones
1. Add Room entities/DAO + DataStore preferences.
2. Integrate Apache POI and MuPDF engines in dedicated modules.
3. Introduce Hilt module bindings for repository/data sources.
4. Add tabbed workspace docking panels and keyboard shortcuts.
5. Add Firebase sync pipeline with conflict resolution.
6. Add AI service abstraction and pluggable provider implementations.
