# قوت لحفظ النعمة وإدارة المطابخ السحابية (`qoot-surplus-food-1290076`)
> منظومة food المتكاملة مع ربط Kitchen Display System (KDS) TCP/IP Print Server

- **Project ID**: `#1290076`
- **Commercial Archetype**: `food`
- **Bound Hardware Peripheral**: `Kitchen Display System (KDS) TCP/IP Print Server`
- **Package Name**: `sa.factory.qootsurplusfood129`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### 1. Executive Architecture Overhaul
- **KitchenDisplaySystemProtocolEngine:**
  - Implemented TCP/IP socket network protocol engine for commercial cloud kitchen displays and thermal ticket printers.
  - Station-specific line routing across 7 distinct operational zones: EXPEDITER, HOT_LINE, GRILL, COLD_PREP, DRINKS, PACKAGING, and SURPLUS_DISPATCH.
  - Authentic binary ESC/POS kitchen ticket rendering supporting formatting (ESC @, GS V, alignment, bold modifiers, and order instruction comments).
  - TCP framing protocol with STX (0x02) / ETX (0x03) framing and 8-bit longitudinal XOR checksum verification.
  - Life-cycle ticket state transitions and order SLA timer expiration tracking.

- **SurplusFoodInventoryEngine:**
  - Dynamic progressive discount model reducing food waste by automatically marking down near-expiry surplus (30% to 70% off).
  - Critical freshness urgency evaluation across 4 distinct tier levels (Stable, Warning, Critical, Expired).
  - Cold-chain temperature safety compliance engine monitoring hot-holding (60-95 C), refrigeration (0-4.5 C), deep-freezing (-25 to -18 C), and ambient conditions according to SFDA guidelines.
  - Automated Rescue Bundle generation and environmental diversion telemetry (calculating kilograms of food diverted and estimated CO2 equivalent avoided).

- **Hardware Driver & UI Modernization:**
  - Bound dual protocol engines into `HardwareSensorDriver.kt` emitting live telemetry frames reporting active tickets, diverted food weight, and CO2 emissions saved.
  - Modernized UI dividers to Material3 `HorizontalDivider` and resolved single-quote template bugs.
  - Cleaned Arabic typography, application manifests, and enforced 100% Zero-Emoji invariant.
  - Established unit test suite under `app/src/test/java` with 10 comprehensive regression tests (100% assertions green).

---

### 2. Build, Run, and Test Instructions

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Run cloud test harness verification
python3 /root/capture_harness_screens.py --repo-dir "/root/sidekick-workspace/qoot-surplus-food-1290076" --output /tmp/sidekick_shot.png
```
