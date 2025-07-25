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

package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

public class CreditsPage extends Page {
    public CreditsPage() {
        super("Credits");
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        nanoVGHelper.drawSvg(vg, SVGs.SKIDONION, x + 570f, y - 10f, 110, 110);
        nanoVGHelper.drawText(vg, "感谢 SkidOnion 提供保护方案", x + 570, y + 110, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, "   https://skidonion.tech/", x + 570, y + 135, -1, 12, Fonts.REGULAR);

        nanoVGHelper.drawSvg(vg, SVGs.SOUTHSIDE, x + 15f, y, 110, 110);

        y += 10;

        nanoVGHelper.drawText(vg, "OneConfig 兼容层编写团队", x + 20, y + 110, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, " - 花花派对", x + 20, y + 135, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, "版权说明", x + 20, y + 175, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, "   OneConfig 以 LGPL 协议开源于 https://github.com/Polyfrost/OneConfig。", x + 20, y + 200, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, "   Southside 团队以编写 forge 兼容层的方法给客户端导入了 OneConfig 作为配置管理系统。", x + 20, y + 215, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, "   符合 LGPL 的标准，仅仅用到了 OneConfig 的功能，故可保持本软件闭源。", x + 20, y + 230, -1, 12, Fonts.REGULAR);
        y += 240;
        nanoVGHelper.drawSvg(vg, SVGs.ONECONFIG_FULL_DARK, x + 15f, y + 20f, 474, 102);
        y -= 52;

        nanoVGHelper.drawText(vg, "Development Team", x + 20, y + 180, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, " - Wyvest - OG Team - Gradle, NanoVGHelper, VCAL, Utilities, GUI Frontend", x + 20, y + 205, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Caledonian - Designer", x + 20, y + 220, -1, 12, Fonts.REGULAR);        // +15/line
        nanoVGHelper.drawText(vg, " - nextdaydelivery - OG Team - GUI Frontend, NanoVGHelper, Utilities", x + 20, y + 235, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Pauline - Utilities", x + 20, y + 250, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - DeDiamondPro - OG Team -  Config Backend, GUI Frontend, HUD", x + 20, y + 295, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - xtrm - Multiversion support, GUI Frontend", x + 20, y + 265, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - MoonTidez - OG Team - Designer", x + 20, y + 280, -1, 12, Fonts.REGULAR);

        nanoVGHelper.drawText(vg, "Libraries", x + 20, y + 333, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, " - LWJGLTwoPointFive (DJTheRedstoner) - LWJGL2 function provider", x + 20, y + 355, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - #getResourceAsStream (SpinyOwl) - IO Utility and shadow", x + 20, y + 370, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - NanoVG (memononen) - NanoVG Library", x + 20, y + 385, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - UniversalCraft (Essential team) - Multiversioning bindings", x + 20, y + 400, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - https://easings.net/ - Easing functions", x + 20, y + 415, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Seraph (Scherso) - Locraw and Multithreading utilities", x + 20, y + 430, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Deencapsulation (xDark) - Java 9+ utilities", x + 20, y + 445, -1, 12, Fonts.REGULAR);
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
