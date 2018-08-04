package com.tterrag.moonball;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageDangerZone implements IMessage {

    private int thrower;

    public MessageDangerZone() {}

    public MessageDangerZone(EntityPlayer thrower) {
        this.thrower = thrower.getEntityId();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(thrower);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.thrower = buf.readInt();
    }

    public static class Handler implements IMessageHandler<MessageDangerZone, IMessage> {

        @Override
        public IMessage onMessage(MessageDangerZone message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.getClientHandler()).addScheduledTask(() -> callOut(message.thrower));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void callOut(int thrower) {
            Entity throwerEntity = Minecraft.getMinecraft().world.getEntityByID(thrower);
            if (throwerEntity != null && throwerEntity != Minecraft.getMinecraft().player) {
                Minecraft.getMinecraft().ingameGUI
                        .setOverlayMessage(TextFormatting.RED.toString() + TextFormatting.ITALIC + TextFormatting.BOLD + I18n.format("moonball.hud.dangerzone", throwerEntity.getName()), false);
            }
        }
    }

}
