package wb.stardewhud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wb.stardewhud.config.ModConfig;
import wb.stardewhud.config.ConfigScreenManager;
import wb.stardewhud.hud.HudRenderer;

@Mod(StardewHUD.MOD_ID)
public class StardewHUD {
    public static final String MOD_ID = "stardewhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HudRenderer hudRenderer;
    private static ModConfig config;

    public StardewHUD(IEventBus modEventBus, ModContainer modContainer) {
        // 注册客户端设置事件
        modEventBus.addListener(this::onClientSetup);
        // 注册NeoForge事件总线
        NeoForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("StardewHUD 正在初始化...");

        // 检查其他模组是否存在的工具方法
        if (isModLoaded("modmenu")) {
            LOGGER.info("检测到 ModMenu，配置界面将可用");
        } else {
            LOGGER.info("使用原生NeoForge配置界面");
        }

        // 初始化配置
        config = new ModConfig();
        config.load();

        // 初始化HUD渲染器
        hudRenderer = new HudRenderer(config);

        // 注册配置界面
        ConfigScreenManager.register();

        LOGGER.info("StardewHUD 初始化完成！");
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        // 渲染实际的HUD
        if (hudRenderer != null && hudRenderer.shouldRender()) {
            hudRenderer.render(event.getGuiGraphics(), event.getPartialTick().getRealtimeDeltaTicks());
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (hudRenderer != null) {
            hudRenderer.update();
        }
    }

    public static HudRenderer getHudRenderer() {
        return hudRenderer;
    }

    public static ModConfig getConfig() {
        if (config == null) {
            LOGGER.warn("配置还未初始化，正在创建默认配置...");
            config = new ModConfig();
            config.load();
        }
        return config;
    }

    // 检查其他模组是否存在的工具方法
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}