# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). While the major version is 0 the mod is in alpha and every
release is a GitHub pre-release; each finished feature or fix release bumps the patch number. The tag, the JAR and
the release title always carry the same version.

## [Unreleased]

### Planned
- The mod's own computers: multiblocks built with the Control Engineering Block, a data network with player-named
  variables, operator console with modules, analogue gauge panel, digital signal board, and expansion chips that make
  the machines report every phase of their process.

## [0.1.6] - 2026-10-27

### Added
- **Industrial Dryer** (machine 5), closing line 1: the water is driven out of the Wet Uranium Concentrate and
  **Yellowcake** comes out. A 5×7×4 multiblock (89 blocks): a horizontal drying chamber with paddles and heaters,
  a feed hopper with a rotary valve, a condenser, a condensate tank and a drum loading station. Each batch takes 24
  Wet Uranium Concentrate and 90 seconds: loading, heating up to 140 °C, 60 seconds of drying while the paddles turn
  the concentrate over, then
  cooling and discharge. 16 Yellowcake and 2000 mB of Process Condensate per batch (76800 IF).
  GUI, JEI page (click the chamber in the GUI), a page in the Engineer's Manual, heaters that glow while they run,
  steam rising from the vent stack and a sound for every step.
  A scrubber on the vapour line catches the fines the vapour carries off: every 4 batches they come back as one more
  yellowcake.
- The Industrial Dryer's Process Condensate goes back into the Chemical Processing Unit through its recycled liquor
  tank, in place of part of the water (unlike the effluents, it saves no reagent).
- **Concentrate Drum:** a sealed steel drum for yellowcake (one slot, up to 64). Broken, it keeps its yellowcake inside,
  like a shulker box, so a filled drum travels as a single item. Hoppers, conveyors and pipes fill and empty it; a
  comparator reads how full it is. Crafted like IE's metal barrel in steel sheetmetal, with a Radioactive Handling
  block in the middle.
- Recipes for the four engineering blocks, after IE's own (ingots in the corners, 2 per craft), and three new parts
  for them: Lead Glass (a reinforced glass block, from IE's Arc Furnace: glass and lead dust), the Remote Manipulator and the Precision Bearing. The
  Electronic and Relay Modules get recipes too and go into the Control Engineering block.
- Industrial Dryer: a drum loading station. Concentrate Drums, empty or partly filled, go in at the north end; an
  animated conveyor takes each one under the spout, where the yellowcake pours into it with its own sound and dust,
  and full drums come out sealed with the yellowcake. Without drums the yellowcake comes out loose.
- Building blocks from the Impure Agglomerate (the raw one): fired in a furnace it becomes the Agglomerate Block,
  smooth like IE's concrete, which makes Agglomerate Tiles (as IE's concrete tile), Agglomerate Bricks and their stairs
  and slabs.
- A set of base detail textures for the machines (flanges, hatches, handwheels, vents and more), the Immersive
  Nuclear maker's plate, and a darker graphite finish for the machines' decks.

### Changed
- Precipitation Unit: the structure follows the rebuilt screw ramp (iron sheetmetal blocks and slabs; 93 IE blocks).
- Precipitation Unit: a batch now takes 90 seconds, 60 s mixing and 30 s dewatering (was 20 s and 10 s).
- Precipitation Unit model: one more plate on the tank.
- Precipitation Unit model: more detail (maker's plate, gauge, inlet valve, ladder, railing on the tank's lid) and a
  darker graphite deck; faces that flickered where two parts met no longer do.
- Yellowcake's icon in its real lemon yellow, no longer mistaken for IE's gold dust.
- Every machine's model is now drawn in pieces, each by the block it stands in, so the machines no longer vanish at
  some view angles and a block placed against them no longer turns their faces black.

### Fixed
- Industrial Dryer sounds no longer ring ('plink') at the contactors and the discharge valve.
- Low frame rate near machines with moving parts (Filtration Unit, Metal Extractor, Precipitation Unit).
- The Engineer's Hammer on a machine's redstone port now inverts its redstone control (it did nothing before).

## [0.1.5] - 2026-10-26

### Added
- **Precipitation Unit** (machine 4): the uranium dissolved in the Purified Uranium Solution becomes a solid. A 5×7×5
  multiblock (94 IE blocks): an agitated tank on legs over a conical bottom, and an inclined screw dewaterer. Each batch
  is the full tank, 12000 mB of solution: 6 Precipitating Agents are dosed and mixed, then the screw lifts the solid to
  the chute while the liquid runs back down. 24 Wet Uranium Concentrate and 9000 mB of Chemical Effluent per batch.
  GUI, JEI page (click the tank in the GUI), a page in the Engineer's Manual, animated agitator (three levels of four
  blades) and liquid seen through the tank's window, and sounds for the agitator and the screw.

