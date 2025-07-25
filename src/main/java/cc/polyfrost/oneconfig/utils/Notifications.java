/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.internal.utils.Notification;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import me.kbrewster.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.Icon;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class Notifications {
    public static final Notifications INSTANCE = new Notifications();
    // animation stores the bottom y of the notification
    private final LinkedHashMap<Notification, Animation> notifications = new LinkedHashMap<>();
    private final float DEFAULT_DURATION = 4000;

    private Notifications() {
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param icon        The icon of the notification, null for none
     * @param duration    The duration the notification is on screen in ms
     * @param progressbar A callable that returns the progress from 0-1
     * @param action      The action executed when the notification is pressed
     */
    public void send(String title, String message, @Nullable Icon icon, float duration, @Nullable Callable<Float> progressbar, @Nullable Runnable action) {
        Notification notification = new Notification(title, message, icon, duration, progressbar, action);
        notifications.put(notification, new DummyAnimation(-1));
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param duration    The duration the notification is on screen in ms
     * @param progressbar A callable that returns the progress from 0-1
     * @param action      The action executed when the notification is pressed
     */
    public void send(String title, String message, float duration, @Nullable Callable<Float> progressbar, @Nullable Runnable action) {
        send(title, message, null, duration, progressbar, action);
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param icon        The icon of the notification, null for none
     * @param duration    The duration the notification is on screen in ms
     * @param progressbar A callable that returns the progress from 0-1
     */
    public void send(String title, String message, @Nullable Icon icon, float duration, @Nullable Callable<Float> progressbar) {
        send(title, message, icon, duration, progressbar, null);
    }

    /**
     * Send a notification to the user
     *
     * @param title    The title of the notification
     * @param message  The message of the notification
     * @param icon     The icon of the notification, null for none
     * @param duration The duration the notification is on screen in ms
     * @param action   The action executed when the notification is pressed
     */
    public void send(String title, String message, @Nullable Icon icon, float duration, @Nullable Runnable action) {
        send(title, message, icon, duration, null, action);
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param duration    The duration the notification is on screen in ms
     * @param progressbar A callable that returns the progress from 0-1
     */
    public void send(String title, String message, float duration, @Nullable Callable<Float> progressbar) {
        send(title, message, duration, progressbar, null);
    }

    /**
     * Send a notification to the user
     *
     * @param title    The title of the notification
     * @param message  The message of the notification
     * @param duration The duration the notification is on screen in ms
     * @param action   The action executed when the notification is pressed
     */
    public void send(String title, String message, float duration, @Nullable Runnable action) {
        send(title, message, duration, null, action);
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param icon        The icon of the notification, null for none
     * @param progressbar A callable that returns the progress from 0-1
     */
    public void send(String title, String message, @Nullable Icon icon, @Nullable Callable<Float> progressbar) {
        send(title, message, icon, DEFAULT_DURATION, progressbar);
    }

    /**
     * Send a notification to the user
     *
     * @param title   The title of the notification
     * @param message The message of the notification
     * @param icon    The icon of the notification, null for none
     * @param action  The action executed when the notification is pressed
     */
    public void send(String title, String message, @Nullable Icon icon, @Nullable Runnable action) {
        send(title, message, icon, DEFAULT_DURATION, action);
    }

    /**
     * Send a notification to the user
     *
     * @param title       The title of the notification
     * @param message     The message of the notification
     * @param progressbar A callable that returns the progress from 0-1
     */
    public void send(String title, String message, @Nullable Callable<Float> progressbar) {
        send(title, message, DEFAULT_DURATION, progressbar);
    }

    /**
     * Send a notification to the user
     *
     * @param title    The title of the notification
     * @param message  The message of the notification
     * @param icon     The icon of the notification, null for none
     * @param duration The duration the notification is on screen in ms
     */
    public void send(String title, String message, @Nullable Icon icon, float duration) {
        send(title, message, icon, duration, (Callable<Float>) null);
    }

    /**
     * Send a notification to the user
     *
     * @param title   The title of the notification
     * @param message The message of the notification
     * @param action  The action executed when the notification is pressed
     */
    public void send(String title, String message, @Nullable Runnable action) {
        send(title, message, DEFAULT_DURATION, action);
    }

    /**
     * Send a notification to the user
     *
     * @param title    The title of the notification
     * @param message  The message of the notification
     * @param duration The duration the notification is on screen in ms
     */
    public void send(String title, String message, float duration) {
        send(title, message, duration, (Callable<Float>) null);
    }

    /**
     * Send a notification to the user
     *
     * @param title   The title of the notification
     * @param message The message of the notification
     * @param icon    The icon of the notification, null for none
     */
    public void send(String title, String message, @Nullable Icon icon) {
        send(title, message, icon, (Callable<Float>) null);
    }

    /**
     * Send a notification to the user
     *
     * @param title   The title of the notification
     * @param message The message of the notification
     */
    public void send(String title, String message) {
        send(title, message, (Callable<Float>) null);
    }

    private float deltaTime = 0;

    @Subscribe
    private void onRenderEvent(RenderEvent event) {
        if (event.stage == Stage.START) {
            deltaTime += GuiUtils.getDeltaTime(); // add up deltatime since we might not render every frame because of hud caching
            return;
        }
        if (notifications.isEmpty()) {
            deltaTime = 0;
            return;
        }
        NanoVGHelper.INSTANCE.setupAndDraw((vg) -> {
            float desiredPosition = -16f;
            float scale = OneConfigGui.getNotificationScaleFactor();
            for (Map.Entry<Notification, Animation> entry : notifications.entrySet()) {
                if (entry.getValue().getEnd() == -1f)
                    entry.setValue(new DummyAnimation(desiredPosition));
                else if (desiredPosition != entry.getValue().getEnd())
                    entry.setValue(new EaseInOutQuad(250, entry.getValue().get(0), desiredPosition, false));
                float height = entry.getKey().draw(vg, UResolution.getWindowHeight() / scale + entry.getValue().get(deltaTime), scale, deltaTime);
                desiredPosition -= height + 16f;
            }
            notifications.entrySet().removeIf(entry -> entry.getKey().isFinished());
        });
        deltaTime = 0;
    }

    private Animation dummyAnimation;
    private static final Icon DEFAULT_ICON = new Icon(SVGs.ONECONFIG_HEAD_DARK);
    private static final File crashPatchSkyClientFile = new File("./OneConfig/CrashPatch/SKYCLIENT");
    private static final File oldCrashPatchSkyClientFile = new File("./W-OVERFLOW/CrashPatch/SKYCLIENT");

    @Subscribe
    private void onTickEvent(TickEvent event) {
        if (Preferences.firstLaunch.getValue()) {
            if (event.stage == Stage.START && !(Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui) && Platform.getServerPlatform().doesPlayerExist() && UMinecraft.getMinecraft().player.ticksExisted > 20) {
                Preferences.firstLaunch.setValue(false);
                Preferences.getInstance().save();
                dummyAnimation = new EaseInOutQuad(4000, 0, 1, false);
//                boolean isSkyClient = isSkyClient();
//                String message = isSkyClient ? "SkyClient now includes OneConfig, the next-gen config library for Minecraft. You can now simply press '" + Preferences.INSTANCE.oneConfigKeyBind.getValue().getDisplay() + "' to configure all your mods!" : "Press '" + Preferences.INSTANCE.oneConfigKeyBind.getValue().getDisplay() + "' to open OneConfig, the next-gen config library for Minecraft.";
                send("Welcome to Southside!", "Press '" + Preferences.INSTANCE.oneConfigKeyBind.getValue().getDisplay() + "' to open the Click GUI.", new Icon(SVGs.SOUTHSIDE), -1f, () -> {
                    if (Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui || Preferences.oneconfigOpened) {
                        Preferences.oneconfigOpened = true;
                        Preferences.getInstance().save();
                        return dummyAnimation.get(GuiUtils.getDeltaTime());
                    } else {
                        return 0f;
                    }
                }, () -> GuiUtils.displayScreen(OneConfigGui.create()));
            }
        }
    }

//    private boolean isSkyClient() {
//        if (crashPatchSkyClientFile.exists()) return true;
//        if (oldCrashPatchSkyClientFile.exists()) return true;
//        return false;
////        return Platform.getLoaderPlatform().getLoadedMods().stream().anyMatch(mod -> mod != null && StringUtils.contains(mod.id, "skyclient") || StringUtils.contains(mod.id, "skyblockclient") || StringUtils.equals(mod.id, "scc"));
//    }
}
