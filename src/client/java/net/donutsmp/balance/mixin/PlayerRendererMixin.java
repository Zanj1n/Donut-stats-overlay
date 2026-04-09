package net.donutsmp.balance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.donutsmp.balance.client.DonutBalanceNametagExtras;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void donutsmp$statsWithNametag(
            PlayerRenderState state,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        DonutBalanceNametagExtras.render((PlayerRenderer) (Object) this, state, poseStack, buffer, packedLight);
    }
}
