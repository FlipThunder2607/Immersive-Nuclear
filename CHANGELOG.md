# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow `MAJOR.MINOR.PATCH-alpha` while the mod is in alpha: each finished feature is one patch release.

## [Unreleased]

### Planned
- **Metal Extractor** (machine 3.5), a branch after the Chemical Separation Unit. An electrolytic cell with hanging
  plates recovers dissolved metals from the Chemical Effluent in batches (fill, prepare, recover, drain, collect):
  the plates are lifted out and scraped, the Mixed Metal Deposit falls down a chute, and Depleted Effluent goes on to
  treatment. The deposit is processed in an IE machine into metal nuggets.
- Filtering the Neutralized Sludge into Neutralization Cake and water, and effluent recycling into the Chemical Processing Unit

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

[Unreleased]: https://github.com/FlipThunder2607/Immersive-Nuclear/compare/v0.1.2-alpha...HEAD
[0.1.2-alpha]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.2-alpha
[0.1.1-alpha]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.1-alpha
[0.1.0-alpha.17]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.0-alpha.17
