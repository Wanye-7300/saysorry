package com.outlook.chensiyuan_2009;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

public class ApologyManager {
    // 存储需要道歉的玩家和他们的计时器
    private static final Map<UUID, ScheduledFuture<?>> pendingApologies = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 添加需要道歉的玩家
    public static void addPlayer(ServerPlayer player) {
        // 如果玩家已在列表中，先取消之前的计时器
        removePlayer(player);

        // 创建踢出任务
        Runnable kickTask = () -> {
            if (player.isAlive() && !player.hasDisconnected()) {
                // 先广播踢出消息
                Component message = Component.literal(player.getDisplayName().getString() +
                        " has been kicked from the game for failing to apologize within the required time.").withStyle(ChatFormatting.YELLOW);
                Objects.requireNonNull(player.getServer()).getPlayerList().broadcastSystemMessage(message, false);
                // 再踢出玩家
                player.connection.disconnect(Component.literal("You have been kicked from the game for failing to apologize within the required time."));
            }
            pendingApologies.remove(player.getUUID());
        };

        // 安排 5 分钟后执行踢出
        ScheduledFuture<?> future = scheduler.schedule(kickTask, 5, TimeUnit.MINUTES);
        pendingApologies.put(player.getUUID(), future);
    }

    // 处理玩家道歉
    public static boolean handleApology(ServerPlayer player) {
        if (pendingApologies.containsKey(player.getUUID())) {
            removePlayer(player);
            // 全服广播
            Component message = Component.literal(player.getDisplayName().getString() +
                    " has apologized successfully.").withStyle(ChatFormatting.YELLOW);
            Objects.requireNonNull(player.getServer()).getPlayerList().broadcastSystemMessage(message, false);
            return true;
        }
        return false;
    }

    // 移除玩家（登出时使用）
    public static void removePlayer(ServerPlayer player) {
        ScheduledFuture<?> future = pendingApologies.remove(player.getUUID());
        if (future != null) {
            future.cancel(false);
        }
    }
}
