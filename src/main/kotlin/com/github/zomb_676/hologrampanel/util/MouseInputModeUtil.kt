package com.github.zomb_676.hologrampanel.util

import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object MouseInputModeUtil {

    /**
     * cursor is hidden, camera is controlled
     */
    const val WORLD_CURSOR_MODE = GLFW.GLFW_CURSOR_DISABLED

    /**
     * cursor is displayed, is actually moving cursor
     */
    const val MOVE_CURSOR_MODE = GLFW.GLFW_CURSOR_NORMAL

    private fun setCursorMode(cursorMode: Int) {
        if (getCursorMode() == cursorMode) return
        GLFW.glfwSetInputMode(Minecraft.getInstance().window.window, GLFW.GLFW_CURSOR, cursorMode)
    }

    private fun getCursorMode(): Int {
        return GLFW.glfwGetInputMode(Minecraft.getInstance().window.window, GLFW.GLFW_CURSOR)
    }

    @JvmStatic
    fun preventPlayerTurn(): Boolean = Minecraft.getInstance().screen == null && getCursorMode() == MOVE_CURSOR_MODE

    fun tryEnter() {
        if (Minecraft.getInstance().screen != null) return
        val window = Minecraft.getInstance().window
        if (getCursorMode() != MOVE_CURSOR_MODE) {
            setCursorMode(MOVE_CURSOR_MODE)
            Minecraft.getInstance().mouseHandler.xpos = window.screenWidth / 2.0
            Minecraft.getInstance().mouseHandler.ypos = window.screenHeight / 2.0
            GLFW.glfwSetCursorPos(window.window, (window.screenWidth / 2).toDouble(), (window.screenHeight / 2).toDouble())
        }
    }

    fun exit() {
        if (Minecraft.getInstance().screen == null) {
            setCursorMode(WORLD_CURSOR_MODE)
        } else {
            setCursorMode(MOVE_CURSOR_MODE)
        }
    }
}