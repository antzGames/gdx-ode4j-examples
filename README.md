# ode4j experimental version on GWT libGDX

This repository hosts an experimental version of Open Dynamics Engine for Java (ode4j) 3D pyshics libary working on libGDX's GWT backend.  Use at your own risk.  Because a lot of original ode4j code was modified to compile and run properly on libGDX's GWT backend, I cannot guarantee everything is working properly.  More importantly keeping up to date with ode4j updates will be difficult.

If you want to use ode4j on libGDX Desktop/Android/iOS backends then use the [odej4](https://github.com/tzaeschke/ode4j) directly.

I created this repository is to play with a 3D physics engine that works on libGDX's GWT backend for game JAMS and other fun stuff.  I do not recommend to use in important projects.

### What I changed in ode4j

Here is a brief summary of what I had to change to get ode4j to work on libGDX's backend:

![image](https://user-images.githubusercontent.com/10563814/233494464-bbd9f043-2cb9-47a6-955c-a2a539652491.png)

I also removed most of the cpp (C++) packages and classes.

