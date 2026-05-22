package net.numericly.superprinter.utils.tasks;

import net.numericly.superprinter.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InteractTask extends Task {

    BlockHitResult hitResult;

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

        return null;
    }

    InteractTask(BlockPos location, BlockState current, BlockState required, BlockHitResult hitResult) {
        super(location, current, required);
        this.hitResult = hitResult;
    }

    @Override
    public boolean execute() {
        assert mc.interactionManager != null;
        assert mc.player != null;
        assert mc.world != null;

        if (mc.world.getBlockState(location) != current) return false;

        if (!Utils.isWithinBlockInteractionRange(location)) return false;

        Utils.setSneaking(false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        return true;
    }
}
