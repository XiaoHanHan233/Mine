package net.optifine.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiPerformanceSettingsOF extends GuiScreen
{
    private GuiScreen prevScreen;
    protected String title;
    private GameSettings settings;
    private static GameSettings.Options[] enumOptions = new GameSettings.Options[] {GameSettings.Options.SMOOTH_FPS, GameSettings.Options.SMOOTH_WORLD, GameSettings.Options.FAST_RENDER, GameSettings.Options.FAST_MATH, GameSettings.Options.CHUNK_UPDATES, GameSettings.Options.CHUNK_UPDATES_DYNAMIC, GameSettings.Options.RENDER_REGIONS, GameSettings.Options.LAZY_CHUNK_LOADING, GameSettings.Options.SMART_ANIMATIONS};
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiPerformanceSettingsOF(GuiScreen guiscreen, GameSettings gamesettings)
    {
        this.prevScreen = guiscreen;
        this.settings = gamesettings;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        this.title = I18n.format("of.options.performanceTitle");
        this.buttonList.clear();

        for (int i = 0; i < enumOptions.length; ++i)
        {
            GameSettings.Options gamesettings$options = enumOptions[i];
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 + 21 * (i / 2) - 12;

            if (!gamesettings$options.isFloat())
            {
                final var guiOptionButtonOF = new GuiOptionButtonOF(gamesettings$options.getOrdinal(), j, k, gamesettings$options, this.settings.getKeyBinding(gamesettings$options));
                if (guiOptionButtonOF.getOption() == GameSettings.Options.FAST_RENDER || guiOptionButtonOF.getOption() == GameSettings.Options.RENDER_REGIONS) {
                    guiOptionButtonOF.enabled = false;
                    Minecraft.getMinecraft().gameSettings.ofFastRender = false;
                    Minecraft.getMinecraft().gameSettings.ofRenderRegions = false;
                }
                this.buttonList.add(guiOptionButtonOF);
            }
            else
            {
                this.buttonList.add(new GuiOptionSliderOF(gamesettings$options.getOrdinal(), j, k, gamesettings$options));
            }
        }

        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 168 + 11, I18n.format("gui.done")));
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton guibutton)
    {
        if (guibutton.enabled)
        {
            if (guibutton.id < 200 && guibutton instanceof GuiOptionButton)
            {
                this.settings.setOptionValue(((GuiOptionButton)guibutton).getOption(), 1);
                guibutton.displayString = this.settings.getKeyBinding(GameSettings.Options.byOrdinal(guibutton.id));
            }

            if (guibutton.id == 200)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(this.prevScreen);
            }
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int x, int y, float f)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 15, 16777215);
        super.drawScreen(x, y, f);
        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
