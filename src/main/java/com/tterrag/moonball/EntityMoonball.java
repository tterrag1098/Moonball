package com.tterrag.moonball;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Vector3d;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

@ParametersAreNonnullByDefault
public class EntityMoonball extends EntityThrowable implements IEntityAdditionalSpawnData {

    // Add a 1-tick delay when bouncing so that new heading is considered fully for new collisions
    // This prevents clipping in tight corners
    private boolean bouncing;

    private double newX, newY, newZ;

    // Meta value from the itemstack that threw it
    int colorMeta;

    // As a fallback, limit the total number of bounces (should only be reached if stuck inside a block)
    private int bounceCount;

    public EntityMoonball(World world) {
        super(world);
    }

    public EntityMoonball(World world, EntityLivingBase thrower, int colorMeta) {
        super(world, thrower);
        this.colorMeta = colorMeta;
    }
    
    public EntityMoonball(World world, double x, double y, double z, int colorMeta) {
        super(world, x, y, z);
        this.colorMeta = colorMeta;
    }

    @Override
    public void onUpdate() {
        if (bouncing) {
            bouncing = false;
            this.motionX = newX;
            this.motionY = newY;
            this.motionZ = newZ;
        }

        super.onUpdate();

        if (bounceCount > 25 && !world.isRemote) {
            setDead();
        }

        float damping = 0.98f;
        this.motionX *= damping;
        this.motionY *= damping;
        this.motionZ *= damping;
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        double velSq = this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ;
        if (getEntityWorld().isRemote) {
            getEntityWorld().playSound(this.posX, this.posY, this.posZ, SoundEvents.BLOCK_SLIME_STEP, SoundCategory.HOSTILE, (float) (0.75f * velSq), 0.5f, false);
        }
        if (result.typeOfHit == Type.ENTITY) {
            if (result.entityHit instanceof EntityPlayer && ((EntityPlayer) result.entityHit).capabilities.isCreativeMode) {
                return;
            }
            if (!getEntityWorld().isRemote) {
                result.entityHit.attackEntityFrom(new DamageSourceMoonball(this.getThrower(), this), (float) (4 * velSq));
                this.setDead();
            }
        } else if (result.typeOfHit == Type.BLOCK) {

            World world = getEntityWorld();
            BlockPos hitLoc = result.getBlockPos();
            if (hitLoc == null) {
                return;
            }
            IBlockState hit = world.getBlockState(hitLoc);
            float hardness = hit.getBlockHardness(world, hitLoc);
            if (hardness >= 0 && hardness < velSq * 0.5) {
                if (!getEntityWorld().isRemote) {
                    world.destroyBlock(hitLoc, true);
                }
                float damping = 1 - hardness;
                this.motionX *= damping;
                this.motionY *= damping;
                this.motionZ *= damping;
            }
            
            if (hit.getCollisionBoundingBox(world, hitLoc) != Block.NULL_AABB) {
                
                // Despawn if going too slow
                if (velSq < 0.1) {
                    if (!getEntityWorld().isRemote) {
                        setDead();
                        return;
                    }
                }
                
                // High angles of incidence (Near 180 degrees) can cause the ball to get stuck in a wall
                // To fix this, I add some "ghost" velocity in the direction normal to the wall
                Vector3d motionVec = new Vector3d(this.motionX, this.motionY, this.motionZ);
                Vec3i dirVec = result.sideHit.getDirectionVec();
                Vector3d bounceVec = new Vector3d(dirVec.getX(), dirVec.getY(), dirVec.getZ());
                motionVec.x *= bounceVec.x;
                motionVec.y *= bounceVec.y;
                motionVec.z *= bounceVec.z;
                // This is a very shallow bounce, so give it some extra "kick"
                if (motionVec.lengthSquared() < 0.001) {
                    motionVec.scale(4);
                    this.motionX += motionVec.x;
                    this.motionY += motionVec.y;
                    this.motionZ += motionVec.z;
                }
    
                bounceCount++;
                bouncing = true;
                this.newX = this.motionX;
                this.newY = this.motionY;
                this.newZ = this.motionZ;
                switch (result.sideHit.getAxis()) {
                case X:
                    this.newX = -this.newX;
                    break;
                case Y:
                    this.newY = -this.newY;
                    break;
                case Z:
                    this.newZ = -this.newZ;
                    break;
                }
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        if (!getEntityWorld().isRemote) {
            getEntityWorld().spawnEntity(new EntityItem(getEntityWorld(), this.posX, this.posY, this.posZ, new ItemStack(Moonball.MOONBALL_ITEM, 1, colorMeta)));
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.03f;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("colorMeta", colorMeta);
        compound.setInteger("bounceCount", bounceCount);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.colorMeta = compound.getInteger("colorMeta");
        this.bounceCount = compound.getInteger("bounceCount");
    }

    @Override
    public void writeSpawnData(@Nullable ByteBuf buffer) {
        if (buffer == null) {
            return;
        }
        buffer.writeInt(colorMeta);
        EntityLivingBase thrower = this.getThrower();
        if (thrower != null) {
            buffer.writeInt(thrower.getEntityId());
        }
    }

    @Override
    public void readSpawnData(@Nullable ByteBuf buffer) {
        if (buffer == null) {
            return;
        }
        this.colorMeta = buffer.readInt();
        if (buffer.isReadable()) {
            this.thrower = (EntityLivingBase) this.getEntityWorld().getEntityByID(buffer.readInt());
        }
    }

}
