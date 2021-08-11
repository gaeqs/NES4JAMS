# NES4JAMS

A NES editor, assembler and simulator for JAMS.

![Image NES4JAMS](https://i.postimg.cc/k4cRRTj9/imagen.png)
![Image NES4JAMS 2](https://i.postimg.cc/k4tXqBW8/imagen.png)
![Image NES4JAMS 3](https://i.postimg.cc/1tbq08G5/imagen.png)

## Introduction

JAMS4NES is a JAMS plugin that extends the editor's capabilities, allowing you to edit, assemble and simulate NES games.

## Features

- Built in Kotlin.
- Modern design thanks to the JAMS framework.
- Project-based editor and assembler.
- Support for several project configurations.
- Macros.
- Sprite editor.
- Cycle-based simulator with several tools to analyze your game.

## Dependencies

- [JAMS](https://github.com/gaeqs/JAMS)

> JAMS is not published in maven central yet.
> Use the fat jat release as a dependency to use it as a library.

## Download and installation

JAMS has been successfully built and Ubuntu 18.04/20.04 and Windows 10/11 using Intellij IDEA.

To compile the plugin inside the IDE use:
```bash
git clone https://github.com/gaeqs/NES4JAMS NES4JAMS

# Create a folder named lib/ in the project folder. 
# Add the JAMS fat jar. Name it JAMS.jar

gradle shadowJar
```

You can use the resulting .jar directly on JAMS.

You can also compile and run JAMS directly using the following gradle command:

```bash
# Shadows and runs the command
# java -jar lib/JAMS.jar 
#   -loadPlugin build/libs/NES4JAMS-XX.jar
gradle shadowAndRun
```

If you require the debugger, use the following gradle command:
```bash
gradle shadowAndRunWithDebugger
```