### Fixed
- Link cables are drawn between the connectors and relays, sag like IE's HV wire (in space grey), can be cut and
  broken, and stay link cables after the game restarts (#5).

## [0.1.4] - 2026-10-25

### Added
- **Engineering blocks**, like IE's light and heavy engineering, as casings for the next machines: Precision
  Engineering (grey), Nuclear Engineering (green), Control Engineering (blue) and Radioactive Handling (yellow). With
  the CTM mod installed, a wall of them joins into one surface, with conduits running from block to block. No recipe
  yet.
- **Computer parts**, the first items of the computers (in development; no recipe and no use yet, from the creative
  tab): Electronic Module, Relay Module, Core Memory, Punched Card, Punched Paper Tape, Disk Pack, Link Cable,
  Magnetic Tape, Continuous Form Paper, and the Link Connector and Link Relay blocks (IE's LV connector model, in
  graphite and brushed steel).

## [0.1.3] - 2026-10-24

### Added
- **Metal Extractor** (machine 3.5), a branch after the Chemical Separation Unit. An electrolytic cell takes the
  Chemical Effluent in 12000 mB batches (fill, prepare, recover, drain): the metals plate out on six hanging electrode
  plates, then the platform lifts them, a tray slides in and a scraper cleans them while the vat drains. Each batch
  gives 6 Mixed Metal Deposits and 12000 mB of Depleted Effluent. GUI, JEI page, animated platform, tray and scraper
  with falling fragments and sparks, HV wires, and sounds for the plates, platform, tray and scraper.
- Electrode Plates for the Metal Extractor, one per plate slot and freely mixed:
  - Steel: the base plate (2 steel rods, copper plate, 2 steel plates);
  - Lead-Silver: more deposit with less energy, twice the life (steel plate + 2 lead plates + silver plate);
  - Electrum-Coated: the most deposit and the longest life (lead-silver plate + 3 electrum plates).
  Every batch wears each plate; a worn-through plate breaks.
- Mixed Metal Deposit, crushed in IE's Crusher into slag and a chance of iron, aluminium, copper, lead, nickel and
  uranium nuggets and clay.
- Neutralized Mineral Residue: 4 Wet Mineral Residue + 1 Quicklime. Crushed in IE's Crusher it gives sand and a
  chance of iron nuggets, clay and uranium nuggets (what the leaching left in the solid).
- Chemical Processing Unit: a recycled liquor tank. The water port also takes Chemical or Depleted Effluent, and up to
  4000 mB of each batch can come from it instead of water, saving up to 4 Processing Reagent.
- Filtration Unit: filters Neutralized Sludge too (1000 mB gives 1 Neutralization Cake and 800 mB of water), one kind
  of feed at a time.
- IE's Mixer makes concrete with Neutralization Cake in place of clay.
- Engineer's Manual: an **Immersive Nuclear** category with an entry per machine (structure, formed preview, texts and
  part recipes), supplies, residues and by-products, and hazard warnings.
- JEI: two pages each for the Chemical Processing Unit (water only, half recycled) and the Filtration Unit (slurry,
  sludge).

### Changed
- Wet Mineral Residue is also corrosive (still soaked in the acid leach): it has to be neutralized before use. The
  Impure Agglomerate is made from the neutralized residue (or from Neutralization Cake, as before).

### Fixed
- The multiblock structures shown in IE's manual (and the projector) were mirrored against the formed machine.

## [0.1.2] - 2026-10-24

### Added
- Recipes:
  - Quicklime, burnt from diorite in IE's Coke Oven (or from limestone, when a mod adds it);
  - Precipitating Agent: 2 Quicklime + 2 saltpeter;
  - Neutralized Sludge in IE's Mixer: Chemical Effluent or Depleted Effluent with Quicklime.
- Chemical Separation Unit: each mixer and each pump has its own sound, played from where it stands in the model.

### Changed
- Processing Reagent: 2 sulfur dust + 2 saltpeter, without the paper.
- The JEI pages of the Chemical Processing Unit and the Filtration Unit are compact copies of their new GUIs.
- Chemical Separation Unit: the solvent reservoir is a surge tank of 18000 mB, and the solvent port only tops it up
  to 6000 mB. The rest is room for the clean solvent coming back from settler B.
- GUI tooltips wrap long lines and stay on the screen.

