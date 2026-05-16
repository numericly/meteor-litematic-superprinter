package net.numericly.superprinter.utils.tasks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Tasks {
    static long MAX_TIME = 100;

    static Map<net.minecraft.util.math.BlockPos, Long> taskUpdatedMap = new HashMap<>();
    static Map<BlockPos, Task> taskMap = new HashMap<>();

    @Nullable
    public static Task getOrCreateTask(BlockState required, BlockState current, BlockPos location) {
        long timeSinceUpdate = System.currentTimeMillis() - taskUpdatedMap.getOrDefault(location, 0L);

        if (timeSinceUpdate < 0) {
            Task task = taskMap.get(location);
            if (task == null || task.current == current) {
                return task;
            }
        }

        Task newTask = createTask(required, current, location);

        taskUpdatedMap.put(location, System.currentTimeMillis());

        if (newTask != null) {
            return taskMap.put(location, newTask);
        } else {
            taskMap.remove(location);
            return null;
        }
    }

    @Nullable
    private static Task createTask(BlockState required, BlockState current, BlockPos location) {
        assert mc.world != null;

        // Already complete
        if (required == current) {
            return null;
        }

        // Wrong block
        if (wrongBlock(required, current)) {
            return null;
        }

        PlaceTask placeTask = PlaceTask.tryCreate(location, current, required);

        if (placeTask != null) {
            return placeTask;
        }

        InteractTask interactTask = InteractTask.tryCreate(location, current, required);

        if (interactTask != null) {
            return interactTask;
        }

        return null;
    }

    private static boolean wrongBlock(BlockState required, BlockState current) {
        if (current.isReplaceable()) {
            return false;
        }

        if (current.getBlock() == Blocks.DIRT && required.getBlock() == Blocks.GRASS_BLOCK ||
            current.getBlock() == Blocks.GRASS_BLOCK && required.getBlock() == Blocks.DIRT) {
            return false;
        }

        return required.getBlock() != current.getBlock();
    }
}
