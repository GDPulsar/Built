package io.github.gdpulsar.built;

import dev.architectury.event.events.common.LifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Built {
    public static final String MOD_ID = "built";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(MultiblockRegistry::load);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(MultiblockRegistry::save);
    }
}