### Fixed
- The Chemical Separation Unit's JEI recipe page was missing.
- The Chemical Separation Unit could lock up when the solvent port kept the reservoir full: the returning solvent had
  nowhere to go, so both settlers stopped. Machines locked this way start again on their own.

## [0.1.1] - 2026-10-24

### Added
- **Chemical Separation Unit** (5×4×7 multiblock, 118 blocks), solvent extraction in two mixer-settler stages:
  - extraction: mixer A mixes Crude Uranium Solution with clean Extraction Solvent into settler A; settled, the
    loaded solvent goes on and the Chemical Effluent goes to its tank;
  - stripping: mixer B mixes the loaded solvent with Stripping Solution into settler B; settled, the Purified Uranium
    Solution goes to its tank and the clean solvent back to the reservoir, less a small loss topped up through the
    solvent port;
  - the mixers run continuously and each settler works in batches (fill, settle for one minute, drain);
  - GUI drawn as a flow diagram: the settlers show the cloudy mixture separating into two layers, the mixers turn,
    the pipes light up while fluid moves; 8 bucket slots;
  - the settler windows of the model show the liquid and its layers;
  - JEI recipe page, mixer and pump sounds, mirrored placement, redstone control.
- IE Mixer recipes: Extraction Solvent (biodiesel + saltpeter) and Stripping Solution (water + saltpeter).
- A pump sound shared by every machine, played while fluid moves inside it.

### Changed
- **Chemical Processing Unit** now works in batches: the hopper doses 16 uranium powder and 16 Processing Reagent,
  8000 mB of water is pumped in, the agitator turns it into slurry, and the reactor drains into the slurry tank.
  The model's tank windows show the batch turning from water into slurry.
- **Filtration Unit**: the pressure release and the filtering are one step now; the solution starts to come out as
  soon as the pressure is released.
- New GUIs for the Chemical Processing Unit and the Filtration Unit in the mod's finish (graphite brushed steel with
  IE's own slots, tanks and markers), as animated flow diagrams: the leaching reactor with its hopper and agitator,
  and the piston pump, pressure vessel with its dial, and filter press with closing plates.
- All machine tanks hold 12000 mB.
- New item icons (16×16).
- All registry names, textures, lang keys and code identifiers are now in English, for example `processing_reagent`, `uranium_slurry` and `impure_agglomerate`.
  - Items and blocks from 0.1.0 worlds are remapped to the new names automatically.
  - Fluids stored in tanks or buckets under the old names are lost.
- `mcmod.info` now has an English description, authors and credits.
- The tank textures are the mod's own now.

### Removed
- The Filtration Unit's separate pressurizing sound (the shared pump sound replaces it).

## [0.1.0] - 2026-10-21

First development snapshot published in this repository. Earlier alpha builds were internal test builds.

### Added
- **Chemical Processing Unit** (3×6×3 multiblock):
  - turns uranium powder, Processing Reagent and water into Uranium Slurry;
  - IE-style GUI with status lights and redstone control;
  - JEI category, looping sound and mirrored placement.
- **Filtration Unit** (3×3×9 multiblock filter press):
  - turns Uranium Slurry into Crude Uranium Solution and Wet Mineral Residue;
  - batch cycle: pressurize, close, release, filter, open;
  - animated actuators and plates, and a discharge conveyor for the residue;
  - 28 filter slots. Filters wear out and change colour with use, and filter input and output ports allow automation.
  - GUI and JEI category.
- Items:
  - Processing Reagent and Filter, both craftable;
  - Wet Mineral Residue;
  - Quicklime, Neutralization Cake, Precipitating Agent, Wet Uranium Concentrate and Yellowcake.
- Fluids, animated, with buckets: Uranium Slurry, Crude Uranium Solution, Extraction Solvent, Stripping Solution, Purified Uranium Solution, Chemical Effluent, Neutralized Sludge, Depleted Effluent and Process Condensate.
- Impure Agglomerate block, crafted from 4 Wet Mineral Residue or 4 Neutralization Cake.
- Hazard tooltips on materials: radioactive, corrosive, flammable, toxic, oxidizer and caustic.
- English and Brazilian Portuguese translations.
- Structure tests for both multiblocks, run with `gradlew build`.

[Unreleased]: https://github.com/FlipThunder2607/Immersive-Nuclear/compare/v0.1.6...HEAD
[0.1.6]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.6
[0.1.5]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.5
[0.1.4]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.4
[0.1.3]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.3
[0.1.2]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.2
[0.1.1]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.1
[0.1.0]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.0
