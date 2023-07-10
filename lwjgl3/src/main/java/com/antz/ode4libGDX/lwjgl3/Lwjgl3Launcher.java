package com.antz.ode4libGDX.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.antz.ode4libGDX.Ode4libGDX;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Ode4libGDX(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("ODE for libGDX Demos");
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
        configuration.useVsync(true);
        //configuration.setMaximized(true);
        //// Limits FPS to the refresh rate of the currently active monitor.
        //configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setBackBufferConfig(8,8,8,8,24,0,8);
        configuration.setWindowedMode(1920, 1080);
        return configuration;
    }
}
