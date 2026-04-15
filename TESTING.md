# SleepMultiplier Testing Checklist

## Status Summary
- Current plugin version: `1.1.0`
- Target runtime: Leaf/Paper-compatible `1.21.11`, Java `21`
- Main feature set under test:
  - sleep acceleration in one configured world
  - separate contribution for players actively in bed and players who already slept that night
  - configurable phantom reset timing based on real time spent sleeping
  - `/sleepmultiplier reload` and `/fastersleep reload`
- No GUIs, storage backends, databases, Vault hooks, ProtocolLib hooks, PlaceholderAPI hooks, or external integrations are present.

## Known Acceptable Limitations
- Weather or thunderstorm sleep support is not implemented. Only the configured night tick window is used.
- Phantom reset timing uses real elapsed in-bed time during server execution, but cannot compensate for a fully frozen server process.
- Reload intentionally clears active night state rather than trying to preserve current sleepers across config changes.
- The plugin is designed for one configured world, not per-world independent behavior.

## Recommended Testing / Release Approach
1. Test first on a staging server running the same Leaf build and Java version as production.
2. Use at least two player accounts for behavior tests so active and recent sleeper interactions can be observed correctly.
3. Run one pass with default config values.
4. Run one pass with non-default values:
   - `speed.extra-ticks-per-active-sleeper`
   - `speed.extra-ticks-per-recent-sleeper`
   - `phantoms.disable-after-seconds-in-bed`
   - `world.target-name`
5. After staging validation, release to production at the start of a fresh night cycle so behavior is easy to observe.

## Ordered Manual Testing Checklist
1. Start the server with the plugin installed and confirm no startup exceptions appear.
2. Confirm the plugin loads with the expected name and version.
3. Confirm the configured target world exists and is loaded.
4. Confirm sleep acceleration does not apply during daytime.
5. Enter a bed at night in the configured world with one player.
6. Confirm the player receives the sleep message once on bed entry.
7. Confirm night starts accelerating only while the player is actually sleeping.
8. Leave the bed immediately with `speed.extra-ticks-per-recent-sleeper: 0.0`.
9. Confirm acceleration stops immediately after leaving the bed.
10. Set a non-zero `speed.extra-ticks-per-recent-sleeper`, reload the plugin, and repeat the test.
11. Confirm acceleration continues after leaving the bed for the rest of that same night.
12. Re-enter and leave the bed multiple times in the same night.
13. Confirm the player is never double-counted as both active and recent in the same tick.
14. Confirm tapping a bed and leaving before actual sleeping does not grant recent-sleeper contribution for the rest of the night.
15. Test with two or more players sleeping at once.
16. Confirm active sleeper contribution scales by the configured amount per actual sleeping player.
17. Have one player remain in bed and another leave after sleeping.
18. Confirm the combined speed reflects one active sleeper plus one recent sleeper, not two active sleepers.
19. Let night end naturally.
20. Confirm all prior sleeper state is cleared at daybreak and does not carry into the next night.

## Phantom Reset Checks
1. With `phantoms.disable-after-seconds-in-bed: 30`, stay in bed for less than 30 seconds.
2. Confirm that session does not count as phantom-resetting sleep.
3. Stay in bed for more than 30 seconds during one night.
4. Confirm that session does count as phantom-resetting sleep.
5. Set `phantoms.disable-after-seconds-in-bed: 0`, reload, and confirm phantom protection is granted immediately on valid bed entry.
6. Set `phantoms.disable-after-seconds-in-bed: -1`, reload, and confirm the plugin never resets `TIME_SINCE_REST`.
7. With a positive threshold, enter and leave bed multiple times in the same night.
8. Confirm the plugin counts cumulative actual in-bed time and grants phantom reset only after the configured total is reached.

## Reload / Restart / Shutdown Checks
1. Begin an accelerated night with at least one active sleeper.
2. Run `/sleepmultiplier reload`.
3. Confirm the command succeeds and no console exception appears.
4. Confirm active acceleration stops immediately after reload because active night state is intentionally cleared.
5. Confirm the next valid bed entry re-registers sleepers normally.
6. Stop the server during an active night.
7. Confirm shutdown completes cleanly without task-cancellation errors.
8. Start the server again and confirm no sleeper state persists from the prior run.
9. Test `/fastersleep reload` alias and permission enforcement.

## Player Cleanup and State Transition Checks
1. While counted as an active sleeper, disconnect.
2. Confirm contribution is removed immediately.
3. Repeat using a kick instead of a normal quit.
4. While counted as an active sleeper, die.
5. Confirm contribution is removed immediately.
6. While counted in the configured world, change to a different world.
7. Confirm contribution is removed immediately.
8. If `speed.extra-ticks-per-recent-sleeper` is non-zero, leave the bed after sleeping and then disconnect.
9. Confirm recent-sleeper contribution is removed immediately.

## Config and Command Checks
1. Set `world.target-name` to a different valid world, reload, and confirm only that world is affected.
2. Set `world.target-name` to an unloaded or invalid world, reload, and confirm:
   - no crash occurs
   - no acceleration occurs
   - a warning is logged if there was active tracked state
3. Set invalid numeric values in config and confirm reload fails cleanly without corrupting runtime state.
4. Confirm the plugin continues operating with the previous valid config after a failed reload.
5. Confirm legacy compatibility:
   - remove `speed.extra-ticks-per-active-sleeper`
   - keep `speed.extra-ticks-per-sleeper`
   - confirm active sleeper speed still uses the legacy value

## Performance / Hot-Path Checks
1. Test with a realistic number of online players in the configured world, including players not using beds.
2. Confirm no lag spike appears when several players enter or leave beds in quick succession.
3. Observe one full night with multiple active/recent sleepers and confirm TPS remains stable.
4. If available, use timings or profiler data to confirm the plugin’s repeating task remains negligible under expected load.
5. Confirm there is no repeated disk access during normal ticking, bed entry, or bed leave.

## Edge Cases To Verify
1. Sleep in a non-target world and confirm the plugin does nothing.
2. Try entering bed outside the configured night window and confirm the plugin does nothing.
3. Reload while the configured world is unloaded and confirm no exception occurs.
4. Test a night where no player sleeps and confirm there is no stray acceleration.
5. Test a night where a player sleeps long enough for phantom reset, then leaves bed, rejoins, and sleeps again the same night.
6. Confirm per-night state still clears correctly at daybreak.

## Final Release Decision
- Release is acceptable after the checklist above passes on staging.
- If any of the following fail, block release:
  - bed tap abuse still grants recent-sleeper contribution
  - phantom threshold does not behave as configured
  - reload leaves stale acceleration active
  - quit/kick/death/world-change cleanup leaves ghost contribution behind
  - target-world misconfiguration causes exceptions or repeated spam
