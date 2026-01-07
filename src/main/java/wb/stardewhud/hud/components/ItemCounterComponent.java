package wb.stardewhud.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

public class ItemCounterComponent {
    private final HudRenderer hudRenderer;
    private String itemId;
    private Item item;
    private int itemCount = 0;
    private int lastSnapTick = -1;

    private final int COUNTER_WIDTH  = HudRenderer.getCounterWidth();
    private final int COUNTER_HEIGHT = HudRenderer.getCounterHeight();

    private static final int ITEM_LEFT_MARGIN      = 9;
    private static final int TEXT_RIGHT_MARGIN     = 8;
    private static final float TEXT_SCALE          = 1.5f;
    private static final int TEXT_COLOR            = 0xFF8B0000;
    private static final int SHADOW_COLOR          = 0xFFFFFFFF;
    private static final boolean ENABLE_SHADOW     = false;
    private static final int ITEM_ICON_SIZE        = 16;
    private static final int ITEM_VERTICAL_OFFSET  = 4;
    private static final int SCALE_COMPENSATION    = 4;

    private static final String KUBEJS_COIN_ID = "kubejs:coin";

    public ItemCounterComponent(HudRenderer hudRenderer, String itemId) {
        this.hudRenderer = hudRenderer;
        this.itemId = itemId;
        parseItemId();
    }

    public void markInventoryChanged() {
        lastSnapTick = -1;
    }

    public void render(GuiGraphics context, int x, int y) {
        String configItemId = StardewHUD.getConfig().counterItemId;
        if (!configItemId.equals(this.itemId)) {
            this.itemId = configItemId;
            parseItemId();
        }

        float alpha = hudRenderer.getConfig().backgroundAlpha;
        context.setColor(1.0f, 1.0f, 1.0f, alpha);
        context.blit(HudRenderer.COUNTER_BG, x, y, 0, 0, COUNTER_WIDTH, COUNTER_HEIGHT, COUNTER_WIDTH, COUNTER_HEIGHT);
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (item != null) {
            int itemX = x + ITEM_LEFT_MARGIN;
            int itemY = y + (COUNTER_HEIGHT - ITEM_ICON_SIZE) / 2 + ITEM_VERTICAL_OFFSET;
            ItemStack stack = new ItemStack(item, 1);
            context.renderItem(stack, itemX, itemY);

            Minecraft client = hudRenderer.getClient();
            String countText = String.valueOf(itemCount);
            int textX = calculateScaledRightAlignedPosition(client, countText, x);
            int textY = y + (COUNTER_HEIGHT - 8) / 2 + 3;
            drawScaledTextWithCustomShadow(context, countText, textX, textY, TEXT_SCALE, TEXT_COLOR, SHADOW_COLOR, ENABLE_SHADOW);
        } else {
            Minecraft client = hudRenderer.getClient();
            String text = "?";
            int textWidth = client.font.width(text);
            int textX = x + (COUNTER_WIDTH - textWidth) / 2;
            int textY = y + (COUNTER_HEIGHT - 8) / 2;
            drawScaledTextWithCustomShadow(context, text, textX, textY, TEXT_SCALE, TEXT_COLOR, SHADOW_COLOR, ENABLE_SHADOW);
        }
    }

    private int calculateScaledRightAlignedPosition(Minecraft client, String text, int counterX) {
        int originalWidth = client.font.width(text);
        float scaledWidth = originalWidth * TEXT_SCALE;
        int targetRightEdge = counterX + COUNTER_WIDTH - TEXT_RIGHT_MARGIN;
        int calculatedX = (int) (targetRightEdge - scaledWidth);
        return calculatedX - SCALE_COMPENSATION;
    }

    private void drawScaledTextWithCustomShadow(GuiGraphics context, String text, int x, int y,
                                                float scale, int textColor, int shadowColor, boolean enableShadow) {
        Minecraft client = hudRenderer.getClient();
        context.pose().pushPose();
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.pose().translate(x, y, 0);
        context.pose().scale(scale, scale, 1.0f);

        if (enableShadow) {
            int opaqueShadowColor = shadowColor | 0xFF000000;
            context.drawString(client.font, text, 1, 1, opaqueShadowColor, false);
            int opaqueTextColor = textColor | 0xFF000000;
            context.drawString(client.font, text, 0, 0, opaqueTextColor, false);
        } else {
            int opaqueTextColor = textColor | 0xFF000000;
            context.drawString(client.font, text, 0, 0, opaqueTextColor, false);
        }
        context.pose().popPose();
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void update() {
        String configItemId = StardewHUD.getConfig().counterItemId;
        if (!configItemId.equals(this.itemId)) {
            this.itemId = configItemId;
        }
        parseItemId();
        snapshotItems();
    }

    private void snapshotItems() {
        Minecraft mc = hudRenderer.getClient();
        if (mc.player == null || item == null) return;

        long now = mc.player.tickCount;
        if (lastSnapTick == (int) now) return;
        lastSnapTick = (int) now;

        itemCount = 0;
        String curId = BuiltInRegistries.ITEM.getKey(item).toString();

        if (curId.equals(KUBEJS_COIN_ID)) {
            int physical = countItemInInventory(KUBEJS_COIN_ID);
            int electronic = (int) getSDMMoneyReflect(mc.player);
            itemCount = physical + electronic;
            StardewHUD.LOGGER.debug("[CoinHUD] 实物:{} + 电子:{} = 总计:{}", physical, electronic, itemCount);
            return;
        }

        // 普通物品统计
        countItemsInInventorySlots(mc.player.getInventory().items);
        countItemsInInventorySlots(mc.player.getInventory().offhand);
        countItemsInInventorySlots(mc.player.getInventory().armor);
    }

    private long getSDMMoneyReflect(Player player) {
        try {
            Class<?> clazz = Class.forName("net.sixik.sdmshoprework.SDMShopR");
            java.lang.reflect.Method m = clazz.getMethod("getMoney", Player.class);
            return (Long) m.invoke(null, player);
        } catch (Exception e) {
            StardewHUD.LOGGER.debug("[CoinHUD] SDMShop 未安装或方法变动，返回 0: {}", e.toString());
        }
        return 0L;
    }

    private int countItemInInventory(String targetId) {
        int sum = 0;
        sum += countItemIn(hudRenderer.getClient().player.getInventory().items, targetId);
        sum += countItemIn(hudRenderer.getClient().player.getInventory().offhand, targetId);
        sum += countItemIn(hudRenderer.getClient().player.getInventory().armor, targetId);
        return sum;
    }

    private int countItemIn(Iterable<ItemStack> slots, String targetId) {
        int sum = 0;
        for (ItemStack s : slots) {
            if (s.isEmpty()) continue;
            if (targetId.equals(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) {
                sum += s.getCount();
            }
        }
        return sum;
    }

    private void countItemsInInventorySlots(Iterable<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                itemCount += stack.getCount();
            }
        }
    }

    private void parseItemId() {
        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                item = BuiltInRegistries.ITEM.get(id);
            } else {
                item = null;
                StardewHUD.LOGGER.warn("物品ID无效: {}", itemId);
            }
        } catch (Exception e) {
            item = null;
            StardewHUD.LOGGER.error("解析物品ID时出错: {}", itemId, e);
        }
    }

    public void setItemId(String newItemId) {
        StardewHUD.getConfig().counterItemId = newItemId;
        this.itemId = newItemId;
        parseItemId();
        markInventoryChanged();
    }
}