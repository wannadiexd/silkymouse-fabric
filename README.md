# TapeMouse for Fabric

A Minecraft Fabric mod that allows you to farm/boat/mine while doing other things, as you can un-focus the Minecraft window. This is a direct port of dries007's TapeMouse mod to Fabric for Minecraft 1.21.8.

## Features

- Emulate any keybinding, not just mouse buttons
- Set delay (or 0 for continuous press)
- Command interface for easy control
- Works even when Minecraft is unfocused
- Reset on scroll to prevent accidents

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.8
2. Download the latest TapeMouse mod JAR from the [Releases](https://github.com/wannadiexd/tapemouse-fabric/releases) page
3. Place the JAR file in your Minecraft mods folder
4. Launch Minecraft with Fabric profile

## Usage

Use the in-game command: `/tapemouse <off|keybinding> [delay]`

- `/tapemouse` - List all available keybindings
- `/tapemouse off` - Disable the active keybinding
- `/tapemouse <keybinding>` - Enable the specified keybinding with default delay (20 ticks)
- `/tapemouse <keybinding> <delay>` - Enable with custom delay (ticks)
  - Set delay to 0 for continuous button press
  - Higher delay values create slower toggling

### Examples

```
/tapemouse forward      # Auto-walk with default delay
/tapemouse attack 0     # Continuous left-click (mining)
/tapemouse use 10       # Fast right-click toggling (farming)
/tapemouse key.jump 15  # Jump repeatedly
/tapemouse off          # Stop all automated actions
```

## ⚠️ Important Warning

**Don't use this on a server that doesn't allow AFK farming.**

This mod is detectable, and it's not designed to be hidden from server administrators. Using it on servers that prohibit AFK farming or automation tools may result in a ban. You use this mod at your own risk!

## Building from Source

```bash
git clone https://github.com/wannadiexd/tapemouse-fabric.git
cd tapemouse-fabric
./gradlew build
```

The built JAR file will be in `build/libs/`.

## How It Works

TapeMouse works by simulating key presses at the Minecraft client level. It can:
- Continuously hold down a key (delay = 0)
- Toggle a key on/off at specified intervals (delay > 0)
- Function even when the Minecraft window is not in focus

The mod uses Fabric mixins to modify Minecraft's window focus handling, allowing it to continue functioning when you're using other applications.


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---