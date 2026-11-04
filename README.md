# Immersive Nuclear

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62b47a)
![Forge](https://img.shields.io/badge/Forge-14.23.5-e04e14)
![Immersive Engineering](https://img.shields.io/badge/Immersive%20Engineering-0.12--98-f5a623)
![Status](https://img.shields.io/badge/status-alpha-orange)
![License](https://img.shields.io/badge/license-GPL--3.0-blue)

An Immersive Engineering addon that brings the nuclear fuel cycle to life: process uranium through chemical multiblock machines, enrich it, run reactors and manage heat, power and waste, all in the Immersive style.

> **Early alpha.** The mod is under active development. Machines, recipes, numbers and registry names may still change between versions. Do not use it in worlds you care about yet.
>
> **Releases:** [0.1.0](../../releases/tag/v0.1.0) is the first pre-release and completes **line 1** (uranium powder to yellowcake). Line 2 (conversion and enrichment) is being built on this branch and will ship as **0.2.0**; what is marked *(0.2.0)* below is in the source but not in the 0.1.0 JAR.

**Wiki:** [flipthunder2607.github.io/Immersive-Nuclear-wiki](https://flipthunder2607.github.io/Immersive-Nuclear-wiki/). Every machine, item and fluid, the production lines as interactive flow charts and the plans for enrichment and fuel fabrication (in Portuguese for now).

---

## About

Immersive Nuclear follows the real nuclear fuel cycle, step by step, with big multiblock machines built in the world and formed with the Engineer's Hammer, just like Immersive Engineering's own.

Design goals:

- **Immersive first.** Machines look and behave like IE machines: real structures, animated parts, IE-style GUIs, JEI pages and sounds.
- **An ecosystem, not a black box.** No machine works alone. Each step needs products, energy and by-products from other IE machines and from the rest of the line.
- **Grounded, not a chemistry exam.** Processes are inspired by real uranium processing, simplified into game mechanics. Reagents are game abstractions.
- **IE is enough.** The whole line is playable with Immersive Engineering alone. Other mods (such as Immersive Petroleum) may offer better optional routes, never mandatory ones.
- **No computers required.** Machines are controlled with redstone and IE tools.

## Current content

Machines 1 to 5 and the Metal Extractor are in release 0.1.0. The last three machines of the table and the line 2 items belong to 0.2.0 (build from source to try them).

### Machines

| Machine | Size | What it does |
|---|---|---|
| **Chemical Processing Unit** | 3×6×3 | Batch leaching reactor. Turns IE uranium powder, Processing Reagent and water into **Uranium Slurry**. |
| **Filtration Unit** | 3×3×9 | Filter press. Splits the slurry into **Crude Uranium Solution** and **Wet Mineral Residue**. Uses 28 replaceable filters that wear out, with ports to automate the swap. Fully animated: actuators, plates and a discharge conveyor. |
| **Chemical Separation Unit** | 5×4×7 | Solvent extraction in two mixer-settler stages. Turns the crude solution into **Purified Uranium Solution** and **Chemical Effluent**; the solvent goes round inside and only its losses are topped up. |
| **Metal Extractor** | 4×4×4 | Electrolytic cell. Plates the metals dissolved in the Chemical Effluent onto six electrode plates, which rise to be scraped over a tray: **Mixed Metal Deposit** and **Depleted Effluent**. Three electrode plates (steel, lead-silver, electrum-coated) with growing yield and life. Animated platform, tray and scraper. |
| **Precipitation Unit** | 5×7×5 | Agitated tank and screw dewaterer. Turns the purified solution and the Precipitating Agent into **Wet Uranium Concentrate** and **Chemical Effluent**. Animated agitator and liquid behind the tank's window. |
| **Industrial Dryer** | 5×7×4 | Paddle dryer with heaters and a condenser. Turns the wet concentrate into **Yellowcake**, the end of line 1, and collects the **Process Condensate**. A drum loading station beside it fills **Concentrate Drums** on an animated conveyor. Heaters that glow while they run, steam and dust. |
| **Sulfuric Acid Unit** *(0.2.0)* | 5×7×8 | Contact process, the first machine of line 2. Burns sulfur (any mod's dust, or a block for nine cycles) and turns it into **Sulfuric Acid** with water, for the enrichment line's own hydrofluoric acid; its waste heat boiler gives **Steam**. Blue burner flame, stack smoke and a relief valve that vents when the steam tank is full. |
| **Hydrofluoric Acid Kiln** *(0.2.0)* | 5×7×3 | Rotary kiln: fluorite dust and sulfuric acid into **Hydrofluoric Acid** for the Conversion Unit, with **Gypsum** as by-product. The drum turns on its rollers, driven by a motor through a pinion and the girth gear; the heating coils and heaters glow while it runs. |
| **Electrolyzer** *(0.2.0)* | 5×7×5 | Splits water (or steam, with less power) into **Hydrogen**, for the Conversion Unit, and **Oxygen**. A rectifier feeds two alkaline cell stacks; the gases rise with the electrolyte into their separators. A full oxygen tank is vented by its relief valve. The cells glow and bubble while it runs. |

The effluent never has to be dumped: it goes back into the Chemical Processing Unit as recycled liquor (saving reagent), through the Metal Extractor, or is neutralized with quicklime in IE's Mixer. The Filtration Unit filters the neutralized sludge into Neutralization Cake and plain water, and the cake replaces clay in IE's concrete.

All machines have:

- a GUI that draws the machine itself, animated, between riveted panels with IE's own slots, tanks and status light
- hit boxes that follow the 3D model; Hwyla/Waila shows the machine, while pick-block gives the block a part replaced
- redstone control (a signal stops the machine, the hammer inverts it)
- JEI recipe pages
- looping and event sounds
- parts that glow while they run (flames, heaters, signal lamps), without lighting the world
- blue input ports and orange output ports
- a page in IE's Engineer's Manual, under its own **Immersive Nuclear** category

### Items, fluids and blocks

- **Ores:** Fluorite Ore (Overworld, and a vein of IE's Excavator); Fluorite and Fluorite Dust
- **Items:** Gypsum, Processing Reagent, Filter, Wet Mineral Residue, Neutralized Mineral Residue, Electrode Plates (steel, lead-silver, electrum-coated), Mixed Metal Deposit, Quicklime, Neutralization Cake, Precipitating Agent, Wet Uranium Concentrate, Uranium Concentrate (Yellowcake); for later stages, Fluorination Ash, Depleted Uranium Oxide and Depleted Uranium Ingot (IE's Arc Furnace)
- **Fluids** (animated, with buckets): Uranium Slurry, Crude Uranium Solution, Extraction Solvent, Stripping Solution, Purified Uranium Solution, Chemical Effluent, Neutralized Sludge, Depleted Effluent, Process Condensate, Sulfuric Acid, Steam, Hydrofluoric Acid, Hydrogen, Oxygen (the last five shared with other mods by name), and the five Uranium Hexafluorides of the enrichment line (natural, low, medium, high, depleted; registered, not used yet); every one can be poured from its bucket
- **Blocks:** Concentrate Drum, which keeps its yellowcake when broken; Impure Agglomerate, a heavy building block made from process waste, and the building blocks fired from it (Agglomerate Block, Tile, Bricks, Brick Stairs and Slab); the gypsum Plaster Block and its Tile, Bricks, Brick Stairs and Slab; five engineering blocks (precision, nuclear, control, radioactive handling, chemical), with recipes, that join into connected walls with the CTM mod; Lead Glass
- **Engineering parts:** Remote Manipulator, Precision Bearing, Electronic and Relay Modules
- **Computer parts** (in development, creative tab only): electronic and relay modules, core memory, punched card and paper tape, disk pack, magnetic tape, link cable, continuous form paper, link connector and link relay
- **Hazard tooltips** on materials: radioactive, corrosive, flammable, toxic, oxidizer, caustic, hot. They are informative only for now.

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

Machines 1 to 5 are playable: line 1 is complete.

A branch after machine 3, the **Metal Extractor** (machine 3.5, playable), recovers dissolved metals from the effluent
in an electrolytic cell before it is treated.

Line 2 (enrichment) makes its own hydrofluoric acid: the **Sulfuric Acid Unit** feeds the **Hydrofluoric Acid Kiln**
(both playable), whose acid goes into the Conversion Unit (in development: the first machine of line 2 that makes uranium hexafluoride), and the **Electrolyzer** (playable) makes its
hydrogen. The kiln's gypsum goes back to quicklime and
sulfur in IE's Arc Furnace, into concrete, or into building blocks.

## Roadmap

**Line 1: uranium powder to yellowcake (released in 0.1.0)**
- [x] Chemical Processing Unit
- [x] Filtration Unit
- [x] Chemical Separation Unit (solvent extraction: extraction and stripping stages)
- [x] Metal Extractor (machine 3.5: electrolytic recovery of metals from the effluent)
- [x] Effluent treatment: recycling, neutralization with quicklime, sludge filtration
- [x] Precipitation Unit
- [x] Industrial Dryer
- [x] Concentrate Drum (storage, and the dryer's drum loading station)
- [ ] Balancing pass (energy, time and quantities are placeholders)

**Hazards**
- [ ] Contamination system with four kinds of exposure: radiological, chemical, particulate (inhaled dust) and physical (fire, pressure, heat, explosion)
- [ ] Protective equipment, shielding, dosimetry and decontamination

**Line 2: conversion and enrichment (0.2.0)**
- [x] Sulfuric Acid Unit (contact process)
- [x] Fluorite ore
- [x] Hydrofluoric Acid Kiln
- [x] Electrolyzer (hydrogen and oxygen)
- [ ] Conversion Unit (in development)
- [ ] Enrichment Unit (with a depleted-uranium branch)

**Later stages**
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

Download the JAR of the latest pre-release from [Releases](../../releases) (currently 0.1.0, line 1). Line 2 machines need a build from source until 0.2.0 is released.

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
- Some item icons and block textures (such as the furnace heater faces of the Industrial Dryer) are copies or remixes of Immersive Engineering textures. They are not covered by this mod's GPL-3.0: they stay under [Immersive Engineering's license](https://github.com/BluSunrize/ImmersiveEngineering/blob/master/LICENSE) and are used under its clause for addons.
