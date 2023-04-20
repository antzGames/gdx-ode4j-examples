# ode4j experimental version on GWT libGDX

This repository hosts an experimental version of Open Dynamics Engine for Java (ode4j) 3D pyshics libary working on libGDX's GWT backend.  Use at your own risk.  Because a lot of original ode4j code was modified to compile and run properly on libGDX's GWT backend, I cannot guarantee everything is working properly.  More importantly keeping up to date with ode4j updates will be difficult.

If you want to use ode4j only on libGDX Desktop/Android/iOS backends then I recommend you use the [odej4](https://github.com/tzaeschke/ode4j) directly.  However if you want cross platform support (i.e include GWT support) then you could use this library for all platforms.

I created this repository to play with a 3D physics engine that works on libGDX's GWT backend for game JAMS and other fun stuff.  I do not recommend to use in important projects.

### What I changed from ode4j's codebase

Here is a brief summary of what I had to change to get ode4j to work on libGDX's backend:

![image](https://user-images.githubusercontent.com/10563814/233494464-bbd9f043-2cb9-47a6-955c-a2a539652491.png)

I also removed most of the cpp (C++) packages and classes.

## Where to get ODE documentation and help

ODE official manual: http://ode.org/wiki/index.php/Manual

ode4j discord channel : https://discord.gg/UFXJcXv2P8 ode4j/Java

ode4j contains also some features that are not present in ODE, such as a ragdoll and heightfields with holes. See pde4j's [Wiki](https://github.com/tzaeschke/ode4j/wiki/Functionality-beyond-ODE).

The [ODE forum](https://groups.google.com/forum/#!forum/ode-users) is useful for questions around physics and general API usage: 

The [ode4j forum](https://groups.google.com/forum/?hl=en#!forum/ode4j) is for problems and functionality specific to ode4j/Java. 

There is also the [old website](https://tzaeschke.github.io/ode4j-old/), including some [screenshots](https://tzaeschke.github.io/ode4j-old/ode4j-features.html).

## What you don't get

So this is a 3D physics library only.  You will have to implement your own draw calls.  ode4j demos use an unoptimized custom drawing helper classes based on LWJGL.  Even ode4j's documentation says that thier render implementation has poor performance and is not optimized.  Regardless, there was no point migrating the drawing helper classes over becasue we use libGDX.

## Demo

Currently I have 1 working demo which is based on the [DemoCrash](https://github.com/tzaeschke/ode4j/blob/master/demo/src/main/java/org/ode4j/demo/DemoCrash.java) in ode4j demo package.

## Repo structure

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

My modified ode4j [source code](https://github.com/antzGames/ode4j-GTW-Compatible-libGDX/tree/master/core/src/main/java/org/ode4j) is included in this repo.  Currently no work has been done to make and publish this as a library.  Still to experiemental for that.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
- `android`: Android mobile platform. Needs Android SDK.
- `html`: Web platform using GWT and WebGL. Supports only Java projects.
