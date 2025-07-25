package net.minecraft.client.renderer.entity;

import dev.diona.southside.Southside;
import dev.diona.southside.module.modules.render.ItemPhysics;
import dev.diona.southside.util.misc.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class RenderEntityItem extends Render<EntityItem> {
    private final RenderItem itemRenderer;
    private final Random random = new Random();

    private TimerUtil delayTimer = new TimerUtil();

    public RenderEntityItem(RenderManager renderManagerIn, RenderItem p_i46167_2_) {
        super(renderManagerIn);
        this.itemRenderer = p_i46167_2_;
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    private int transformModelCount(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_) {
        ItemStack itemstack = itemIn.getItem();
        Item item = itemstack.getItem();

        if (item == null) {
            return 0;
        } else {
            if (Southside.moduleManager.getModuleByClass(ItemPhysics.class).isEnabled()) {
                boolean var12 = p_177077_9_.isAmbientOcclusion();
                int var13 = this.getModelCount(itemstack);
                if (!(item instanceof ItemBlock))
                    GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + 0.1, (float) p_177077_6_);
                else
                    GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + 0.2, (float) p_177077_6_);

                float var16;

                float pitch = itemIn.onGround ? 90 : itemIn.rotationPitch;

                if (delayTimer.hasReached(5)) {
                    itemIn.rotationPitch += 1;
                }

                if (itemIn.rotationPitch > 180)
                    itemIn.rotationPitch = -180;

                GlStateManager.rotate(pitch, 1, 0, 0);

                GlStateManager.rotate(itemIn.rotationYaw, 0, 0, 1);

                if (!var12) {
                    var16 = -0.0F * (float) (var13 - 1) * 0.5F;
                    float var17 = -0.0F * (float) (var13 - 1) * 0.5F;
                    float var18 = -0.046875F * (float) (var13 - 1) * 0.5F;
                    GlStateManager.translate(var16, var17, var18);
                }

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                return var13;
            } else {
                boolean flag = p_177077_9_.isGui3d();
                int i = this.getModelCount(itemstack);
                float f = 0.25F;
                float f1 = MathHelper.sin(((float) itemIn.getAge() + p_177077_8_) / 10.0F + itemIn.hoverStart) * 0.1F + 0.1F;
                float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
                GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + f1 + 0.25F * f2, (float) p_177077_6_);

                if (flag || this.renderManager.options != null) {
                    float f3 = 180.0F - (Minecraft.getMinecraft().getRenderManager()).playerViewY;
                    GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(((Minecraft.getMinecraft()).gameSettings.thirdPersonView == 2) ? (Minecraft.getMinecraft().getRenderManager()).playerViewX : -(Minecraft.getMinecraft().getRenderManager()).playerViewX, 1.0F, 0.0F, 0.0F);
                }

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                return i;
            }
        }
    }

    private int getModelCount(ItemStack stack) {
        int i = 1;

        if (stack.getCount() > 48) {
            i = 5;
        } else if (stack.getCount() > 32) {
            i = 4;
        } else if (stack.getCount() > 16) {
            i = 3;
        } else if (stack.getCount() > 1) {
            i = 2;
        }

        return i;
    }

    /**
     * Renders the desired {@code T} type Entity.
     */
    public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ItemStack itemstack = entity.getItem();
        int i = itemstack.isEmpty() ? 187 : Item.getIdFromItem(itemstack.getItem()) + itemstack.getMetadata();
        this.random.setSeed((long) i);
        boolean flag = false;

        if (this.bindEntityTexture(entity)) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).setBlurMipmap(false, false);
            flag = true;
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.pushMatrix();
        IBakedModel ibakedmodel = this.itemRenderer.getItemModelWithOverrides(itemstack, entity.world, (EntityLivingBase) null);
        int j = this.transformModelCount(entity, x, y, z, partialTicks, ibakedmodel);
        float f = ibakedmodel.getItemCameraTransforms().ground.scale.x;
        float f1 = ibakedmodel.getItemCameraTransforms().ground.scale.y;
        float f2 = ibakedmodel.getItemCameraTransforms().ground.scale.z;
        boolean flag1 = ibakedmodel.isGui3d();

        if (!flag1) {
            float f3 = -0.0F * (float) (j - 1) * 0.5F * f;
            float f4 = -0.0F * (float) (j - 1) * 0.5F * f1;
            float f5 = -0.09375F * (float) (j - 1) * 0.5F * f2;
            GlStateManager.translate(f3, f4, f5);
        }

        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }

        for (int k = 0; k < j; ++k) {
            if (flag1) {
                GlStateManager.pushMatrix();

                if (k > 0) {
                    float f7 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f9 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f6 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    GlStateManager.translate(f7, f9, f6);
                }

                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                if (itemstack.getItem() instanceof ItemBlock) {
                    this.itemRenderer.renderItem(itemstack, ibakedmodel);
                } else {
                    GlStateManager.disableLighting();
                    GlStateManager.scale(1.0F, 1.0F, 0.0F);
                    this.itemRenderer.renderItem(itemstack, ibakedmodel);
                    GlStateManager.enableLighting();
                }
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();

                if (k > 0) {
                    float f8 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float f10 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    GlStateManager.translate(f8, f10, 0.0F);
                }

                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(itemstack, ibakedmodel);
                GlStateManager.popMatrix();
                GlStateManager.translate(0.0F * f, 0.0F * f1, 0.09375F * f2);
            }
        }

        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.bindEntityTexture(entity);

        if (flag) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
     */
    protected ResourceLocation getEntityTexture(EntityItem entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
