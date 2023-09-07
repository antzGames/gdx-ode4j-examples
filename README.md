![image](https://github.com/antzGames/gdx-ode4j-examples/assets/10563814/8327cad7-de09-43b5-b9ae-01260ed5cbf2)
# gdx-ode4j Examples

Some examples and demos using my [gdx-ode4j](https://github.com/antzGames/gdx-ode4j) library.

`gdx-ode4j` library is compatible with all libGDX backends, including GWT.

You can test these demos on your browser on [itch.io](https://antzgames.itch.io/physics-in-a-browser).

`gdx-ode4j` is based on version 0.4.2 of [Open Dynamics Engine for Java](https://github.com/tzaeschke/ode4j).

https://github.com/antzGames/gdx-ode4j/assets/10563814/bc46a9a9-f2e8-414b-bfde-8be6ea54e46b

If you want to use ode4j only on libGDX Desktop/Android/iOS backends then I recommend you use [odej4](https://github.com/tzaeschke/ode4j) directly.  
However if you want cross platform support (i.e include GWT support) then you need to use my [gdx-ode4j](https://github.com/antzGames/gdx-ode4j) library.

Currently this is the only 3D physics engine option for GWT on libGDX.  

Fully tested on libGDX v1.12.0 and tested using WebGL2 support for GWT.

## Demos

You can test the demos on your browser [here](https://antzgames.itch.io/physics-in-a-browser).

```F1``` key will cycle to the next demo.  On the last demo will need you to press F2 to return you to first demo.

Description of included demos:

* `Demo Collision` - the standard physics collision test with 1000 boxes.
* `Dynamic Character` - [JamesTKhan's libGDX jBullet tutorial example](https://www.youtube.com/watch?v=O0Deshj2-KU), but using odej4 library. (Picture below)
* `DemoCrash` - destroy a wall of cubes with a cannon, port from ODE4J demos.
* `DemoRagdoll` - Rigid bodies and joints that you can apply to a humanoid character, to simulate behaviour such as impact collisions
 and character death, port from ODE4J demos.
* `OdeBulletTest` - the teaVM Bullet demo converted to ODE4J. 

The demos have been tested on GWT, Desktop and Android.

![odeGif](https://user-images.githubusercontent.com/10563814/235331595-2bffb58e-b429-44d8-b7fb-d3e5d89c0bca.gif)

## Gotchas while porting ODE4J demos

FYI, the original ode4j demos have Z UP which is a pain.  

During demo migration I either rotated the camera `camera.up.set(Vector.Z)` or
reconfigured the simulation (gravity, positions, rotations) to have Y UP.  Both worked but eventually its best to implement the second option.

In addition, libGDX primitive 3D shapes are created on the Y-axis orientation, and ode4j is on the z-axis.  
You need to be careful that your physics objects and libGDX render objects have the same orientation.

## Where to get ODE/ode4j documentation and help

ODE official manual: http://ode.org/wiki/index.php/Manual

By far the most useful part is the [HOWTO](http://ode.org/wiki/index.php/HOWTO) section

ode4j discord channel : https://discord.gg/UFXJcXv2P8 ode4j/Java

ode4j contains also some features that are not present in ODE, such as a ragdoll and heightfields with holes. See ode4j's [Wiki](https://github.com/tzaeschke/ode4j/wiki/Functionality-beyond-ODE).

The [ODE forum](https://groups.google.com/forum/#!forum/ode-users) is useful for questions around physics and general API usage.

The [ode4j forum](https://groups.google.com/forum/?hl=en#!forum/ode4j) is for problems and functionality specific to ode4j/Java.

There is also the [old website](https://tzaeschke.github.io/ode4j-old/), including some [screenshots](https://tzaeschke.github.io/ode4j-old/ode4j-features.html).

Here is a [Youtube video](https://www.youtube.com/watch?v=ENlpu_Jjp3Q) of a list of games that used ODE from 2002-2015.  You will be suprised how many of your favorite games used this physcis libary.


## Repo structure

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

Use IntelliJ or Android Studio as your IDE for minimal issues building.  You may have to your tweak the gradle version/plugin.  I used Gradle Version 7.6 and Android Gradle Plugin Version 7.0.4

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

## Legal

ode4j:
Copyright (c) 2009-2017 Tilmann Zäschke ode4j@gmx.de.
All rights reserved.

Like the original ODE, ode4j is licensed under LGPL v2.1 and BSD 3-clause. Choose whichever license suits your needs. 

### ode4j contains Java ports of the following software

[ODE/OpenDE](http://www.ode.org/):
Copyright  (c) 2001,2002 Russell L. Smith
All rights reserved.

GIMPACT (part of ODE/OpenDE):
Copyright of GIMPACT (c) 2006 Francisco Leon. C.C. 80087371.
email: projectileman(AT)yahoo.com

[LIBCCD](https://github.com/danfis/libccd):
Copyright (c) 2010 Daniel Fiser <danfis(AT)danfis.cz>;
3-clause BSD License

[Turbulenz Engine](https://github.com/turbulenz/turbulenz_engine):
Copyright (c) 2009-2014 Turbulenz Limited; MIT License
