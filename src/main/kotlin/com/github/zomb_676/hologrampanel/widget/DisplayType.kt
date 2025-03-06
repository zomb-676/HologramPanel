package com.github.zomb_676.hologrampanel.widget

enum class DisplayType {

    /**
     * display as detail as possible
     */
    MAXIMUM,

    /**
     * display normal
     */
    NORMAL,

    /**
     * display minimal
     */
    MINIMAL,

    /**
     * the widget is at screen edge or out of screen
     *
     * render a simple icon may be a good idea
     */
    SCREEN_EDGE
}