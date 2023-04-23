# ode4j experimental version on GWT libGDX

https://user-images.githubusercontent.com/10563814/233501942-d7043084-1781-4413-bcf3-16b595506bb9.mp4

This repository hosts an experimental version of [Open Dynamics Engine for Java](https://github.com/tzaeschke/ode4j) (ode4j v0.4.1) 3D physics library working on libGDX's GWT backend.  Use at your own risk.  Because a lot of the original ode4j code was modified to compile and run properly on libGDX's GWT backend, I cannot guarantee everything is working properly.  More importantly keeping up to date with ode4j updates will be difficult.

If you want to use ode4j only on libGDX Desktop/Android/iOS backends then I recommend you use [odej4](https://github.com/tzaeschke/ode4j) directly.  However if you want cross platform support (i.e include GWT support) then you could use this library for all platforms.

I created this repository to play with a 3D physics engine that works on libGDX's GWT backend for game JAMS and other fun stuff.  I do not recommend to use in important projects.

### What I changed from ode4j's codebase

Here is a brief summary of what I had to change to get ode4j to work on libGDX's backend:

![image](https://user-images.githubusercontent.com/10563814/233494464-bbd9f043-2cb9-47a6-955c-a2a539652491.png)

I also removed most of the cpp (C++) packages and classes.

## Where to get ODE/ode4j documentation and help

ODE official manual: http://ode.org/wiki/index.php/Manual

ode4j discord channel : https://discord.gg/UFXJcXv2P8 ode4j/Java

ode4j contains also some features that are not present in ODE, such as a ragdoll and heightfields with holes. See ode4j's [Wiki](https://github.com/tzaeschke/ode4j/wiki/Functionality-beyond-ODE).

The [ODE forum](https://groups.google.com/forum/#!forum/ode-users) is useful for questions around physics and general API usage.

The [ode4j forum](https://groups.google.com/forum/?hl=en#!forum/ode4j) is for problems and functionality specific to ode4j/Java. 

There is also the [old website](https://tzaeschke.github.io/ode4j-old/), including some [screenshots](https://tzaeschke.github.io/ode4j-old/ode4j-features.html).

Here is a [Youtube video](https://www.youtube.com/watch?v=ENlpu_Jjp3Q) of a list of games that used ODE from 2002-2015.  You will be suprised how many of your favorite games used this physcis libary.

## What you don't get

This is a 3D physics library only.  You will have to implement your own draw calls.  ode4j demos use an unoptimized custom drawing helper classes based on LWJGL.  Even ode4j's documentation says that thier render implementation has poor performance and is not optimized.  Regardless, there was no point migrating the drawing helper classes over because we use libGDX.

Becasue I did not migrate the draw helper classes, every demo from ode4j will not work.  

## Some headaches

Ode4j has its own math classes similar to libGDX's Vector3, Matrix3, and Quaternion.  You will have to get to know them and learn how to convert ode4j's versions to libGDX's versions in your render loop.  Be very careful as the signatures for Quanternion's yaw, pitch roll and x,y,x,w are reversed between ode4j and libGDX which added wasted hours of fustration getting demos to work.

In addition ode4j uses double and not float like most of libGDX's math classes.

## Demos

The built in demo is a modified version of the [DemoCrash](https://github.com/tzaeschke/ode4j/blob/master/demo/src/main/java/org/ode4j/demo/DemoCrash.java) in the ode4j [demo package](https://github.com/tzaeschke/ode4j/tree/master/demo/src/main/java/org/ode4j/demo).  This demo is shown on the video at the top of this page.

It currently creates 75 boxes and I get 60 frame per second (fps) on a Chrome based browser.  You can increase the size of the wall, resulting in more boxes by modifying these two constants:

```java
    private static final float WALLWIDTH = 12;		// width of wall
    private static final float WALLHEIGHT = 10;		// height of wall
```

For me if when I generate over 200 boxes my fps goes down significantly.  To experiment for yourself, modify the [DemoCrashScreen](https://github.com/antzGames/ode4j-GWT-Compatible-libGDX/blob/master/core/src/main/java/com/antz/ode4libGDX/screens/DemoCrashScreen.java).

The demo was tested on GWT, Desktop and Android.

I have converted the Ragdoll test and it looks like its working.

https://user-images.githubusercontent.com/10563814/233760215-26e2ef8f-5c0f-4a38-a489-596630e0eeaf.mp4

## Repo structure

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

My modified ode4j v0.4.1 [source code](https://github.com/antzGames/ode4j-GWT-Compatible-libGDX/tree/master/core/src/main/java/org/ode4j) is included in this repo.  Currently no work has been done to make and publish this as a library.  Still too experiemental for that.  Once I have used this in a few game JAMS, I might make a library out of it.

### Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
- `android`: Android mobile platform. Needs Android SDK.
- `html`: Web platform using GWT and WebGL. Supports only Java projects.

This project uses [Gradle](http://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `android:lint`: performs Android project validation.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `html:dist`: compiles GWT sources. The compiled application can be found at `html/build/dist`: you can use any HTTP server to deploy it.
- `html:superDev`: compiles GWT sources and runs the application in SuperDev mode. It will be available at [localhost:8080/html](http://localhost:8080/html). Use only during development.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

## ODE, ode4j and other Licenses

### Licensing & Copyright for ODE and ode4j

This library is under copyright by Tilmann Zäschke (Java port), Russell L. Smith (copyright holder of the original ODE code), Francisco Leon (copyright holder of the original GIMPACT code) and Daniel Fiser (copyright holder of the original libccd).

This library is free software; you can redistribute it and/or modify it under the terms of EITHER:
(1) The GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. The text of the GNU Lesser General Public License is included with this library in the file LICENSE.TXT.
(2) The BSD-style license that is included with this library in the files ODE-LICENSE-BSD.TXT and ODE4j-LICENSE-BSD.TXT.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files LICENSE.TXT, ODE-LICENSE-BSD.TXT, GIMPACT-LICENSE-BSD.TXT, GIMPACT-LICENSE-LGPL.TXT, ODE4J-LICENSE-BSD.TXT and LIBCCD_BSD-LICENSE for more details.

The LICENSE.TXT, ODE-LICENSE-BSD.TXT, GIMPACT-LICENSE-BSD.TXT, GIMPACT-LICENSE-LGPL.TXT, LIBCCD_BSD-LICENSE and ODE4J-LICENSE-BSD.TXT files are available in the source code.
