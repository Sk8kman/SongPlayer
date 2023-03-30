# SongPlayer
A Fabric mod for Minecraft that plays songs with noteblocks.
The current version is for Minecraft 1.19.x.

# How to install
You can grab the mod jar from releases section.
This mod requires fabric api.

# Adding songs
You can put midis or NoteBlockStudio files in the `.minecraft/songs` folder.
SongPlayer supports any valid midi and all versions of NBS files.

# Using the client
To get started, add some midis or nbs files to your songs folder, and use `$play <filename>` in an open area.
If you have provided it a valid midi or nbs file, it will either:
try to set your gamemode to creative, place the required noteblocks for the song, try to switch you to survival, then start playing. OR:
try to play the song using only commands such as /playsound depending on your settings.

# Commands
All the commands are not case-insensitive.

### $help
### OR: $help \<command>
Lists all SongPlayer commands or explains a command and shows the command usage.

### $play \<filename or url>
Plays a particular midi from the .minecraft/songs folder, or, if a url is specified, downloads the song at that url and tries to play it.

If there is a song already playing, the new song will be added to the queue.

### $playlist \<create, delete, play> <playlist name>
### OR: $playlist \<sort> <addedfirst, alphabetically, shuffle>
### OR: $playlist \<edit> \<playlist name> \<add, remove> \<song>
Manage or play a list of songs

### $setPlayMode \<client, commands, gamemode>
*$aliases: `$playMode`, `$pMode`, `$updatePlayMode`*

Change what method to use when playing songs.
- client: plays your songs client-side, so no one else can hear them.
- gamemode: switches to creative to automatically build & repair noteblocks, then back to survival to play them.
- commands: Use commands to play songs instead of noteblocks

### $setStage <default, legacy, compact>
*$aliases: `$stage`, `$updateStage`*

Changes the shape of the stage when automatically building noteblocks.
- default: a 11x10x11 stage. Contains 353 noteblocks in total.
- compact: a 11x11x11 stage. Contains all 400 possible noteblocks, and is more of a circular shape. Huge thanks to Lizard16 for making this design!
- legacy: a 9x8x9 stage. Contains 300 noteblocks in total. (This is the stage that the original SongPlayer mod uses)

### $prefix <prefix>
Sets the command prefix used to execute these commands.
Example: `$prefix !` will change commands to `!example command` instead of `$example command`

### $toggle <swing, rotate, allMovements> <true, false>
Toggles certain features I added as ideas when forking this. All are toggled to false by default.
- allMovements: toggles all movement-related features at once such as swing and rotate
- Rotate: weather you rotate to the noteblock you are placing / playing
- Swing: weather you swing your hand when you play a noteblock

### $stop
Stops playing/building and clears the queue or playlist.

### $skip
Skips the current song and goes to the next one in the queue or playlist.

### $goto \<mm:ss>
Goes to a specific time in the song.

### $loop
Toggles song looping.

### $status
*aliases: `$current`*

Gets the status of the current song that is playing.

### $queue
*aliases: `$showQueue`*

Shows all the songs in the queue or playlist.

### $songs
*aliases: `$list`, `$listSongs`*

Lists the songs in your .minecraft/songs folder.

### $setCommand <creative, survival, playnote, displayprogress> \<command>
*aliases: `$setCmd`*

Changes what command is used for certain circumstances.

### $setCreativeCommand \<command>
*aliases: `$sc`, `$creativeCommand`*

Shortcut for `$setCommand creative`
For example, if the server uses vanilla commands, do `$setCreativeCommand /gamemode creative`.

### $setSurvivalCommand \<command>
*aliases: `$ss`, `$survivalCommand`*

Shortcut for `$setCommand survival`
For example, if the server uses vanilla commands, do `$setSurvivalCommand /gamemode survival`.

### $useEssentialsCommands
*aliases: `$essentials`, `$useEssentials`, `$essentialsCommands`*

Switch to using Essentials gamemode commands.

Equivalent to `$setCommand creative /gmc` and `$setCommand survival /gms`

