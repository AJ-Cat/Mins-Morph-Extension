# Min's Morph Extension

An extension fork of [Min's Morph](https://github.com/min2222/Mins-Morph) by
[min01](https://github.com/min2222), maintained by
[AJ-Cat](https://github.com/AJ-Cat).

This project is licensed under the **GNU General Public License v3.0 only**
(`GPL-3.0-only`). See [LICENSE](LICENSE) and [NOTICE](NOTICE) for the original
copyright and the list of modifications.

## Highlights

- Dynamic `/morph <targets> data <dataName> <dataValue>` suggestions for the
  current morph's writable entity data.
- Morph model/entity-data synchronization fixes, including phase-dependent
  models and the client-side model refresh on ability use.
- Removed the vanilla 1024 max-health cap for morphs.
- Automatic flying-morph detection, while `flightMorphOverrides` in the config
  still works as an explicit override.
- Mob effect immunity/inversion handling based on the morphed mob.
- Targetless ability triggering through an invisible close-range dummy target.
- Brain-based AI action triggering in addition to normal `GoalSelector` goals.
  Brain actions are listed as `brain:<activity>:<behavior>` in the goal menu and
  command suggestions.
- Larger third-person camera distance for large morph models.

## Building

Requires Java 17 and a Java 17 JDK for ForgeGradle.

```bash
./gradlew build
```

The built mod is placed in `build/libs/`.

## Upstream

- Original source: https://github.com/min2222/Mins-Morph
- Fork source: https://github.com/AJ-Cat/Mins-Morph-Extension

Because this is a GPLv3 derivative, redistributed binaries must remain under the
GPLv3 and the corresponding source must be made available.
