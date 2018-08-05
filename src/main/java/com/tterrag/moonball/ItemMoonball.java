package com.tterrag.moonball;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
public class ItemMoonball extends Item {
    
    private static final int FLAVOR_COUNT = 6;

    public ItemMoonball() {
        this.setCreativeTab(CreativeTabs.MISC);
        this.setHasSubtypes(true);
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> items) {
        for (int i = 0; i < EnumDyeColor.values().length; i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + "." + EnumDyeColor.values()[MathHelper.clamp(stack.getMetadata(), 0, 15)].getName();
    }
    
    private static final UUID uuid = UUID.fromString("659f26f3-93b1-4cd4-a0b6-3391b1d4ae74");
    private static final Random flavorRand = new Random();

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean debug) {
        super.addInformation(stack, player, tooltip, debug);
        flavorRand.setSeed(stack.hashCode());
        String suffix = String.valueOf(flavorRand.nextInt(FLAVOR_COUNT));
        if (flavorRand.nextInt(250) == 0 || (Minecraft.getMinecraft().getSession().getProfile().getId().equals(uuid) && flavorRand.nextBoolean())) {
            suffix = "rare";
        }
        tooltip.add(TextFormatting.ITALIC + I18n.format(getUnlocalizedName() + ".flavortext." + suffix));
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

        if (!worldIn.isRemote && entityIn instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityIn;
            ItemStack active = player.getActiveItemStack();
            if (active == stack && player.getItemInUseMaxCount() == 10) {
                Moonball.NETWORK.sendToAllAround(new MessageDangerZone(player), new TargetPoint(worldIn.provider.getDimension(), player.posX, player.posY, player.posZ, 32));
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {
        super.onPlayerStoppedUsing(stack, worldIn, entityLiving, timeLeft);

        int charge = getMaxItemUseDuration(stack) - timeLeft;
        float power = (float) charge / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;

        if (power > 1.0F) {
            power = 1.0F;
        }

        EntityPlayer player = entityLiving instanceof EntityPlayer ? (EntityPlayer) entityLiving : null;

        if (player != null && !player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }

        worldIn.playSound((EntityPlayer) null, entityLiving.posX, entityLiving.posY, entityLiving.posZ, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F,
                0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        if (!worldIn.isRemote) {
            EntityMoonball munbah = new EntityMoonball(worldIn, entityLiving, stack.getItemDamage());
            munbah.setHeadingFromThrower(entityLiving, entityLiving.rotationPitch, entityLiving.rotationYaw, 0.0F, 1.25F * power, 0.5F);
            worldIn.spawnEntity(munbah);
        }

        if (player != null) {
            player.addStat(StatList.getObjectUseStats(this));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        playerIn.setActiveHand(handIn);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
    }
}
