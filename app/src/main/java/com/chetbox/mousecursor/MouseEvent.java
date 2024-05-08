package com.chetbox.mousecursor;

/**
 * Created by chetan on 30/04/15.
 */
public class MouseEvent {

    public static final int
            SHOW = 1,
            HIDE = 2;

    public final int type;

    public MouseEvent(int type) {
        this.type = type;
    }
}
