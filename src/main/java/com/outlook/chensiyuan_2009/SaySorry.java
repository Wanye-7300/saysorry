package com.outlook.chensiyuan_2009;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(SaySorry.MODID)
public class SaySorry {
    public static final String MODID = "saysorry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SaySorry(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(SaySorry::onPlayerKill);
        NeoForge.EVENT_BUS.addListener(SaySorry::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(SaySorry::onPlayerChat);
    }

    private static void onPlayerKill(LivingDeathEvent event) {
        // 检查死亡实体是否是玩家
        if (!(event.getEntity() instanceof Player victim)) return;

        // 检查伤害来源是否是玩家
        if (!(event.getSource().getEntity() instanceof Player killer)) return;

        // 仅服务端处理
        if (victim.level().isClientSide) return;

        ServerPlayer serverKiller = (ServerPlayer) killer;

        // 发送警告消息
        Component message = Component.literal(killer.getDisplayName().getString()).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" killed ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(victim.getDisplayName().getString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(". Please type 'sorry' in the chat to apologize, or you'll be kicked out of the game in 5 minutes.").withStyle(ChatFormatting.YELLOW));
        Objects.requireNonNull(serverKiller.getServer()).getPlayerList().broadcastSystemMessage(message, false);

        LOGGER.info("{} killed {}", killer.getDisplayName().getString(), victim.getDisplayName().getString());

        ApologyManager.addPlayer(serverKiller);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ApologyManager.removePlayer((ServerPlayer) event.getEntity());
        }
    }

    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString().toLowerCase();
        ServerPlayer player = event.getPlayer();

        if (message.equals("sorry") || message.equals("sor")) {
            // 处理道歉
            boolean handled = ApologyManager.handleApology(player);

            if (handled) {
                // 向玩家发送私人的确认信息
                player.sendSystemMessage(
                        Component.literal("your apology has been accepted.")
                                .withStyle(ChatFormatting.GRAY)
                );
            } else {
                // 不需要道歉
                player.sendSystemMessage(
                        Component.literal("There are no apology requests to handle at the moment.")
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }
}
