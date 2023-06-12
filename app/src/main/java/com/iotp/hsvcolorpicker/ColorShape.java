package com.iotp.hsvcolorpicker;

import androidx.annotation.IntDef;

@IntDef({ ColorShape.SQUARE, ColorShape.CIRCLE }) public @interface ColorShape {

    int SQUARE = 0;

    int CIRCLE = 1;
}
