# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow `MAJOR.MINOR.PATCH-alpha.N` while the mod is in alpha.

## [Unreleased]

### Planned
- Chemical Separation Unit (solvent extraction: extraction and stripping stages)
- IE Mixer recipes for Extraction Solvent, Stripping Solution and Neutralized Sludge
- Quicklime recipe and effluent recycling into the Chemical Processing Unit

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

[Unreleased]: https://github.com/FlipThunder2607/Immersive-Nuclear/compare/v0.1.0-alpha.17...HEAD
[0.1.0-alpha.17]: https://github.com/FlipThunder2607/Immersive-Nuclear/releases/tag/v0.1.0-alpha.17
