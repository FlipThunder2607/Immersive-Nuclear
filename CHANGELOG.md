# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow `MAJOR.MINOR.PATCH-alpha` while the mod is in alpha: each finished feature is one patch release.

## [Unreleased]

### Planned
- Industrial Dryer (machine 5), closing line 1.
- The mod's own computers: multiblocks built with the Control Engineering Block, a data network with player-named
  variables, operator console with modules, analogue gauge panel, digital signal board, and expansion chips that make
  the machines report every phase of their process.

## [0.1.4-alpha] - 2026-10-26

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

## [0.1.3.1-alpha] - 2026-10-25

### Added
- **Engineering blocks**, like IE's light and heavy engineering, as casings for the next machines: Precision
  Engineering (grey), Nuclear Engineering (green), Control Engineering (blue) and Radioactive Handling (yellow). With
  the CTM mod installed, a wall of them joins into one surface, with conduits running from block to block. No recipe
  yet.
- **Computer parts**, the first items of the computers (in development; no recipe and no use yet, from the creative
  tab): Electronic Module, Relay Module, Core Memory, Punched Card, Punched Paper Tape, Disk Pack, Link Cable,
  Magnetic Tape, Continuous Form Paper, and the Link Connector and Link Relay blocks (IE's LV connector model, in
  graphite and brushed steel).

## [0.1.3-alpha] - 2026-10-24

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

## [0.1.2-alpha] - 2026-10-24

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

## [0.1.1-alpha] - 2026-10-24

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
  - Items and blocks from 0.1.0-alpha.17 worlds are remapped to the new names automatically.
  - Fluids stored in tanks or buckets under the old names are lost.
- `mcmod.info` now has an English description, authors and credits.
- The tank textures are the mod's own now.

### Removed
- The Filtration Unit's separate pressurizing sound (the shared pump sound replaces it).

## [0.1.0-alpha.17] - 2026-10-21

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

[Unreleased]: https://github.com/FlipThunder2607/Immersive-Nuclear/compare/v0.1.3-alpha...HEAD
[0.1.3-alpha]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.3-alpha
[0.1.2-alpha]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.2-alpha
[0.1.1-alpha]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.1-alpha
[0.1.0-alpha.17]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.0-alpha.17
