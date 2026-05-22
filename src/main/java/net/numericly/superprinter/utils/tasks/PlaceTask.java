package net.numericly.superprinter.utils.tasks;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.numericly.superprinter.utils.InventoryManager;
import net.numericly.superprinter.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlaceTask extends Task {

    ItemPlacementContext context;

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

        Hand hand = Hand.MAIN_HAND;

        for (Direction direction : Direction.values()) {
            BlockHitResult blockHitResult = new BlockHitResult(location.toCenterPos(), direction, location, false);
            ItemPlacementContext context = new PlaceContextNoRotation(mc.world, hand, itemStack, blockHitResult);

            if (validate(current, required, blockItem.getBlock(), context)) {
                return new PlaceTask(location, current, required, context, blockItem);
            }
        }

        for (Direction direction : Direction.values()) {
            for (float yaw : new float[] { 0, 90, 180, 270 }) {
                for (float pitch : new float[] { 0, 90, -90 }) {
                    if (pitch != 0 && direction.getAxis() != Direction.Axis.Y) continue;

                    BlockHitResult blockHitResult = new BlockHitResult(location.toCenterPos(), direction, location, false);
                    ItemPlacementContext context = new PlaceContextRotation(mc.world, yaw, pitch, hand, itemStack, blockHitResult);

                    if (validate(current, required, blockItem.getBlock(), context)) {
                        return new PlaceTask(location, current, required, context, blockItem);
                    }
                }
            }
        }

        return null;
    }

    PlaceTask(BlockPos location, BlockState current, BlockState required, ItemPlacementContext context, BlockItem item) {
        super(location, current, required);
        this.context = context;
        this.item = item;
    }

    public static boolean validate(BlockState current, BlockState required, Block block, ItemPlacementContext context) {
        assert mc.world != null;

        if (!required.canPlaceAt(mc.world, context.getBlockPos())) {
            return false;
        }

        if (required.getBlock() instanceof FallingBlock &&
            FallingBlock.canFallThrough(mc.world.getBlockState(context.getBlockPos().down())) && context.getBlockPos().getY() >= mc.world.getBottomY()) {
            return false;
        }

        try {
            BlockState newState = block.getPlacementState(context);

            return validateNewState(current, required, newState);
        } catch (RuntimeException e) {
            return false;
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

        // HALF - Clicked face TOP and BOTTOM
        // AXIS - Clicked face only
        // FACING - Clicked face and getNearestLookingDirection()
        // FACING_HOPPER - Clicked face only
        // HORIZONTAL_FACING - getHorizontalDirection()


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

        BlockHitResult hitResult = new BlockHitResult(context.getHitPos(), context.getSide(), location, false);

        if (context instanceof PlaceContextRotation contextRotation) {
            placeBlock(hitResult, Hand.MAIN_HAND, contextRotation.getLookYaw(), contextRotation.getLookPitch());
        } else {
            placeBlock(hitResult, Hand.MAIN_HAND);
        }

        return true;
    }

    public void placeBlock(BlockHitResult hitResult, Hand hand, float lookYaw, float lookPitch) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(lookYaw, lookPitch, false, false));

        placeBlock(hitResult, hand);
    }

    public void placeBlock(BlockHitResult hitResult, Hand hand) {
        assert mc.player != null;
        assert mc.interactionManager != null;

        Utils.setSneaking(context.shouldCancelInteraction());

        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
    }

    public BlockItem getItem() {
        return item;
    }
}
