# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). While the major version is 0 the mod is in alpha and every
release is a GitHub pre-release; a release marks a milestone (a whole production line), a new milestone bumps the minor
number and fixes between milestones bump the patch number. The tag, the JAR and
the release title always carry the same version.

## [Unreleased]

### Added
- **Computers, first appearance (models only, no function yet):** six single-block devices you can place on the ground and that face you (Card Reader, Card Punch, Paper Tape Reader/Punch, CRT Terminal, Chart Recorder, Wall Panel), ten multiblocks formed with the Engineer's Hammer (Mainframe, I/O Processor, Memory Cabinet, Disk Unit, Tape Unit, Operator Console, Printer, Teletype, Power Cabinet, Data Cabinet), the Data Cable Coil Block, the Power Wire (laid with the Link Cable through the conduits, from one computer port to another; connections only for now) and 28 panel modules (commands, indicators and the universal text/colour ones) in the creative tab and JEI.
- **Conduit:** the computers' cable tray. It connects by itself to the conduits on all six sides and to the back of the devices, runs vertically as a closed duct, takes a support on the face it was placed on, and the Engineer's Hammer cuts and rejoins each connection. No wires yet.

### Planned (not started)
- The mod's own computers: multiblocks built with the Control Engineering Block, a data network with player-named
  variables, operator console with modules, analogue gauge panel, digital signal board, and expansion chips that make
  the machines report every phase of their process.

### Line 2, part 3: Electrolyzer and new interfaces (built 2026-11-01)

The hydrogen of line 2, and a new look for every machine's interface and JEI page.

### Added
- **Electrolyzer** (5×7×5, 106 blocks): splits water into **Hydrogen**, for the Conversion Unit, and **Oxygen**. Each
  5 s cycle takes 250 mB of water and gives 500 mB of hydrogen and 250 mB of oxygen, drawing 160 IF/t; steam works
  too, with 110 IF/t (provisional numbers). A full hydrogen tank stops it; a full oxygen tank lifts the relief valve,
  which vents the gas. The cell stacks glow and bubble while it runs; sounds for the cells, the rectifier, the pumps,
  the feed and the vent; animated GUI; JEI page; Engineer's Manual page.
- **Hydrogen** and **Oxygen** fluids (gases, shared with other mods by name).
- For the next stages (registered, not used yet): the five **Uranium Hexafluoride** fluids (natural, low, medium,
  high, depleted), **Fluorination Ash**, **Depleted Uranium Oxide** and **Depleted Uranium Ingot** (IE's Arc Furnace:
  the oxide with coke dust).
- **Chemical Engineering Block**, a fifth engineering block, used in the line 1 and line 2 structures.
- Hit boxes that follow each machine's 3D model (selection, collision and aim).
- Hwyla/Waila shows a formed machine's name and 3D model.

### Changed
- Every machine's interface redrawn: the machine itself between riveted side panels, with IE's inventory in the
  middle, shaded pipes and vessels, one status lamp and a single grid for tanks, slots and energy.
- Every JEI page redrawn as a simplified copy of the machine's interface, within JEI's window.
- The structures of the Filtration Unit, the Chemical Separation Unit, the Metal Extractor, the Precipitation Unit, the
  Industrial Dryer, the Sulfuric Acid Unit and the Hydrofluoric Acid Kiln, rebuilt with Chemical Engineering Blocks
  and steel (see each machine's page in the Engineer's Manual or JEI).
- New 3D models of the Chemical Processing Unit, the Sulfuric Acid Unit and the Hydrofluoric Acid Kiln.
- Machine models in the inventory face the player.
- Pick-block on a formed machine gives the block that part replaced; the machine items are no longer in the creative
  tab, which lists the mod's items by groups.

### Line 2, part 2: Fluorite and Hydrofluoric Acid Kiln (built 2026-10-31)

The hydrofluoric acid of line 2, made in the mod from its own fluorite; machines that glow, look and sound alive.

### Added
- **Fluorite ore** in the Overworld's stone (layers 10 to 60, veins of about 8 blocks), breaking into 1-2 Fluorite
  (more with Fortune); IE's Crusher grinds the ore into 3 Fluorite Dust (a crystal into 1); a furnace smelts the ore
  into Fluorite. Also a new vein of IE's Excavator (fluorite with galena and quartz). Shared ore dictionary names
  (`oreFluorite`, `gemFluorite`, `dustFluorite`), so other mods' fluorite works too. `config/immersivenuclear.cfg`
  turns the generation or the Excavator vein off, or changes the veins.
