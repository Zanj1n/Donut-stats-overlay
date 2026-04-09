package net.donutsmp.balance.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.donutsmp.balance.client.config.DonutBalanceConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Renders stat lines using the same billboard-style path as vanilla nametags
 * (called from {@link net.donutsmp.balance.mixin.PlayerRendererMixin} at the end of {@code renderNameTag}).
 */
public final class DonutBalanceNametagExtras {
    private static final double MAX_DIST_SQR = 64.0 * 64.0;
    private static final float LINE_STEP = 10.0F;

    private DonutBalanceNametagExtras() {
    }

    public static void render(
            PlayerRenderer renderer,
            PlayerRenderState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        DonutBalanceConfig cfg = DonutBalanceConfig.get();
        if (!cfg.enabled || !DonutSmpServerDetector.shouldShowOverlay(cfg)) {
            return;
        }
        if (state.distanceToCameraSq > MAX_DIST_SQR) {
            return;
        }
        if (state.isInvisibleToPlayer) {
            return;
        }

        String lookupName = state.name;
        if (lookupName == null || lookupName.isEmpty()) {
            return;
        }

        LocalPlayer self = mc.player;
        Entity entity = mc.level.getEntity(state.id);
        if (entity instanceof Player) {
            if (entity == self && !cfg.showSelf) {
                return;
            }
        } else if (self.getGameProfile().getName().equalsIgnoreCase(lookupName) && !cfg.showSelf) {
            return;
        }

        if (state.isCrouching) {
            return;
        }

        List<Component> lines = DonutBalanceOverlayText.buildLines(lookupName);
        if (lines.isEmpty()) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        Vec3 attach = state.nameTagAttachment;
        int n = lines.size();
        // User-configurable vertical placement from Mod Menu.
        double baseClearance = cfg.nametagClearance;
        double liftPerLine = cfg.nametagLiftPerLine;
        double lift = baseClearance + n * liftPerLine;
        double x;
        double y;
        double z;
        if (attach != null && attach.lengthSqr() > 1.0E-8) {
            x = state.x + attach.x;
            y = state.y + attach.y + lift;
            z = state.z + attach.z;
        } else {
            // Rare fallback if attachment not filled — approximate head/nametag height.
            x = state.x;
            y = state.y + state.boundingBoxHeight + 0.5 + lift;
            z = state.z;
        }

        Font font = renderer.getFont();

        poseStack.pushPose();
        poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        float yText = (lines.size() - 1) * LINE_STEP * 0.5F;
        for (Component line : lines) {
            float w = font.width(line);
            font.drawInBatch(
                    line,
                    -w / 2.0F,
                    yText,
                    0xFFFFFFFF,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    packedLight
            );
            yText -= LINE_STEP;
        }
        poseStack.popPose();
    }
}
