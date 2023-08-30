package io.redstudioragnarok.alfheim.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;

import java.util.List;

/**
 * Mostly copied from <a href="https://github.com/ACGaming/UniversalTweaks/blob/main/src/main/java/mod/acgaming/universaltweaks/util/compat/UTCompatScreen.java#L45">Universal Tweaks Compat Screen</a>
 */
public class WarningScreen extends CustomModLoadingErrorDisplayException {

    public final List<String> messages;
    private int textHeight;

    public WarningScreen(final List<String> messages) {
        this.messages = messages;
    }

    @Override
    public void initGui(final GuiErrorScreen guiErrorScreen, final FontRenderer fontRenderer) {
        textHeight = messages.size() * fontRenderer.FONT_HEIGHT;
    }

    @Override
    public void drawScreen(final GuiErrorScreen guiErrorScreen, final FontRenderer fontRenderer, final int mouseX, final int mouseY, final float partialTicks) {
        int posY = guiErrorScreen.height / 2 - textHeight / 2;

        for (final String message : messages) {
            guiErrorScreen.drawCenteredString(fontRenderer, message, guiErrorScreen.width / 2, posY, 16777215);
            posY += fontRenderer.FONT_HEIGHT;
        }
    }
}
