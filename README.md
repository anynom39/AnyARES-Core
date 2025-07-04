# AnyARES (Advanced Regional Editing Suite)

![alt text](https://placehold.co/800x200/2c2c2c/e0e0e0/png?text=AnyARES)


AnyARES is a modern, high-performance, asynchronous world editing suite for PaperMC servers, built from the ground up for performance and extensibility.

Inspired by giants like WorldEdit and FAWE, AnyARES aims to provide server administrators and advanced builders with a powerful, intuitive, and lag-free toolset for large-scale region manipulation. Its core philosophy is based on a lean, powerful core plugin and a rich ecosystem of optional addon plugins, allowing server owners to install only the features they need.

# Server Requirements

### Server Software

**Paper** (or a downstream fork like Purpur). Spigot is **not** supported due to the plugin's reliance on Paper's advanced asynchronous APIs for performance.

### Minecraft Version

**1.21.x**. The plugin is built against the 1.21 API and will not work on older versions like 1.20.x. It is always recommended to use the latest version of Paper for the corresponding Minecraft release.

### Java Version

**Java 21**. This is a requirement for Minecraft 1.21+ itself, so if your server is running, you already meet this requirement.

# ✨ Core Features

The AnyARES-Core plugin provides the essential foundation for all world editing tasks.

Asynchronous by Design: All block operations are processed through a robust, asynchronous task engine, minimizing impact on server performance (TPS).

Multi-Shape Selection System: Natively supports a variety of selection shapes beyond simple cuboids.

Cuboid: The classic box selection.

Sphere: Perfect spherical selections defined by a center and radius.

Cylinder: Vertical or arbitrarily oriented cylinders.

2D Polygon (Extruded): Define custom flat shapes and extrude them vertically to create complex prisms.

Powerful Pattern Parser: Go beyond single blocks. Use complex patterns like 50%stone,30%dirt,20%gravel and specify block states like oak_log[axis=y].

Full History System: Unlimited undo (/undo) and redo (/redo) for every operation, stored per-player.

Player Clipboard: A powerful /copy, /cut, and /paste system that works with all selection shapes.

Extensible API: A clean, well-defined API (AnyAresAPI) designed for developers to easily create and integrate their own addons.

Live Visualizations: See your selection in real-time with a lightweight, particle-based visualizer that outlines your selected shape.

# 🔌 Addon Ecosystem

AnyARES is designed to be modular. The core plugin is powerful on its own, but its true potential is unlocked through addons.

AnyARES-Selections (Included): The official addon providing advanced selection manipulation commands.

# 🚀 Planned Features & Roadmap

The journey for AnyARES has just begun! Here is a look at what's planned for the future, both for the Core and for new addons.
## Core Enhancements

Performance Optimization Pass: Continuous profiling and optimization of block iterators, the task engine, and memory usage.

Brush System API: A core API for binding operations to items, allowing for the creation of powerful painting and sculpting tools in addons.

Masking System API: An API for source and destination masks, allowing operations to only affect specific blocks (e.g., //replace stone dirt -m !air).

Transformation API: A framework for applying geometric transforms (like rotate and flip) to the player clipboard.

## Future Addons

### AnyARES-Clipboard:

Full .schem schematic file support (/schem load, /schem save, /schem list).

Support for sharing clipboards between players.

### AnyARES-Operations:

Common WorldEdit commands like //walls, //outline, //overlay.

Advanced operations like //smooth and //regen.

### AnyARES-BrushesAddon:

A suite of standard brushes: sphere brush, smooth brush, paste brush, etc.

### AnyARES-Tools:

"Magic Wand" / Contiguous block selection tools.

Information tools (e.g., /tool info to get block data).

### AnyARES-Scripting:

Integration with a scripting engine to allow users to write their own complex operations.

# 🛠️ For Developers

AnyARES is built with developers in mind. To create your own addon:

1. Add AnyARES-Core as a provided dependency in your pom.xml.

2. Add depend: [AnyARES-Core] to your addon's plugin.yml.

3. Use the com.anynom39.anyares.api.AnyAresAPI class to safely interact with all core managers and functionality.

# ❓ Why Another World Editor?

The goal of AnyARES is not just to clone existing tools, but to re-imagine them with a modern architecture focused on:

Performance: Leveraging PaperMC's asynchronous capabilities to their fullest.

Modularity: Providing a stable core and letting server owners choose which feature sets they want via addons.

Extensibility: Making it as easy as possible for other developers to build upon the platform.

We believe this approach will lead to a more stable, efficient, and versatile tool for the modern Minecraft server.
