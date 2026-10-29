# Fabric Mod Template

## Project Setup

1. Generate the repository using this template
2. Clone the repository to your local machine
3. Open the project in IntelliJ IDEA
4. Replace `rootProject.name` in `settings.gradle.kts` with the name of your mod
5. Edit the `BuildConfig` constants in `build.gradle.kts` — mod id/name, versions, and publishing ids all live there
    - The jar name is derived from `MOD_ID`
    - `MINECRAFT_VERSION_RANGE` is expanded into `fabric.mod.json` and used as the publishing version range
6. Sync the Gradle project

## Development

1. Replace the default package structure `dev.erudites.mods.template` under `src/main/java/`
2. Rename the following entrypoint classes to match your mod's identity:
    - `TemplateMod.java` in `src/main/java/dev/erudites/mods/template/`
    - `TemplateClientMod.java` in `src/main/java/dev/erudites/mods/template/client/`
3. Replace `MODID` in `TemplateMod.java` with your **modid** — it must match `fabric.mod.json` and the asset folder below
4. Rename `src/main/resources/template.mixins.json` to match your **modid** (e.g., `modid.mixins.json`) and update the "package" reference inside the file
5. Rename `src/main/resources/assets/template/` to match your **modid**
6. Update `src/main/resources/fabric.mod.json` to reflect your mod's ID, name, entrypoints, and mixin configurations

## Gradle Tasks

- `./gradlew build` - Builds the mod JAR into `build/mods/`
- `./gradlew runClient` - Runs a local client for testing the mod
- `./gradlew runServer` - Runs a local server for testing the mod

## Publishing

Publishing uses [mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin) and is configured in `build.gradle.kts`.

1. Set `GITHUB_REPOSITORY`, `MODRINTH_PROJECT_ID` and `CURSEFORGE_PROJECT_ID` in `BuildConfig` — an empty project id skips that platform
2. Add the `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` repository secrets (`GITHUB_TOKEN` is provided by Actions)
3. Update `changelog.md` and bump `MOD_VERSION`
4. Run the `build-release` workflow from the Actions tab

- `./gradlew publishMods -Pbuild.release=true` - Publishes locally using the same environment variables as the workflow
