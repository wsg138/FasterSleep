# FasterSleep

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/dcd5366e28be4c679146d6e1e7c22e7a)](https://app.codacy.com/gh/wsg138/FasterSleep/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

FasterSleep (plugin name `SleepMultiplier`) speeds up the Overworld night as more players sleep, without requiring a fixed percentage of online players to be in beds.

## Current Enthusia SMP behavior

The current live configuration applies only to the main `world` and uses Minecraft night ticks 12541-23458.

Each player **currently sleeping in a bed** adds **1.5 extra world ticks every server tick**. Since normal Minecraft time already advances by one tick per server tick, the displayed night-speed multiplier is:

- 1 active sleeper -> about **2.5x** normal night speed
- 2 active sleepers -> about **4x**
- 3 active sleepers -> about **5.5x**
- and so on

Players who have slept and then leave their bed remain a recent-sleeper contributor for the plugin's current-night tracking and add **0.5 extra ticks per server tick** while that recent-sleeper state remains active. All sleep contribution state is cleared when night ends.

## Phantom protection

A player must physically sleep for **10 real seconds during the night** before FasterSleep resets their vanilla `TIME_SINCE_REST` statistic. When that threshold is reached, the player receives the configured message that phantoms will no longer spawn for them because of lack of sleep.

Simply touching a bed and immediately leaving it is therefore not enough to reset the phantom timer on the current SMP configuration.

## Player feedback

On valid bed entry during the configured night, the plugin can show:

- total contributing sleepers,
- players actively in bed,
- recent sleepers,
- the resulting effective night-speed multiplier.

It also sends a separate message when the player's phantom timer has actually been reset.

## Scope and safety

- Sleep acceleration only runs in the configured target world.
- State is cleared when night ends.
- Players are removed from tracking when they leave the world, disconnect, or are otherwise no longer valid sleepers.
- The plugin does not change daytime speed.
- It does not require every player or a percentage of players to sleep.

## Administration

`/sleepmultiplier reload` (alias `/fastersleep reload`) reloads the configuration. It requires `sleepmultiplier.reload`, which defaults to operators.

## Build

```powershell
mvn -q -DskipTests package
```

## Documentation source note

The repository's bundled default config is not identical to the live Enthusia SMP config. Future wiki/player documentation should use the current deployment snapshot for exact speed and phantom-delay values. At the time of this documentation pass, the live values are 1.5 active-sleeper contribution, 0.5 recent-sleeper contribution, and a 10-second phantom-reset threshold.