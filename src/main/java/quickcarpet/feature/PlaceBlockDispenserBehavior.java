package quickcarpet.feature;

import net.minecraft.block.*;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import quickcarpet.settings.Settings;
import quickcarpet.utils.CarpetRegistry;

import java.util.Collection;

public class PlaceBlockDispenserBehavior  extends ItemDispenserBehavior {
    @Override
    public ItemStack dispenseStack(BlockPointer blockPointer, ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (Settings.dispensersPlaceBlocks == Option.FALSE || !(item instanceof BlockItem)) {
            return super.dispenseStack(blockPointer, itemStack);
        }
        Block block = ((BlockItem) item).getBlock();

        Direction facing = blockPointer.getBlockState().get(DispenserBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        World world = blockPointer.getWorld();
        BlockPos pos = blockPointer.getBlockPos();

        final Direction ffacing = facing;

        if (item.getClass() != BlockItem.class || true) {
            BlockHitResult hitResult = new BlockHitResult(new Vec3d(pos.offset(facing, 2)), facing, pos, false);
            ItemPlacementContext ipc = new ItemPlacementContext(world, null, Hand.MAIN_HAND, itemStack, hitResult) {
                @Override
                public Direction getPlayerFacing() {
                    return ffacing;
                }

                @Override
                public Direction getPlayerHorizontalFacing() {
                    return ffacing.getAxis() == Direction.Axis.Y ? Direction.NORTH : ffacing;
                }

                @Override
                public Direction[] getPlacementFacings() {
                    return new Direction[] {getPlayerFacing(), Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                }
            };
            if (((BlockItem) item).place(ipc) == ActionResult.SUCCESS) {
                return itemStack;
            } else {
                return super.dispenseStack(blockPointer, itemStack);
            }
        }

        pos = pos.offset(facing);

        BlockState state = block.getDefaultState();
        if (state == null) return super.dispenseStack(blockPointer, itemStack);
        Collection<Property<?>> properties = state.getProperties();

        if (block instanceof StairsBlock) {
            facing = facing.getOpposite();
        }

        if (properties.contains(Properties.FACING)) {
            state = state.with(Properties.FACING, facing);
        } else if (properties.contains(Properties.FACING_HORIZONTAL) && axis != Direction.Axis.Y) {
            state = state.with(Properties.FACING_HORIZONTAL, facing);
        } else if (properties.contains(Properties.HOPPER_FACING) && axis != Direction.Axis.Y) {
            state = state.with(Properties.HOPPER_FACING, facing);
        } else if (properties.contains(Properties.AXIS_XYZ)) {
            state = state.with(Properties.AXIS_XYZ, axis);
        } else if (properties.contains(Properties.AXIS_XZ)  && axis != Direction.Axis.Y) {
            state = state.with(Properties.AXIS_XZ, axis);
        }

        if (properties.contains(Properties.BLOCK_HALF)) {
            state = state.with(Properties.BLOCK_HALF, facing == Direction.UP ? BlockHalf.TOP : BlockHalf.BOTTOM);
        } else if (properties.contains(Properties.SLAB_TYPE)) {
            state = state.with(Properties.SLAB_TYPE, facing == Direction.DOWN ? SlabType.TOP : SlabType.BOTTOM);
        }

        state = Block.getRenderingState(state, world, pos);

        BlockState currentBlockState = world.getBlockState(pos);
        FluidState currentFluidState = world.getFluidState(pos);
        if ((world.isAir(pos) || currentBlockState.getMaterial().isReplaceable()) && currentBlockState.getBlock() != block && state.canPlaceAt(world, pos)) {
            world.setBlockState(pos, state);
            if (currentFluidState.isStill() && block instanceof FluidFillable) {
                ((FluidFillable) block).tryFillWithFluid(world, pos, state, currentFluidState);
            }
            BlockSoundGroup soundType = state.getSoundGroup();
            world.playSound(null, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F / 2.0F), soundType.getPitch() * 0.8F);
            itemStack.subtractAmount(1);
            return itemStack;
        }

        return super.dispenseStack(blockPointer, itemStack);
    }

    public enum Option {
        FALSE, WHITELIST, BLACKLIST, ALL
    }

    public static boolean canPlace(Block block) {
        switch (Settings.dispensersPlaceBlocks) {
            case WHITELIST: return CarpetRegistry.DISPENSER_BLOCK_WHITELIST.contains(block);
            case BLACKLIST: return !CarpetRegistry.DISPENSER_BLOCK_BLACKLIST.contains(block);
            case ALL: return true;
        }
        return false;
    }
}
