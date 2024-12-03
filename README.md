# Carpet Mod
Yes.

## Note
- this version (0.1.0) was based on Carpet-Vastech-Addition 0.1.5.12

## Additions from [Voided server](https://discord.com/invite/P8naD2wBc7)

### Shetro

- Chatbridge should have been added, but I'm too lazy to deal with the original author's license.
- [Syncmatica](https://github.com/RealShetro/syncmatica-1.12) protocol support

### Bjoel2

- fix bug where worldedit does block post processing on the wrong coords
- fix console spam `Wrong logger name` liquidPocket error
- add memory logger `/log memory`

### Carpet Rules
#### viewVillagerInventory
- Description: `Right click on villager to view it's inventory (use this only for debug on cmp)`
- Type: `Boolean`
- Default: `false`
- Suggestions: `true`, `false`
- Categories: `CREATIVE`, `VOIDED`
- By: `Bjoel2`
#### chatTweaks
- Description: `Adds chat tweaks such as !seed, !cords, !ncords, !ocords`
- Type: `Boolean`
- Default: `false`
- Suggestions: `true`, `false`
- Categories: `SURVIVAL`, `VOIDED`
- By: `Bjoel2`
#### customWhitelistMessage
- Description: `Sets custom whitelist disconnect message defined in whitelist_message.txt file`
- Type: `Boolean`
- Default: `false`
- Suggestions: `true`, `false`
- Categories: `FEATURE`, `VOIDED`
- By: `Bjoel2`

### Also join [our Discord server](https://discord.com/invite/P8naD2wBc7) :)

## Getting Started
### Setting up your sources
- Clone this repository.
- Run `gradlew setupCarpetmod` in the root project directory.

### Using an IDE
- To use Eclipse, run `gradlew eclipse`, then import the project in Eclipse.
- To use Intellij, run `gradlew idea`, then import the project in Intellij.

## Using the build system
Edit the files in the `src` folder, like you would for a normal project. The only special things you have to do are as follows:
### To generate patch files so they show up in version control
Use `gradlew genPatches`
### To apply patches after pulling
Use `gradlew setupCarpetmod`. It WILL overwrite your local changes to src, so be careful.
### To create a release / patch files
In case you made changes to the local copy of the code in `src`, run `genPatches` to update the project according to your src.
Use `gradlew createRelease`. The release will be a ZIP file containing all modified classes, obfuscated, in the `build/distributions` folder.
### To run the server locally (Windows)
Use `mktest.cmd` to run the modified server with generated patches as a localhost server. It requires `gradlew createRelease` to finish successfully as well as using default paths for your minecraft installation folder.

In case you use different paths, you might need to modify the build script.
This will leave a ready server jar file in your saves folder.

It requires to have 7za installed in your paths
