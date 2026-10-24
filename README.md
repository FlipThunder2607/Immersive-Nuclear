# Immersive Nuclear

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62b47a)
![Forge](https://img.shields.io/badge/Forge-14.23.5-e04e14)
![Immersive Engineering](https://img.shields.io/badge/Immersive%20Engineering-0.12--98-f5a623)
![Status](https://img.shields.io/badge/status-alpha-orange)
![License](https://img.shields.io/badge/license-GPL--3.0-blue)

An Immersive Engineering addon that brings the nuclear fuel cycle to life: process uranium through chemical multiblock machines, enrich it, run reactors and manage heat, power and waste, all in the Immersive style.

> **Early alpha.** The mod is under active development. Machines, recipes, numbers and registry names may still change between versions. Do not use it in worlds you care about yet.

---

## About

Immersive Nuclear follows the real nuclear fuel cycle, step by step, with big multiblock machines built in the world and formed with the Engineer's Hammer, just like Immersive Engineering's own.

Design goals:

- **Immersive first.** Machines look and behave like IE machines: real structures, animated parts, IE-style GUIs, JEI pages and sounds.
- **An ecosystem, not a black box.** No machine works alone. Each step needs products, energy and by-products from other IE machines and from the rest of the line.
- **Grounded, not a chemistry exam.** Processes are inspired by real uranium processing, simplified into game mechanics. Reagents are game abstractions.
- **IE is enough.** The whole line is playable with Immersive Engineering alone. Other mods (such as Immersive Petroleum) may offer better optional routes, never mandatory ones.
- **No computers required.** Machines are controlled with redstone and IE tools.

## Current content (0.1.1-alpha)

### Machines

| Machine | Size | What it does |
|---|---|---|
| **Chemical Processing Unit** | 3×6×3 | Batch leaching reactor. Turns IE uranium powder, Processing Reagent and water into **Uranium Slurry**. |
| **Filtration Unit** | 3×3×9 | Filter press. Splits the slurry into **Crude Uranium Solution** and **Wet Mineral Residue**. Uses 28 replaceable filters that wear out, with ports to automate the swap. Fully animated: actuators, plates and a discharge conveyor. |
| **Chemical Separation Unit** | 5×4×7 | Solvent extraction in two mixer-settler stages. Turns the crude solution into **Purified Uranium Solution** and **Chemical Effluent**; the solvent goes round inside and only its losses are topped up. |

All machines have:

- GUI drawn as an animated flow diagram, in graphite steel with IE's own slots, tanks and status lights
- redstone control (a signal stops the machine, the hammer inverts it)
- JEI recipe pages
- looping and event sounds
- blue input ports and orange output ports

### Items, fluids and blocks

- **Items:** Processing Reagent, Filter, Wet Mineral Residue, Quicklime, Neutralization Cake, Precipitating Agent, Wet Uranium Concentrate, Uranium Concentrate (Yellowcake)
- **Fluids** (animated, with buckets): Uranium Slurry, Crude Uranium Solution, Extraction Solvent, Stripping Solution, Purified Uranium Solution, Chemical Effluent, Neutralized Sludge, Depleted Effluent, Process Condensate
- **Blocks:** Impure Agglomerate, a heavy building block made from process waste
- **Hazard tooltips** on materials: radioactive, corrosive, flammable, toxic, oxidizer, caustic. They are informative only for now.

Some items and fluids are already registered for the next machines. Until those machines exist, they can only be stored.

## Production line

Line 1 takes IE uranium powder to yellowcake, the non-enriched uranium concentrate.

```
Uranium powder (IE)
  └─► [1] Chemical Processing Unit ──► Uranium Slurry
        └─► [2] Filtration Unit ──► Crude Uranium Solution        (+ Wet Mineral Residue)
              └─► [3] Chemical Separation Unit ──► Purified Solution   (+ Chemical Effluent)
                    └─► [4] Precipitation Unit ──► Wet Concentrate    (+ Chemical Effluent)
                          └─► [5] Industrial Dryer ──► Yellowcake     (+ Process Condensate)
```

Machines 1 to 3 are playable. Machines 4 and 5 are being designed.

A branch after machine 3, the **Metal Extractor** (machine 3.5), will recover dissolved metals from the effluent in
an electrolytic cell before it is treated.

## Roadmap

**Line 1: uranium powder to yellowcake**
- [x] Chemical Processing Unit
- [x] Filtration Unit
- [x] Chemical Separation Unit (solvent extraction: extraction and stripping stages)
- [ ] Metal Extractor (machine 3.5: electrolytic recovery of metals from the effluent)
- [ ] Precipitation Unit
- [ ] Industrial Dryer
- [ ] Concentrate Drum (storage)
- [ ] Effluent treatment: recycling, neutralization with quicklime
- [ ] Balancing pass (energy, time and quantities are placeholders)

**Hazards**
- [ ] Contamination system with four kinds of exposure: radiological, chemical, particulate (inhaled dust) and physical (fire, pressure, heat, explosion)
- [ ] Protective equipment, shielding, dosimetry and decontamination

**Later stages**
- [ ] Conversion and enrichment
- [ ] Fuel fabrication
- [ ] Reactors, with an analog control and automation system (see [`TODO_List_automation.md`](TODO_List_automation.md))
- [ ] Spent fuel and waste management

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.12.2 |
| Minecraft Forge | 14.23.5.2847 or newer |
| [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) | 0.12-98 or newer, **required** |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | optional, for recipe pages |

## Installation

There are no public releases yet. Development builds are attached to [Releases](../../releases) when available.

1. Install Minecraft Forge for 1.12.2.
2. Put Immersive Engineering and the Immersive Nuclear JAR in the `mods` folder.
3. Start the game. The machines appear in the *Immersive Nuclear* creative tab, and their structures are described on their JEI pages.

## Building from source

You need a **JDK 8**. Newer JDKs do not work with ForgeGradle 2.

```sh
git clone https://github.com/FlipThunder2607/Immersive-Nuclear.git
cd Immersive-Nuclear
./gradlew build        # Windows: gradlew.bat build
```

The JAR is written to `build/libs/immersivenuclear-1.12.2-<version>.jar`.

- The first build downloads Gradle, Forge, the Minecraft mappings and the dependencies, so it takes a while.
- `build` also runs the multiblock structure tests.
- To run a development client, use `./gradlew setupDecompWorkspace`, then `./gradlew runClient`, or generate IDE files with `./gradlew idea` / `./gradlew eclipse`.

## Contributing

Bug reports and suggestions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## License

Immersive Nuclear is licensed under the [GNU General Public License v3.0](LICENSE).

## Credits

- [Immersive Engineering](https://github.com/BluSunrize/ImmersiveEngineering) by BluSunrize and contributors. This addon builds on its API and references its textures at runtime.
- Some item icons are remixes of Immersive Engineering textures. They are not covered by this mod's GPL-3.0: they stay under [Immersive Engineering's license](https://github.com/BluSunrize/ImmersiveEngineering/blob/master/LICENSE) and are used under its clause for addons.
