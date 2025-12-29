package io.github.gdpulsar.built.neoforge;

import io.github.gdpulsar.built.Built;
import net.neoforged.fml.common.Mod;

@Mod(Built.MOD_ID)
public final class BuiltNeoForge {
    public BuiltNeoForge() {
        // Run our common setup.
        Built.init();
    }
}
