package net.numericly.superprinter.utils;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.player.PlayerUtils.squaredDistanceTo;

public class Utils {

    public static Comparator<BlockPos> NEAREST = Comparator.comparingDouble(pos ->
        squaredDistanceTo(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5)
    );

    public static void setSneaking(boolean sneaking) {
        assert mc.player != null;

        PlayerInput input = mc.player.input.playerInput;

        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
            new PlayerInput(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.jump(),
                sneaking,
                input.sprint()
            )
        ));
    }

    public static boolean isWithinBlockInteractionRange(BlockPos pos) {
        assert mc.player != null;

        return isWithinBlockInteractionRange(mc.player.getEyePos(), pos);
    }

    public static boolean isWithinBlockInteractionRange(Vec3d eyePos, BlockPos pos) {
        double d = 4.5 + 1.0;
        return new Box(pos).squaredMagnitude(eyePos) < d * d;
    }

    public static boolean isValid(World world, Vec3d pos) {
        EntityDimensions hitbox = mc.player.getDimensions(mc.player.getPose());

        return isValid(world, pos, hitbox);
    }

    public static boolean isValid(World world, Vec3d pos, EntityDimensions hitbox) {
        Box box = hitbox.getBoxAt(pos);


        if (!world.isSpaceEmpty(mc.player, box)) {
            return false;
        }

        return true;
    }

    @Nullable
    public static Vec3d findSafeSpotForPlacement(BlockPos location) {
        assert mc.player != null;
        assert mc.world != null;

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null) return null;

        int maxRange = 6;

        List<Vec3d> toCheck = new ArrayList<>();

        for (int x = -maxRange; x <= maxRange; x++) {
            for (int y = -maxRange; y <= maxRange; y++) {
                for (int z = -maxRange; z <= maxRange; z++) {
                    int magnitudeSquared = x*x + y*y + z*z;

                    if (magnitudeSquared <= maxRange * maxRange) {
                        toCheck.add(new Vec3d(x, y, z));
                    }
                }
            }
        }

        toCheck.sort(Comparator.comparingDouble(Vec3d::lengthSquared));
        toCheck.replaceAll(offset -> location.toBottomCenterPos().add(offset));

        EntityDimensions hitbox = mc.player.getDimensions(mc.player.getPose());

        Vec3d eyeOffset = new Vec3d(0, mc.player.getStandingEyeHeight(), 0);

        for (Vec3d pos : toCheck) {
            if (!Utils.isWithinBlockInteractionRange(pos.add(eyeOffset), location)) {
                continue;
            }

            if (!Utils.isValid(mc.world, pos, hitbox)) {
                continue;
            }

            if (!Utils.isValid(worldSchematic, pos, hitbox)) {
                continue;
            }

            return pos;
        }

        return null;
    }
}
