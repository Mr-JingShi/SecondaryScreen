package com.secondaryscreen.app;

import java.util.Objects;

public class Resolution {
    static public Resolution R;
    private final String NAME;
    public final int VIRTUALDISPLAY_WIDTH;
    public final int VIRTUALDISPLAY_HEIGHT;
    public final int VIRTUALDISPLAY_DENSITYDPI;
    public int TEXTUREVIEW_WIDTH;
    public int TEXTUREVIEW_HEIGHT;
    public float SCALE_X;
    public float SCALE_Y;

    public Resolution(String name, int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensityDpi) {
        this.NAME = name;
        this.VIRTUALDISPLAY_WIDTH = virtualDisplayWidth;
        this.VIRTUALDISPLAY_HEIGHT = virtualDisplayHeight;
        this.VIRTUALDISPLAY_DENSITYDPI = virutalDisplayDensityDpi;
        this.TEXTUREVIEW_WIDTH = virtualDisplayWidth;
        this.TEXTUREVIEW_HEIGHT = virtualDisplayHeight;
        this.SCALE_X = 1.0f;
        this.SCALE_Y = 1.0f;
    }

    public Resolution(String name, int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensityDpi, int textureViewWidth, int textureViewHeight) {
        this.NAME = name;
        this.VIRTUALDISPLAY_WIDTH = virtualDisplayWidth;
        this.VIRTUALDISPLAY_HEIGHT = virtualDisplayHeight;
        this.VIRTUALDISPLAY_DENSITYDPI = virutalDisplayDensityDpi;
        this.TEXTUREVIEW_WIDTH = textureViewWidth;
        this.TEXTUREVIEW_HEIGHT = textureViewHeight;
        this.SCALE_X = (float) textureViewWidth / virtualDisplayWidth;
        this.SCALE_Y = (float) textureViewHeight / virtualDisplayHeight;
    }

    public void changeScale(float scaleX, float scaleY) {
        this.SCALE_X = scaleX;
        this.TEXTUREVIEW_WIDTH *= scaleX;
        this.SCALE_Y = scaleY;
        this.TEXTUREVIEW_HEIGHT *= scaleY;
    }

    public boolean match(int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensityDpi, int textureViewWidth, int textureViewHeight) {
        return VIRTUALDISPLAY_WIDTH == virtualDisplayWidth
                && VIRTUALDISPLAY_HEIGHT == virtualDisplayHeight
                && VIRTUALDISPLAY_DENSITYDPI == virutalDisplayDensityDpi
                && TEXTUREVIEW_WIDTH == textureViewWidth
                && TEXTUREVIEW_HEIGHT == textureViewHeight;
    }

    public boolean match(Resolution other) {
        return VIRTUALDISPLAY_WIDTH == other.VIRTUALDISPLAY_WIDTH
                && VIRTUALDISPLAY_HEIGHT == other.VIRTUALDISPLAY_HEIGHT
                && VIRTUALDISPLAY_DENSITYDPI == other.VIRTUALDISPLAY_DENSITYDPI
                && TEXTUREVIEW_WIDTH == other.TEXTUREVIEW_WIDTH
                && TEXTUREVIEW_HEIGHT == other.TEXTUREVIEW_HEIGHT;
    }

    @Override
    public int hashCode() {
        return Objects.hash(NAME,
                VIRTUALDISPLAY_WIDTH,
                VIRTUALDISPLAY_HEIGHT,
                VIRTUALDISPLAY_DENSITYDPI,
                TEXTUREVIEW_WIDTH,
                TEXTUREVIEW_HEIGHT);
    }

    @Override
    public String toString() {
        return "Resolution{NAME="
            + NAME
            + ", VIRTUALDISPLAY_WIDTH="
            + VIRTUALDISPLAY_WIDTH
            + ", VIRTUALDISPLAY_HEIGHT="
            + VIRTUALDISPLAY_HEIGHT
            + ", VIRTUALDISPLAY_DENSITYDPI="
            + VIRTUALDISPLAY_DENSITYDPI
            + ", TEXTUREVIEW_WIDTH="
            + TEXTUREVIEW_WIDTH
            + ", TEXTUREVIEW_HEIGHT="
            + TEXTUREVIEW_HEIGHT
            + ", SCALE_X="
            + SCALE_X
            + ", SCALE_Y="
            + SCALE_Y
            + '}';
    }

    public String toSimpleString() {
        return NAME
            + "("
            + VIRTUALDISPLAY_WIDTH
            + "x"
            + VIRTUALDISPLAY_HEIGHT
            + "/"
            + VIRTUALDISPLAY_DENSITYDPI
            + ")";
    }
}