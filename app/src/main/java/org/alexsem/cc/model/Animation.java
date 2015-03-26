package org.alexsem.cc.model;

import android.graphics.Canvas;

public interface Animation {

    public void tick();
    public void draw(Canvas c);
    public boolean isFinished();
    public void finish();
}
