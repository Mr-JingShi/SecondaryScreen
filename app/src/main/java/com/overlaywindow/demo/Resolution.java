package com.overlaywindow.demo;

import java.util.Objects;

public class Resolution {
    static public Resolution R;
    public final int VIRTUALDISPLAY_WIDTH;
    public final int VIRTUALDISPLAY_HEIGHT;
    public final int VIRTUALDISPLAY_DENSITYDPI;
    public int TEXTUREVIEW_WIDTH;
    public int TEXTUREVIEW_HEIGHT;
    public Resolution(int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensity) {
        this.VIRTUALDISPLAY_WIDTH = virtualDisplayWidth;
        this.VIRTUALDISPLAY_HEIGHT = virtualDisplayHeight;
        this.VIRTUALDISPLAY_DENSITYDPI = virutalDisplayDensity;
        this.TEXTUREVIEW_WIDTH = virtualDisplayWidth;
        this.TEXTUREVIEW_HEIGHT = virtualDisplayHeight;
    }

    public Resolution(int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensity, int textureViewWidth, int textureViewHeight) {
        this.VIRTUALDISPLAY_WIDTH = virtualDisplayWidth;
        this.VIRTUALDISPLAY_HEIGHT = virtualDisplayHeight;
        this.VIRTUALDISPLAY_DENSITYDPI = virutalDisplayDensity;
        this.TEXTUREVIEW_WIDTH = textureViewWidth;
        this.TEXTUREVIEW_HEIGHT = textureViewHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(VIRTUALDISPLAY_WIDTH, VIRTUALDISPLAY_HEIGHT, VIRTUALDISPLAY_DENSITYDPI, TEXTUREVIEW_WIDTH, TEXTUREVIEW_HEIGHT);
    }

    @Override
    public String toString() {
        return "Resolution{" + "VIRTUALDISPLAY_WIDTH=" + VIRTUALDISPLAY_WIDTH + ", VIRTUALDISPLAY_HEIGHT=" + VIRTUALDISPLAY_HEIGHT + ", VIRTUALDISPLAY_DENSITYDPI=" + VIRTUALDISPLAY_DENSITYDPI + ", TEXTUREVIEW_WIDTH=" + TEXTUREVIEW_WIDTH + ", TEXTUREVIEW_HEIGHT=" + TEXTUREVIEW_HEIGHT + '}';
    }
}