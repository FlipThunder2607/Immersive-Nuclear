# Contributing to Immersive Nuclear

Thanks for your interest in the project! Issues and pull requests can be written in English or Portuguese.

## Reporting bugs

Open an issue using the **Bug report** template and include:

- Immersive Nuclear, Immersive Engineering and Forge versions
- what you did, what you expected and what happened
- the crash report or `latest.log` (as a file or a [gist](https://gist.github.com/)) if the game crashed
- a screenshot if it is a visual problem

## Suggesting features

Use the **Feature request** template. Ideas that fit the fuel cycle and the Immersive style are the most likely to be accepted. Check the [roadmap](README.md#roadmap) first.

## Development setup

- Install a **JDK 8**. ForgeGradle 2 does not run on newer JDKs.
- Clone the repository and run `./gradlew build` once (`gradlew.bat build` on Windows).
- For a development client: `./gradlew setupDecompWorkspace`, then `./gradlew runClient`.
- For IDE files: `./gradlew idea` or `./gradlew eclipse`.

## Branches and pull requests

- `1.12.2` is the main branch. Keep it buildable.
- Create a branch per change: `feature/<name>`, `fix/<name>` or `docs/<name>`.
- Open a pull request against `1.12.2` and fill in the template.
- Before opening it, run `./gradlew build`. It must pass, including the structure tests.
- Keep pull requests focused: one machine, fix or feature per PR.

### Commit messages

Use the imperative mood, with a short first line (about 72 characters at most):

```
Add stripping stage to the Chemical Separation Unit
Fix filter wear not saving on world reload
```

## Code style

- Java 8. Indent with **tabs**. Braces go on their own line, following the existing code.
- Code is organised by side and role: `common/block`, `common/multiblock`, `common/gui`, `common/item`, `client`, `client/jei`.
- Every new player-facing string goes into **both** `en_us.lang` and `pt_br.lang`.
- Registry names, file names, identifiers and comments are in English, with registry and file names in `snake_case`.
- **Do not rename registry names** of existing blocks, items or fluids. That breaks existing worlds. If a rename is really needed, add the old name to the remap table in `INContent`.

## Design conventions

- **Ports:** input ports are **blue**, output ports are **orange**.
- **Multiblocks** are formed with the Engineer's Hammer on a redstone engineering block, like IE machines.
  - Every multiblock has a structure test in `src/test`.
  - Changing a structure means updating its test.
- **Textures:**
  - item textures are 16×16;
  - fluid textures follow IE: `still` 16×16 and `flow` 32×32, both 32 frames;
  - block textures are square, with sides that are powers of two;
  - reuse original IE textures at runtime instead of copying them into this mod.
- **Hazards:** nothing is described as a gameplay effect (tooltip or docs) until it actually exists in the game.
- **Third-party assets:** only with a compatible license, credited in `README.md` and `mcmod.info`.

## License

By contributing, you agree that your contributions are licensed under the [GPL-3.0](LICENSE), the same license as the project.