- **Hydrofluoric Acid Kiln** (5×7×3, 84 blocks): a rotary kiln. Each 10 s cycle takes 1 Fluorite Dust and 500 mB of
  Sulfuric Acid and gives 500 mB of Hydrofluoric Acid and 1 Gypsum, drawing 64 IF/t (provisional numbers). Its copper
  drum turns on its rollers, driven by the motor, the pinion and the girth gear; its heating coils and heaters glow
  while it runs. Sounds for the drum, the gears and the coil; animated GUI; JEI page; Engineer's Manual page.
- **Hydrofluoric Acid** fluid (corrosive and toxic) and **Gypsum**, with three uses: back to Quicklime and sulfur in
  IE's Arc Furnace with coke dust; in IE's Mixer in place of the clay of concrete, for more concrete; and building
  blocks: 4 Gypsum make a Plaster Block, which becomes Plaster Tiles, Plaster Bricks, and their slab and stairs.
- Every fluid of the mod can be poured from its bucket (steam rises).
- Glowing parts, shining in the dark without lighting the world: the dryer's heaters, the Sulfuric Acid Unit's flame
  and instrument lamp, the Filtration Unit's and the kiln's signal lamps.

### Changed
- Every interface lined up on one grid (tanks, process parts, slots, energy bar and button).
- JEI and the creative tab list the mod's items in order: machines, line 1, line 2, building blocks, engineering,
  computers.
- Steam is grey; steam and hydrofluoric acid animate more slowly.

### Line 2, part 1: Sulfuric Acid Unit (built 2026-10-31)

The first machine of line 2 (enrichment): the mod now makes its own sulfuric acid, the start of its own
hydrofluoric acid, so the line does not depend on other mods.

### Added
- **Sulfuric Acid Unit**: the contact process in a 5×7×8 multiblock (126 blocks). The sulfur burns with a blue flame
  in the air from the blower, a waste heat boiler cools the gas and makes steam, a converter with four catalyst beds
  makes sulfur trioxide and an absorption tower turns it into sulfuric acid; the clean gas leaves by the stack. Each
  5 s cycle takes 1 sulfur and 500 mB of water and gives 500 mB of Sulfuric Acid and 250 mB of Steam, drawing 32 IF/t
  (provisional numbers). Sulfur from any mod is taken (dust, gem or crystal); a block of sulfur makes one cycle nine
  times as long, with nine times the water and the output.
- Its GUI, an animated flow diagram (flame, glowing boiler tubes, the gas passing through the catalyst beds, acid
  trickling down the packing), with bucket slots for every tank; JEI page; Engineer's Manual page.
- Burner flame that lights only while the machine runs; smoke from the stack; with the steam tank full, the steam
  drum's relief valve vents with a hiss. Sounds for the burner, the pump and the vent.
- **Sulfuric Acid** and **Steam** fluids, animated, with buckets. The steam is shared with other mods by name
  (`steam`), so their steam machines can take it.
- Hazard tooltips: sulfuric acid is corrosive and an oxidizer; steam is hot.

## [0.1.0] - 2026-10-27

Line 1 is complete: from uranium dust to yellowcake, five machines plus a branch, all as Immersive Engineering style
multiblocks.

### Added
- **Chemical Processing Unit**, **Filtration Unit** (filter press) and **Chemical Separation Unit** (two-stage solvent
  extraction), with the reagents, solvents and fluids they need.
- **Metal Extractor**: recovers metals from the effluent.
- **Precipitation Unit**: turns the purified solution into wet uranium concentrate.
- **Industrial Dryer** with a drum loading station: wet concentrate becomes yellowcake, packed into **Concentrate Drums**.
- Four engineering blocks, Agglomerate building blocks, link cables and the first computer parts.
- Animated interfaces, sounds, glowing parts, JEI pages and Engineer's Manual entries for every machine. Energy, time
  and amounts are provisional.

[Unreleased]: https://github.com/FlipThunder2607/Immersive-Nuclear/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.0
