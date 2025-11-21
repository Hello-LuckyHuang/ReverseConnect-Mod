package com.hxzhitang.reverseconnect.mixin;

import com.hxzhitang.reverseconnect.screen.ReverseConnectScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    // 按顺序写入局部变量，直到到希望的为止
    private void createPauseMenu(CallbackInfo ci, GridLayout gridlayout, GridLayout.RowHelper gridlayout$rowhelper) {
        if (this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer().isPublished()) {
            gridlayout$rowhelper.addChild(this.reverseConnect$openScreenButton(Component.translatable("reverse_connect.open_rcscreen"), () -> {
                return new ReverseConnectScreen(((PauseScreen)(Object)this));
            }),2);
        }
    }

    @Unique
    private net.minecraft.client.gui.components.Button reverseConnect$openScreenButton(Component p_262567_, Supplier<Screen> p_262581_) {
        return Button.builder(p_262567_, (p_280817_) -> {
            this.minecraft.setScreen(p_262581_.get());
        }).width(204).build();
    }
}
