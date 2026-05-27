package net.numericly.superprinter.utils;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.numericly.superprinter.SuperPrinter;
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

    public static boolean isWithinBlockInteractionRange(BlockPos pos) {
        assert mc.player != null;

        return isWithinBlockInteractionRange(mc.player.getEyePos(), pos);
    }

    public static boolean isWithinBlockInteractionRange(Vec3d eyePos, BlockPos pos) {
        double d = mc.player.getBlockInteractionRange() + 1.0;
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

    public static void interactBlock(BlockHitResult hitResult, Hand hand) {
        assert mc.player != null;
        assert mc.interactionManager != null;

        mc.interactionManager.interactBlock(mc.player, hand, hitResult);

    }

    public static void interactBlock(BlockHitResult hitResult, Hand hand, float pitch, float yaw, boolean sneaking) {
        assert mc.player != null;
        assert mc.interactionManager != null;

        float oldPitch = mc.player.getPitch();
        float oldLastPitch = mc.player.lastPitch;
        float oldYaw = mc.player.getYaw();
        float oldLastYaw = mc.player.lastYaw;
        float oldHeadYaw = mc.player.getHeadYaw();
        float oldLastHeadYaw = mc.player.lastHeadYaw;
        boolean oldSneaking = mc.player.isSneaking();

        mc.player.setPitch(pitch);
        mc.player.lastPitch = pitch;
        mc.player.setYaw(yaw);
        mc.player.lastYaw = yaw;
        mc.player.setHeadYaw(yaw);
        mc.player.lastHeadYaw = yaw;
        mc.player.setSneaking(sneaking);

        mc.interactionManager.interactBlock(mc.player, hand, hitResult);

        mc.player.setPitch(oldPitch);
        mc.player.lastPitch = oldLastPitch;
        mc.player.setYaw(oldYaw);
        mc.player.lastYaw = oldLastYaw;
        mc.player.setHeadYaw(oldHeadYaw);
        mc.player.lastHeadYaw = oldLastHeadYaw;
        mc.player.setSneaking(oldSneaking);

    }

    public static void rotate(float lookYaw, float lookPitch) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(lookYaw, lookPitch, false, false));
    }

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
}
