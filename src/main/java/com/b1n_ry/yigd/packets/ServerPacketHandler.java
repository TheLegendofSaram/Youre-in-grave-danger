package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerPacketHandler {
    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_RESTORE_C2S, (server, player, handler, buf, responseSender) -> {
            if (!Permissions.check(player, "yigd.command.restore")) {
                player.sendMessage(Text.translatable("yigd.command.permission_fail"));
                return;
            }

            UUID graveId = buf.readUuid();

            Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
            maybeComponent.ifPresentOrElse(component -> {
                GameProfile owner = component.getOwner();
                ServerPlayerEntity restoringPlayer = server.getPlayerManager().getPlayer(owner.getId());
                if (restoringPlayer == null) {
                    player.sendMessage(Text.translatable("yigd.command.restore.fail.offline_player"));
                    return;
                }

                component.applyToPlayer(restoringPlayer, restoringPlayer.getServerWorld(), restoringPlayer.getBlockPos(), true);
                player.sendMessage(Text.translatable("yigd.command.restore.success"));
            }, () -> player.sendMessage(Text.translatable("yigd.command.restore.fail")));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_ROBBING_C2S, (server, player, handler, buf, responseSender) -> {
            if (!Permissions.check(player, "yigd.command.rob")) {
                player.sendMessage(Text.translatable("yigd.command.permission_fail"));
                return;
            }

            UUID graveId = buf.readUuid();

            Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
            maybeComponent.ifPresentOrElse(component -> {
                component.applyToPlayer(player, player.getServerWorld(), player.getBlockPos(), false);
                player.sendMessage(Text.translatable("yigd.command.rob.success"));
            }, () -> player.sendMessage(Text.translatable("yigd.command.rob.fail")));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_DELETE_C2S, (server, player, handler, buf, responseSender) -> {
            if (!Permissions.check(player, "yigd.command.delete")) {
                player.sendMessage(Text.translatable("yigd.command.permission_fail"));
                return;
            }

            UUID graveId = buf.readUuid();

            ActionResult deleted = DeathInfoManager.INSTANCE.delete(graveId);

            String translatable = switch (deleted) {
                case SUCCESS -> "yigd.command.delete.success";
                case PASS -> "yigd.command.delete.pass";
                case FAIL -> "yigd.command.delete.fail";
                default -> "If you see this, congratulations. You've broken YIGD";
            };
            player.sendMessage(Text.translatable(translatable));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_LOCKING_C2S, (server, player, handler, buf, responseSender) -> {
            if (!Permissions.check(player, "yigd.command.lock")) {
                player.sendMessage(Text.translatable("yigd.command.permission_fail"));
                return;
            }

            UUID graveId = buf.readUuid();
            boolean lockState = buf.readBoolean();

            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
            component.ifPresentOrElse(grave -> grave.setLocked(lockState),
                    () -> player.sendMessage(Text.translatable("yigd.command.lock.fail")));
        });
    }

    public static void sendGraveOverviewPacket(ServerPlayerEntity player, GraveComponent component) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(component.toNbt());

        ServerPlayNetworking.send(player, PacketIdentifiers.GRAVE_OVERVIEW_S2C, buf);
    }

    public static void sendGraveSelectionPacket(ServerPlayerEntity player, List<LightGraveData> data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(data.size());

        for (LightGraveData grave : data) {
            buf.writeNbt(grave.toNbt());
        }

        ServerPlayNetworking.send(player, PacketIdentifiers.GRAVE_SELECTION_S2C, buf);
    }

    public static void sendPlayerSelectionPacket(ServerPlayerEntity player, List<LightPlayerData> data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(data.size());

        for (LightPlayerData playerData : data) {
            buf.writeNbt(playerData.toNbt());
        }

        ServerPlayNetworking.send(player, PacketIdentifiers.PLAYER_SELECTION_S2C, buf);
    }
}
