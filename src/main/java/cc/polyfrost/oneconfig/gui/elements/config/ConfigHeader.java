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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.lang.reflect.Field;

public class ConfigHeader extends BasicOption {

    public ConfigHeader(Field field, Object parent, String name, String category, String subcategory, int size) {
        super(field, parent, name, "", category, subcategory, size);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        ScissorHelper scissorHelper = ScissorHelper.INSTANCE;
        Scissor scissor = scissorHelper.scissor(vg, x, y, size == 1 ? 480 : 992, 32);
        NanoVGHelper.INSTANCE.drawText(vg, name, x, y + 17, nameColor, 24, Fonts.MEDIUM);
        scissorHelper.resetScissor(vg, scissor);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
