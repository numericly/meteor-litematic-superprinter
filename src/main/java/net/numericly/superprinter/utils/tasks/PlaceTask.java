package net.numericly.superprinter.utils.tasks;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.SlabType;
import net.numericly.superprinter.modules.ModulePrinter;
import net.minecraft.item.*;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.numericly.superprinter.SuperPrinter;
import net.numericly.superprinter.utils.InventoryManager;
import net.numericly.superprinter.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlaceTask extends Task {

    PlaceContextRotation context;

    BlockItem item;

    @Nullable
    static PlaceTask tryCreate(BlockPos location, BlockState current, BlockState required) {
        if (!current.isReplaceable() && current.getBlock() != required.getBlock()) {
            return null;
        }
        assert mc.player != null;

        ItemStack itemStack;

        if (mc.player.getGameMode() == GameMode.CREATIVE) {
            itemStack = new ItemStack(required.getBlock().asItem(), 1);
        } else {
            FindItemResult result = InvUtils.find(required.getBlock().asItem());

            if(!result.found()) {
                return null;
            }

            itemStack = mc.player.getInventory().getStack(result.slot());
        }

        if (!(itemStack.getItem() instanceof BlockItem blockItem)) return null;

        if (!required.canPlaceAt(mc.world, location)) {
            return null;
        }

        if (required.getBlock() instanceof FallingBlock &&
            FallingBlock.canFallThrough(mc.world.getBlockState(location.down())) &&
            location.getY() >= mc.world.getBottomY()) {
            return null;
        }

        Hand hand = Hand.MAIN_HAND;

        for (float yaw : createYaws(required)) {
            for (float pitch : createPitches(blockItem, required)) {
                for (Vec3d offset : createOffsets(required)) {
                    for (Direction direction : Direction.values()) {
                        BlockHitResult blockHitResult = new BlockHitResult(location.toCenterPos().add(offset), direction, location, false);
                        PlaceContextRotation context = new PlaceContextRotation(mc.world, yaw, pitch, hand, itemStack, blockHitResult);

                        BlockState newState = blockItem.getPlacementState(context);

                        if (newState == null) continue;

                        boolean valid = validateNewState(current, required, newState);

                        if (valid) {
                            return new PlaceTask(location, current, required, context, blockItem);
                        }
                    }
                }
            }
        }

        return null;
    }

    PlaceTask(BlockPos location, BlockState current, BlockState required, PlaceContextRotation context, BlockItem item) {
        super(location, current, required);
        this.context = context;
        this.item = item;
    }

    public static List<Float> createYaws(BlockState state) {
        if (state.contains(Properties.ROTATION)) {
            return List.of(0.0F, 22.5F, 45.0F, 67.5F, 90.0F, 112.5F, 135.0F, 157.5F, 180.0F, 202.5F, 225.0F, 247.5F, 270.0F, 292.5F, 315.0F, 337.5F);
        } else if (state.contains(Properties.HORIZONTAL_FACING) ||
            state.contains(Properties.FACING) ||
            state.contains(Properties.BLOCK_FACE) ||
            state.contains(Properties.ORIENTATION)) {
            return List.of(0F, 90F, 180F, 270F);
        } else {
            return List.of(0F);
        }
    }

    public static List<Float> createPitches(BlockItem item, BlockState state) {
        if (item instanceof VerticallyAttachableBlockItem) {
            if (state.contains(Properties.HORIZONTAL_FACING)) {
                return List.of(0F);
            } else if (state.contains(Properties.ROTATION)) {
                return List.of(90F, -90F);
            }
        }

        if (state.contains(Properties.VERTICAL_DIRECTION) ||
            state.contains(Properties.FACING) ||
            state.contains(Properties.SLAB_TYPE) ||
            state.contains(Properties.BLOCK_HALF) ||
            state.contains(Properties.ORIENTATION) ||
            state.contains(Properties.BLOCK_FACE) ||
            state.contains(Properties.ROTATION) ||
            state.contains(Properties.HANGING) ||
            state.getBlock() instanceof MultifaceGrowthBlock
        ) {
            return List.of(0F, 90F, -90F);
        } else {
            return List.of(0F);
        }
    }

    public static List<Vec3d> createOffsets(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return List.of(
                Vec3d.ZERO,
                new Vec3d(0.5, 0, 0.5),
                new Vec3d(0.5, 0, -0.5),
                new Vec3d(-0.5, 0, 0.5),
                new Vec3d(-0.5, 0, -0.5)
            );
        } else if (state.getBlock() instanceof SnowBlock) {
            return List.of(new Vec3d(0, 0.5, 0));
        } else {
            return List.of(Vec3d.ZERO);
        }
    }

    private static boolean validateNewState(BlockState old, BlockState required, BlockState newState) {
        if (old == newState) {
            return false;
        }

        if (newState.getBlock() != required.getBlock()) {
            return false;
        }

        if (old.getBlock() == newState.getBlock()) {
            if (old.contains(Properties.SLAB_TYPE)) {
                SlabType oldType = old.get(Properties.SLAB_TYPE);
                SlabType placedType = newState.get(Properties.SLAB_TYPE);
                SlabType reqType = required.get(Properties.SLAB_TYPE);

                // If the slab type has changed from top or bottom to double
                return oldType != SlabType.DOUBLE && placedType == reqType;
            }

            if (old.contains(Properties.LAYERS)) {
                int oldCount = old.get(Properties.LAYERS);
                int placedCount = newState.get(Properties.LAYERS);
                int reqCount = required.get(Properties.LAYERS);

                return reqCount > oldCount && placedCount > oldCount;
            }

            if (old.contains(Properties.CANDLES)) {
                int oldCount = old.get(Properties.CANDLES);
                int placedCount = newState.get(Properties.CANDLES);
                int reqCount = required.get(Properties.CANDLES);

                return reqCount > oldCount && placedCount > oldCount;
            }

            if (old.contains(Properties.FLOWER_AMOUNT)) {
                int oldCount = old.get(Properties.FLOWER_AMOUNT);
                int placedCount = newState.get(Properties.FLOWER_AMOUNT);
                int reqCount = required.get(Properties.FLOWER_AMOUNT);

                return reqCount > oldCount && placedCount > oldCount;
            }

            if (old.contains(Properties.SEGMENT_AMOUNT)) {
                int oldCount = old.get(Properties.SEGMENT_AMOUNT);
                int placedCount = newState.get(Properties.SEGMENT_AMOUNT);
                int reqCount = required.get(Properties.SEGMENT_AMOUNT);

                return reqCount > oldCount && placedCount > oldCount;
            }

            return false;
        }

        if (required.contains(Properties.SLAB_TYPE)) {
            SlabType req = required.get(Properties.SLAB_TYPE);
            SlabType placed = newState.get(Properties.SLAB_TYPE);

            if (req != SlabType.DOUBLE && req != placed) {
                return false;
            }
        }

        if (required.contains(Properties.CHEST_TYPE)) {
            ChestType req = required.get(Properties.CHEST_TYPE);
            ChestType placed = newState.get(Properties.CHEST_TYPE);

            if (req != placed && !(req == ChestType.LEFT && placed == ChestType.SINGLE )) {
                return false;
            }
        }

        if (required.getBlock() instanceof MultifaceGrowthBlock) {
            if (newState.get(Properties.UP) && !required.get(Properties.UP)) return false;
            if (newState.get(Properties.DOWN) && !required.get(Properties.DOWN)) return false;
            if (newState.get(Properties.EAST) && !required.get(Properties.EAST)) return false;
            if (newState.get(Properties.NORTH) && !required.get(Properties.NORTH)) return false;
            if (newState.get(Properties.SOUTH) && !required.get(Properties.SOUTH)) return false;
            if (newState.get(Properties.WEST) && !required.get(Properties.WEST)) return false;
        }

        if (propertyMismatch(required, newState, Properties.BLOCK_HALF)) return false;
        if (propertyMismatch(required, newState, Properties.AXIS)) return false;
        if (propertyMismatch(required, newState, Properties.FACING)) return false;
        if (propertyMismatch(required, newState, Properties.HOPPER_FACING)) return false;
        if (propertyMismatch(required, newState, Properties.HORIZONTAL_FACING)) return false;
        if (propertyMismatch(required, newState, Properties.BLOCK_FACE)) return false;
        if (propertyMismatch(required, newState, Properties.BED_PART)) return false;
        if (propertyMismatch(required, newState, Properties.DOUBLE_BLOCK_HALF)) return false;
        if (propertyMismatch(required, newState, Properties.DOOR_HINGE)) return false;
        if (propertyMismatch(required, newState, Properties.ATTACHMENT)) return false;
        if (propertyMismatch(required, newState, Properties.ATTACHED)) return false;
        if (propertyMismatch(required, newState, Properties.HANGING)) return false;
        if (propertyMismatch(required, newState, Properties.ORIENTATION)) return false;
        if (propertyMismatch(required, newState, Properties.VERTICAL_DIRECTION)) return false;
        if (propertyMismatch(required, newState, Properties.ROTATION)) return false;

        return true;
    }

    private static <T extends Comparable<T>> boolean propertyMismatch(BlockState bs1, BlockState bs2, Property<@NotNull T> property) {
        if (bs1.contains(property) || bs2.contains(property)) {
            if (bs1.contains(property) != bs2.contains(property)) {
                return true;
            }
            return bs1.get(property) != bs2.get(property);
        } else {
            return false;
        }
    }


    @Override
    public boolean execute() {
        assert mc.player != null;
        assert mc.world != null;

        if (mc.world.getBlockState(location) != current) return false;

        if (!InventoryManager.switchItem(item)) {
            return false;
        }

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null) {
            return false;
        }

        BlockState state = worldSchematic.getBlockState(location);

        if (!mc.world.canPlace(state, location, ShapeContext.absent())) {
            return false;
        }

        if (context instanceof PlaceContextRotation contextRotation) {
            Utils.rotate(contextRotation.getLookYaw(), contextRotation.getLookPitch());
        }

        Utils.setSneaking(context.shouldCancelInteraction());

        if (required.getBlock() instanceof AbstractSignBlock) {
            ModulePrinter.lastSignPlaceTime = System.currentTimeMillis();
        }

        Utils.interactBlock(context.getHitResult(), Hand.MAIN_HAND, context.getLookPitch(), context.getLookYaw(), context.shouldCancelInteraction());

        return true;
    }

}
