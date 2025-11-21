package com.hxzhitang.reverseconnect.screen;

import com.hxzhitang.reverseconnect.proxy.coreproxy.CoreProxyMain;
import com.hxzhitang.reverseconnect.proxy.tools.IPAddressParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class ReverseConnectScreen extends Screen {
    private final Screen lastScreen;
    private Button connectButton;
    private static Thread connectThread = null;
    private static String infoTip = "";
    private static String errTip = "";

    public ReverseConnectScreen(Screen lastScreen) {
        super(Component.translatable("reverse_connect.title"));
        this.lastScreen = lastScreen;
    }

    protected void init() {
        EditBox proxyServerAddressEdit = new EditBox(this.font, this.width / 2 - 150, 80, 300, 20, Component.translatable("addServer.enterIp"));
        proxyServerAddressEdit.setMaxLength(128);
        connectButton = Button.builder(Component.translatable("reverse_connect.connect"), (p_280826_) -> {
            connectEdgeServer(this.minecraft.getSingleplayerServer().getPort(), proxyServerAddressEdit.getValue());
        }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build();

        this.addRenderableWidget(proxyServerAddressEdit);
        this.addRenderableWidget(connectButton);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_280824_) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
    }

    public void render(GuiGraphics p_281738_, int p_96653_, int p_96654_, float p_96655_) {
        this.renderBackground(p_281738_);
        p_281738_.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
        p_281738_.drawCenteredString(this.font, Component.translatable("reverse_connect.edge_ip_edit"), this.width / 2, 65, 16777215);
        p_281738_.drawCenteredString(this.font, Component.translatable(errTip), this.width / 2, 105, 16733525);
        p_281738_.drawCenteredString(this.font, Component.translatable(infoTip), this.width / 2, 125, 16777215);
        super.render(p_281738_, p_96653_, p_96654_, p_96655_);
    }

    public void tick() {
        super.tick();
        // 开启对局域网开放才可连接代理服务器
        if (this.minecraft.getSingleplayerServer().isPublished()) {
            if (connectThread != null && connectThread.isAlive()) {
                connectButton.setTooltip(Tooltip.create(Component.translatable("reverse_connect.publish_tip")));
                connectButton.active = false;
            } else {
                connectButton.setTooltip(null);
                connectButton.active = true;
            }
        } else {
            connectButton.setTooltip(Tooltip.create(Component.translatable("reverse_connect.publish_tip_2")));
            connectButton.active = false;
        }
    }

    // 连接代理服务器
    private void connectEdgeServer(int port, String edgeAddress) {
        try {
            if (connectThread != null && connectThread.isAlive())
                return;
            IPAddressParser.IPAddressInfo info = IPAddressParser.parseAddress(edgeAddress);
            CoreProxyMain coreProxy = new CoreProxyMain(
                    info.getIpAddress(),
                    info.getPort(),
                    "127.0.0.1",
                    port,
                    (statusString, status) -> {
                        infoTip = statusString;
                        switch (status) {
                            case INFO: break;
                            case ERROR: break;
                            case START: break;
                            case CLOSE:
                                Component component0 = ComponentUtils.copyOnClickText(String.valueOf(info.getPort()));
                                this.minecraft.gui.getChat().addMessage(Component.translatable("reverse_connect.connect_close", component0));
                                break;
                            case COMPLETED:
                                Component component1 = ComponentUtils.copyOnClickText(String.valueOf(info.getPort()));
                                this.minecraft.gui.getChat().addMessage(Component.translatable("reverse_connect.connect_success", component1));
//                                this.minecraft.setScreen(null);
                                break;
                        }
                    }
            );
            connectThread = new Thread(coreProxy);
            connectThread.start();
            errTip = "";
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                errTip = "reverse_connect.address_err";
            } else {
                errTip = "reverse_connect.connect_err";
            }
        }
    }
}
