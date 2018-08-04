package com.tterrag.moonball;

import static com.tterrag.moonball.Moonball.MOD_ID;
import static com.tterrag.moonball.Moonball.MOD_NAME;
import static com.tterrag.moonball.Moonball.VERSION;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.BlockDispenser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = MOD_ID, name = MOD_NAME, version = VERSION)
@EventBusSubscriber
@ParametersAreNonnullByDefault
public class Moonball {

    public static final @Nonnull String MOD_ID = "moonball";
    public static final @Nonnull String MOD_NAME = "Moonball";
    public static final @Nonnull String VERSION = "@VERSION@";

    @Instance
    public static Moonball instance;

    public static final SimpleNetworkWrapper NETWORK = new SimpleNetworkWrapper("moonball");

    @SuppressWarnings("null")
    @ObjectHolder("moonball")
    public static final @Nonnull ItemMoonball MOONBALL_ITEM = null;

    @SuppressWarnings("null")
    @ObjectHolder("mystery")
    public static final @Nonnull Item MYSTERY_ITEM = null;

    @SubscribeEvent
    public static void registerItems(Register<Item> event) {
        event.getRegistry().register(new ItemMoonball().setRegistryName("moonball").setUnlocalizedName("moonball.moonball"));
        event.getRegistry().register(new ItemFood(1, 0.1f, false) {

            @Override
            protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
                if (!worldIn.isRemote) {
                    player.addPotionEffect(new PotionEffect(ForgeRegistries.POTIONS.getValue(new ResourceLocation("nausea")), 30 * 20, 1));
                }
            }

            @Override
            public EnumAction getItemUseAction(ItemStack stack) {
                return EnumAction.DRINK;
            }
        }.setAlwaysEdible().setRegistryName("mystery").setUnlocalizedName("moonball.mystery"));
    }

    @SubscribeEvent
    public static void registerEntities(Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityMoonball.class)
                .id(new ResourceLocation(MOD_ID, "moonball"), 0)
                .name(MOD_ID + ".moonball")
                .tracker(128, 32, true)
                .build());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        for (int i = 0; i < EnumDyeColor.values().length; i++) {
            ModelLoader.setCustomModelResourceLocation(MOONBALL_ITEM, i, new ModelResourceLocation(new ResourceLocation(MOD_ID, "moonball"), "inventory"));
        }
        ModelLoader.setCustomModelResourceLocation(MYSTERY_ITEM, 0, new ModelResourceLocation(new ResourceLocation(MOD_ID, "mystery"), "inventory"));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            registerEntityRenderers();
        }

        NETWORK.registerMessage(MessageDangerZone.Handler.class, MessageDangerZone.class, 0, Side.CLIENT);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            registerItemColors();
        }
        
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(MOONBALL_ITEM, new BehaviorProjectileDispense() {

            @Override
            protected IProjectile getProjectileEntity(World worldIn, IPosition position, ItemStack stackIn) {
                return new EntityMoonball(worldIn, position.getX(), position.getY(), position.getZ(), stackIn.getMetadata());
            }
            
            @Override
            protected float getProjectileVelocity() {
                return 1.25F;
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityMoonball.class, manager -> new RenderSnowball<EntityMoonball>(manager, MOONBALL_ITEM, Minecraft.getMinecraft().getRenderItem()) {

            @Override
            public ItemStack getStackToRender(EntityMoonball entity) {
                return new ItemStack(item, 1, entity.colorMeta);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private void registerItemColors() {
        Minecraft.getMinecraft().getItemColors()
                .registerItemColorHandler((stack, index) -> index == 0 ? EnumDyeColor.values()[MathHelper.clamp(stack.getItemDamage(), 0, 15)].getColorValue() : 0xFFFFFFF, MOONBALL_ITEM);
    }

    private static final UUID uuid = UUID.fromString("97a6f46a-b028-4b00-83df-fec9af2d7567");

    @SubscribeEvent
    public static void onPlayerJoin(PlayerLoggedInEvent event) {
        if (!event.player.getEntityData().getBoolean("moonball.fed") && event.player.getGameProfile().getId().equals(uuid)) {
            ItemStack stack = new ItemStack(MYSTERY_ITEM, 4);
            if (!event.player.addItemStackToInventory(stack)) {
                event.player.dropItem(stack, false);
            }
            event.player.getEntityData().setBoolean("moonball.fed", true);
        }
    }
}
