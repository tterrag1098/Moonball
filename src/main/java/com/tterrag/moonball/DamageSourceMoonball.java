package com.tterrag.moonball;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSourceIndirect;

public class DamageSourceMoonball extends EntityDamageSourceIndirect {

    public DamageSourceMoonball(Entity source, Entity indirectEntityIn) {
        super("moonball", source, indirectEntityIn);
    }

}
