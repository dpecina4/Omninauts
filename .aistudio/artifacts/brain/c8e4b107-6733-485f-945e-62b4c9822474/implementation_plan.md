# Omninauts: Expedition Requirements Info Button & Missing Requirements Pop-up

Add an `(i)` requirements info button to the bottom action bar next to the prepare/launch button on each expedition card, and ensure tapping locked or restricted expeditions opens a comprehensive requirements pop-up dialog listing all unmet criteria.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Confirmed through user preferences:
> - **Info Button Placement**: Positioned in the bottom action bar next to the "Prepare Squad" / launch button on each sector card and procedural scanner card.
> - **Locked Tap Behavior**: Tapping a locked or restricted expedition (or its button) opens a clean requirements modal dialog listing all missing requirements (e.g. required tutorial progression, unlocked elemental labs, and biome element restrictions).

---

## 1. Overview & Core Concept

- **What It Does**: Provides clear visibility into expedition requirements via an `(i)` info button on every expedition card. When tapping locked or restricted expeditions, players receive an immediate, helpful requirements pop-up explaining precisely what needs to be accomplished (e.g., clearing preceding tutorial sectors or unlocking required elemental labs).
- **Key Value**: Eliminates user confusion when an expedition button is unclickable, guiding players toward base lab upgrades and tutorial progression.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Expedition Requirements Inspection (`(i)` Button)**:
   - Located on the bottom action bar of each sector card alongside "Prepare Squad".
   - Tapping opens the **Expedition Requirements Dialog** displaying:
     - Sector Name & Subtitle
     - Biome restrictions & allowed element types (Pyronauts, Hydronauts, Voltnauts)
     - Weather condition effects
     - Prerequisite tutorial status (e.g., "Cleared" vs "Requires Tutorial 01")
2. **Locked / Restricted Tap Feedback**:
   - Tapping a locked sector card or its button now successfully opens the requirements dialog listing missing prerequisites (instead of doing nothing).
   - Unlocked sectors open the normal `SquadDeploymentModal`.

---

## 3. Technical Architecture & Data Strategy

### Component & State Changes

- **SectorCard & ProceduralScannerCard**:
  - Add an `Info` (`(i)`) button in the bottom action bar alongside the primary action button.
  - Expose `onShowRequirements: (SectorSpec) -> Unit` callback.
- **Requirements Dialog (`SectorRequirementsDialog`)**:
  - A Material 3 Dialog styled with Cavern Alloid theme tokens, listing prerequisites, biome restrictions, weather conditions, and clearance status.
- **BaseHubScreen Content**:
  - Manage state for `pendingRequirementsSector: SectorSpec?`.
  - When a locked sector is clicked, trigger `pendingRequirementsSector = sector` to show requirements. When an unlocked sector is clicked, trigger `pendingSquadSector = sector` to open squad deployment.

---

## 4. Verification Plan

1. **Automated Unit & Robolectric Tests**:
   - Run `gradle :app:testDebugUnitTest` to verify all test suites pass.
2. **Build & APK Verification**:
   - Compile via `compile_applet`.
   - Re-assemble debug APK and update `/Omninauts.apk`.
