# FasterSleep testing guide

This repository owns its handwritten unit/regression tests. Sentinel Sim is an additional built-artifact/runtime compatibility layer; it is not the storage location for FasterSleep's normal tests.

## What this test-hardening branch adds

The owner-directed hardening work establishes the repository's first normal JUnit 5 harness and covers the deterministic plugin surface without changing production behavior.

### `ConfigLoaderTest`

Protects configuration parsing and fail-closed validation:

- shipped defaults;
- target-world trimming;
- inclusive night tick bounds;
- new active-sleeper speed key and legacy fallback/precedence;
- fixed-point speed rounding;
- recent-sleeper contribution;
- phantom seconds -> ticks conversion;
- disabled phantom reset (`-1`);
- blank world names;
- negative/out-of-range tick and speed values;
- values too large for the in-memory representation.

### `SleepConfigTest`

Protects target-world matching, inclusive night boundaries, and phantom-reset disabled semantics.

### `PlayerSleepStateTest`

Protects the per-player state machine:

- fresh state;
- idempotent sleep start;
- multi-session accumulation;
- backwards-time clamping;
- harmless stop-without-start;
- independent bed/recent/phantom flags.

### `SleepServiceTest`

Protects the core behavior:

- target-world/night filtering;
- bed-enter/leave membership;
- quit and world-change cleanup;
- zero-threshold phantom reset;
- message placeholder/speed/color formatting;
- reload clearing and replacement configuration;
- active-sleeper night advancement;
- fixed-point fractional carry between ticks;
- stale/offline player removal;
- night-end state clearing.

The tick tests use Mockito static doubles for Bukkit's global world/player lookup. They test FasterSleep's arithmetic and state transitions; they do **not** claim a real Paper scheduler/world integration pass.

### `SleepCommandTest`

Protects `/sleepmultiplier reload`:

- usage for missing/extra arguments;
- case-insensitive `reload`;
- permission denial;
- success/failure result messages;
- no reload on invalid/unauthorized input;
- tab-completion shape.

### `BukkitSleepListenerTest`

Protects adapter forwarding:

- only successful bed-enter events are accepted;
- optional/empty feedback is suppressed correctly;
- formatted feedback is sent to the entering player;
- leave/quit/kick/death cleanup is delegated;
- source-world information is preserved on world changes.

### `PluginDescriptorContractTest`

Protects `plugin.yml` identity and the reviewed command/permission contract:

- plugin name/main class/version/API version;
- exact command surface;
- reload permission;
- usage and alias;
- operator-default reload permission.

## Running tests locally

Requirements:

- JDK 21 or newer compatible with the current Paper dependency;
- Maven 3.9+ recommended;
- network access to the Paper Maven repository for a cold dependency cache.

Run the entire suite:

```bash
mvn -B clean test
```

Run the same validation used by the existing Sentinel artifact workflow:

```bash
mvn -B clean verify
```

Run one class:

```bash
mvn -B -Dtest=SleepServiceTest test
```

Run selected classes:

```bash
mvn -B -Dtest=ConfigLoaderTest,SleepCommandTest test
```

## Where results are stored

Maven Surefire writes:

- XML: `target/surefire-reports/TEST-*.xml`
- text summaries: `target/surefire-reports/*.txt`

GitHub Actions is the durable exact-head evidence source. The existing `.github/workflows/sentinel-artifact.yml` runs `mvn --batch-mode --no-transfer-progress clean verify` before it stages/uploads the plugin JAR, so a successful artifact run also proves the repository-local test suite passed on that exact head.

Do not report a green run from an older commit as evidence for a changed branch.

## Failure triage

### Configuration-test failure

Do not simply update expected values. Determine whether the configuration contract intentionally changed. If so, update `config.yml`, production validation, tests, and user documentation together.

### Core service/state failure

Treat fixed-point arithmetic, cleanup, night boundaries, and phantom-reset failures as behavioral regressions until proven otherwise. Avoid weakening assertions to match an accidental behavior change.

### Command/listener failure

Check authority/permission handling and whether an event is being forwarded or suppressed at the correct adapter boundary. Do not add Bukkit-global state to a unit test just to make it pass.

### CI fails before checkout / has no steps

That is infrastructure evidence, not a Java/test failure and not a pass. Preserve the exact head and retry only when runner availability materially changes.

### Sentinel artifact failure after tests pass

Keep packaging/provenance failures distinct from JUnit behavior failures. A successful unit suite does not prove a correctly packaged deployable JAR.

## Remaining boundaries

This suite deliberately does not claim to cover everything a real server can do. High-value follow-up evidence remains:

- full `SleepMultiplier.onEnable()`/`onDisable()` lifecycle with a Paper-compatible plugin loader;
- Bukkit scheduler registration/cancellation;
- a real Paper world moving through night while players enter/leave beds;
- actual `TIME_SINCE_REST` behavior against a server player implementation;
- reload behavior with a real Bukkit `FileConfiguration` loaded from disk;
- compatibility with future Paper API changes.

Those are appropriate for MockBukkit only if the current plugin/Paper version is genuinely supported; otherwise use a disposable real-Paper harness or Sentinel profile. Do not change production modifiers/metadata merely to satisfy a simulator.

## Maintenance rules for future workers

When FasterSleep behavior changes:

1. reconcile live GitHub/open PR ownership first;
2. add or update the focused repository-local tests in the same feature area;
3. include negative/boundary behavior, not just happy paths;
4. preserve exact permission/config contracts unless the product change intentionally changes them;
5. run focused tests, then `mvn -B clean verify`;
6. inspect Surefire XML/text output on failure;
7. keep production credentials/player data out of fixtures;
8. update this guide when test layout, commands, or evidence meaning changes;
9. reconcile Sentinel separately when built-artifact dependencies/runtime expectations change.

A green test suite means the automated contracts above passed. It does not substitute for real-Paper evidence where the behavior depends on Bukkit scheduler/world internals.
