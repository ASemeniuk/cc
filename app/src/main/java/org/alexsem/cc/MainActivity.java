package org.alexsem.cc;

import android.app.Activity;
import android.os.Bundle;

import org.alexsem.cc.widget.BoardView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new BoardView(this));
    }
}
