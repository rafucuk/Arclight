package io.izzel.arclight.common.mod;

import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.mixin.injector.EjectorInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public class ArclightConnector implements IMixinConnector {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    @Override
    public void connect() {
        InjectionInfo.register(EjectorInfo.class);
        Mixins.addConfiguration("mixins.arclight.core.json");
        Mixins.addConfiguration("mixins.arclight.bukkit.json");
        Mixins.addConfiguration("mixins.arclight.forge.json");
        LOGGER.info("mixin-load.core");
        Mixins.addConfiguration("mixins.arclight.impl.optimization.json");
        LOGGER.info("mixin-load.optimization");
    }
}
