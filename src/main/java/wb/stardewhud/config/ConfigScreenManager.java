package wb.stardewhud.config;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ConfigScreenManager {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (client, parent) -> new ModConfigScreen(parent)
        );
    }
}