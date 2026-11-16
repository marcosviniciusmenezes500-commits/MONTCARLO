# DroidScope

DroidScope is a native Android monitoring and benchmarking suite designed around the architecture described in the project brief: a unified dashboard for device diagnostics, battery health, hardware metadata, benchmark execution history, and performance telemetry.

## Architecture

The project is intentionally organized as a multi-module Android application, with a clean separation between:

- app: host application and Compose entry point
- core: cross-cutting utilities and system abstractions
- core-ui: shared UI primitives and material components
- core-database: persistence and room schema layer
- core-domain: device, battery, benchmark and system models
- feature-* modules: domain-specific screens and logic

## Module layout

- app/
- core/
- core-ui/
- core-database/
- core-domain/
- feature-dashboard/
- feature-battery/
- feature-benchmark/
- feature-hardware/
- feature-cpu/
- feature-memory/
- feature-storage/
- feature-sensors/
- feature-network/
- feature-display/
- feature-diagnostics/
- feature-history/
- feature-settings/

## Build notes

This project uses:

- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.5.2
- Kotlin 1.9.24
- Material 3 Compose UI

The repository is scaffolded to match the requested DroidScope structure and can be expanded into a complete Android product once the Android SDK and tooling are available.
