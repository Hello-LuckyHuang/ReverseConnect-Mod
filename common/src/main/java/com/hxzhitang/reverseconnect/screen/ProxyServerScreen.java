package com.hxzhitang.reverseconnect.screen;

import com.hxzhitang.reverseconnect.proxy.edgeproxy.EdgeProxyServer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;

public class ProxyServerScreen extends Screen {
    private static final Component PORT_UNAVAILABLE = Component.translatable("lanServer.port.unavailable.new", 1024, 65535);
    private static final Component INVALID_PORT = Component.translatable("lanServer.port.invalid.new", 1024, 65535);

    private final Screen lastScreen;
    private static int proxyPort = 25567;
    private static int userPort = 25566;
    private static String infoTip = "";

    private Button openButton;

    private EditBox proxyPortEdit;
    private EditBox userPortEdit;

    private static boolean proxyPortOk = false;
    private static boolean userPortOk = false;

    private static EdgeProxyServer edgeProxyServer = null;
    private static Thread serverThread = null;

    public ProxyServerScreen(Screen lastScreen) {
        super(Component.translatable("reverse_connect.proxyserver_title"));
        this.lastScreen = lastScreen;
    }

    protected void init() {
        proxyPortEdit = new EditBox(this.font, this.width / 2 - 75, 55, 150, 20, Component.translatable("lanServer.port"));
        userPortEdit = new EditBox(this.font, this.width / 2 - 75, 105, 150, 20, Component.translatable("lanServer.port"));

        openButton = Button.builder(Component.translatable("reverse_connect.open"), (p_280826_) -> {
            if (serverThread != null && serverThread.isAlive()) {
                edgeProxyServer.close();
            } else {
                edgeProxyServer = new EdgeProxyServer(Integer.parseInt(userPortEdit.getValue()), Integer.parseInt(proxyPortEdit.getValue()));
                serverThread = new Thread(edgeProxyServer);
                serverThread.start();
            }

        }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build();

        this.addRenderableWidget(proxyPortEdit);
        this.addRenderableWidget(userPortEdit);
        this.addRenderableWidget(openButton);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_280824_) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());

        this.proxyPortEdit.setResponder((p_258130_) -> {
            Component component = this.tryParsePort(p_258130_);
            this.proxyPortEdit.setHint(Component.literal("" + this.proxyPort).withStyle(ChatFormatting.DARK_GRAY));
            if (component == null) {
                this.proxyPortEdit.setTextColor(14737632);
                this.proxyPortEdit.setTooltip((Tooltip)null);
                proxyPortOk = true;
            } else {
                this.proxyPortEdit.setTextColor(16733525);
                this.proxyPortEdit.setTooltip(Tooltip.create(component));
                proxyPortOk = false;
            }
        });
        this.proxyPortEdit.setHint(Component.literal("" + this.proxyPort).withStyle(ChatFormatting.DARK_GRAY));

        this.userPortEdit.setResponder((p_258130_) -> {
            Component component = this.tryParsePort(p_258130_);
            this.userPortEdit.setHint(Component.literal("" + this.userPort).withStyle(ChatFormatting.DARK_GRAY));
            if (component == null) {
                this.userPortEdit.setTextColor(14737632);
                this.userPortEdit.setTooltip((Tooltip)null);
                userPortOk = true;
            } else {
                this.userPortEdit.setTextColor(16733525);
                this.userPortEdit.setTooltip(Tooltip.create(component));
                userPortOk = false;
            }
        });
        this.userPortEdit.setHint(Component.literal("" + this.userPort).withStyle(ChatFormatting.DARK_GRAY));

    }

    public void render(GuiGraphics p_281738_, int p_96653_, int p_96654_, float p_96655_) {
        this.renderBackground(p_281738_);
        p_281738_.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        p_281738_.drawCenteredString(this.font, Component.translatable("reverse_connect.proxy_port"), this.width / 2, 40, 10526880);
        p_281738_.drawCenteredString(this.font, Component.translatable("reverse_connect.user_port"), this.width / 2, 90, 10526880);
        p_281738_.drawCenteredString(this.font, Component.translatable(infoTip), this.width / 2, 135, 16777215);
        if (serverThread != null && serverThread.isAlive()) {
            p_281738_.drawCenteredString(this.font, Component.translatable("reverse_connect.server_started"), this.width / 2, 155, 16777215);
        }
        super.render(p_281738_, p_96653_, p_96654_, p_96655_);
    }

    public void tick() {
        super.tick();
        proxyPortEdit.tick();
        userPortEdit.tick();

        if (serverThread != null && serverThread.isAlive()) {
            openButton.setMessage(Component.translatable("reverse_connect.close"));
            openButton.active = true;
        } else {
            openButton.setMessage(Component.translatable("reverse_connect.open"));
            openButton.active = proxyPortOk && userPortOk && (!proxyPortEdit.getValue().equals(userPortEdit.getValue()));
        }

        if (EdgeProxyServer.getClientCount() == 0) {
            infoTip = "";
        } else {
            infoTip = "reverse_connect.proxy_connect";
        }
    }

    private Component tryParsePort(String testPort) {
        try {
            int post = Integer.parseInt(testPort);
            if (post >= 1024 && post <= 65535) {
                return !HttpUtil.isPortAvailable(post) ? PORT_UNAVAILABLE : null;
            } else {
                return INVALID_PORT;
            }
        } catch (NumberFormatException numberformatexception) {
            return INVALID_PORT;
        }
    }
}
