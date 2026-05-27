package net.numericly.superprinter.utils.tasks;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.numericly.superprinter.utils.Utils;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static net.minecraft.block.RedstoneWireBlock.*;

public class InteractTask extends Task {

    BlockHitResult hitResult;
    @Nullable
    Vec2f rotation;

    @Nullable
    static InteractTask tryCreate(BlockPos location, BlockState current, BlockState required) {
        if (current.getBlock() != required.getBlock()) {
            return null;
        }

        if (current.getBlock() instanceof TrapdoorBlock || current.getBlock() instanceof DoorBlock) {
            if (current.get(Properties.OPEN) != required.get(Properties.OPEN)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof FenceGateBlock) {
            if (current.get(Properties.OPEN) != required.get(Properties.OPEN)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);

                float yaw = Direction.getHorizontalDegreesOrThrow(required.get(Properties.HORIZONTAL_FACING));

                return new InteractTask(location, current, required, result, new Vec2f(yaw, 0));
            }
        }

        if (current.getBlock() instanceof ComparatorBlock) {
            if (current.get(Properties.COMPARATOR_MODE) != required.get(Properties.COMPARATOR_MODE)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof RedstoneWireBlock) {
            if ((isFullyConnected(current) && isNotConnected(required)) || (isNotConnected(current) && isFullyConnected(required))) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof CopperGolemStatueBlock) {
            if (current.get(Properties.COPPER_GOLEM_POSE) != required.get(Properties.COPPER_GOLEM_POSE)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof RepeaterBlock) {
            if (current.get(Properties.DELAY).intValue() != required.get(Properties.DELAY).intValue()) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof LeverBlock) {
            if (current.get(Properties.POWERED) != required.get(Properties.POWERED)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        if (current.getBlock() instanceof DaylightDetectorBlock) {
            if (current.get(Properties.INVERTED) != required.get(Properties.INVERTED)) {
                BlockHitResult result = new BlockHitResult(location.toCenterPos(), Direction.NORTH, location, false);
                return new InteractTask(location, current, required, result);
            }
        }

        return null;
    }

    InteractTask(BlockPos location, BlockState current, BlockState required, BlockHitResult hitResult) {
        super(location, current, required);
        this.hitResult = hitResult;
    }

    InteractTask(BlockPos location, BlockState current, BlockState required, BlockHitResult hitResult, @NotNull Vec2f rotation) {
        super(location, current, required);
        this.hitResult = hitResult;
        this.rotation = rotation;
    }

    @Override
    public boolean execute() {
        assert mc.interactionManager != null;
        assert mc.player != null;
        assert mc.world != null;

        if (mc.world.getBlockState(location) != current) return false;

        if (!Utils.isWithinBlockInteractionRange(location)) return false;

        if (rotation != null) {
            Utils.rotate(rotation.x, rotation.y);
        }

        Utils.setSneaking(false);

        Utils.interactBlock(hitResult, Hand.MAIN_HAND);

        return true;
    }
}
