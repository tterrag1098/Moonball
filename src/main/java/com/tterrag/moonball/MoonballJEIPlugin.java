package com.tterrag.moonball;

import javax.annotation.ParametersAreNonnullByDefault;

import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.item.ItemStack;

@JEIPlugin
@ParametersAreNonnullByDefault
public class MoonballJEIPlugin extends BlankModPlugin {

    @Override
    public void register(IModRegistry registry) {
        registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(new ItemStack(Moonball.MYSTERY_ITEM));
    }
}