### $useVanillaCommands
*$aliases: `$vanilla`, `$useVanilla`, `$vanillaCommands`*

Switch to using vanilla gamemode commands.

Equivalent to `$setCommand creative /gamemode creative` and `$setCommand survival /gamemode survival`

### $toggleFakePlayer
*aliases: `$fakePlayer`, `$fp`*

Toggles whether a fake player will show up to represent your true position while playing a song. When playing a song, since it automatically enables freecam, your true position will be different from your apparent position. The fake player will show where you actually are. By default, this is disabled.

### $testSong
A command I used for testing during development.
It plays every single noteblock sound possible in order.
However, there are 400 possible noteblocks but the max stage size is 300, so that last 100 notes aren't played.

# Mechanism
--- using noteblocks ---
SongPlayer places noteblocks with nbt and instrument data already in them, so the noteblocks do not need to be individually tuned. Ayunami2000 has previously done a proof-of-concept of this method.

My client will automatically detect what noteblocks are needed and place them automatically before each song is played, which makes playing songs quite easy. The only drawback is that you need to be able to switch between creative and survival mode, which my client will attempt to do automatically.

When playing a song, freecam is enabled. You will be able to move around freely, but in reality you are only moving your camera while your player stays at the center of the noteblocks. This is because noteblocks can only be played if you're within reach distance of them, so you have to stand at the center of the noteblocks to play them, but it's still nice to be able to move around while your song is playing.

--- using commands ---
SongPlayer will use commands such as /playsound and display progress for everyone with /title by default.

This will only work if you have /op on a minecraft server, and some servers may kick you for spamming.

Use this mode with caution.

--- using client ---
SongPlayer will read the song file and play it on your game. No one else can hear the songs besides you. However, you do not need any permissions on the server to use this.

This is nice if you want to test out song files before playing them for everyone else, or if you just want to enjoy your songs without any interruptions of other players.

# --- CHANGELOGS ---
### 3.1.3
```
- Fixed an issue where you didn't teleport properly and could move around
```

### 3.1.2
```
- Fixed an issue where you wouldn't teleport to the center of the stage server-side when you start a song, causing some noteblocks to not be placed
- Changed the default command of playnote - replaced "player" with "record" (whoops)
- Minor changes to chat messages & typo fixes
- Added more command aliases
```

### 3.1.1
```
- Fixed an issue where the mod would switch to creative when it did not have to
- Fixed an issue where the mod would break more blocks than it should when building the stage
- 1.19.4 support
```

### 3.1.0
```
- added new commands:
  - $setStage
  - $setPlayMode (replaces $toggle useCommandsToPlay)
- Added new stage layouts when switching gamemode to play:
    - default (11x10x11 stage, fits 353 noteblocks)
    - compact (11x11x11 stage, fits 400 noteblocks)
    - legacy (9x8x9 stage, fits 300 noteblocks - Not new, used in older versions of SongPlayer)
- Added support for playing songs client-side
- Command prefix can no longer be manually changed to begin with a / character
- Fixed a bug where powering noteblocks with redstone could cause infinite building loops, or have the note's pitch off by 1
- Fixed a crash when interacting with an entity while playing
- Fixed a bug where players could change noteblock types without the mod fixing it when using gamemode method of playing noteblocks
- Fixed an issue where the mod sometimes wouldn't detect blocks above noteblocks, and won't fix them, causing the noteblocks to not be played
- You no longer show sprinting particles from other players while playing
```

### 3.0.0
```
- added new commands:
  - $prefix
  - $toggle
  - $playlist
- SongPlayer will now attempt to restore what item you were holding before you started building, after it's finished building.
- Added support to change your command prefix
- Added support to rotate and swing your hand to the target noteblock while playing
- Added playlist support
- Added a way to use commands to play instead of noteblocks that can be played for the entire server. ($toggle useCommandsToPlay true, REQUIRES /OP)
- Added support for 1.19.3
- Added config file support, so the mod will remember your settings.
- You no longer teleport back to the stage when there's more songs in queue.
```
