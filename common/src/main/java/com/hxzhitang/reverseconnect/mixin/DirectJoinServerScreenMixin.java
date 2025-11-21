package com.hxzhitang.reverseconnect.mixin;

import com.hxzhitang.reverseconnect.screen.ProxyServerScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DirectJoinServerScreen.class)
public class DirectJoinServerScreenMixin extends Screen {
    @Final
    @Shadow
    private static Component ENTER_IP_LABEL;

    @Shadow
    private EditBox ipEdit;

    @Shadow
    private Button selectButton;

    protected DirectJoinServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "RETURN"))
    protected void init(CallbackInfo ci) {
        DirectJoinServerScreen self = (DirectJoinServerScreen) (Object)this;
        ipEdit.setPosition(self.width / 2 - 100, 96);
        selectButton.setPosition(self.width / 2 - 100, self.height / 4 + 96 + 12 - 24);

        this.addRenderableWidget(Button.builder(Component.translatable("reverse_connect.open_proxyserverscreen"), (p_95981_) -> {
            this.minecraft.setScreen(new ProxyServerScreen(((DirectJoinServerScreen)(Object)this)));
        }).bounds(self.width / 2 - 100, self.height / 4 + 96 + 12, 200, 20).build());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private int newDrawString(GuiGraphics instance, Font font, Component text, int x, int y, int color) {
        DirectJoinServerScreen self = (DirectJoinServerScreen) (Object)this;
        instance.drawString(font, ENTER_IP_LABEL, self.width / 2 - 100, 80, 10526880);
        return x;
    }
}
