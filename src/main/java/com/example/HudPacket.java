package com.example;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * What the server tells the screen.
 *
 * Everything the heads-up display shows -- where the quest is, how far, the
 * date, your money -- lives on the server, and the client has no way of
 * knowing any of it. So it is sent, four times a second, as one line of text
 * with the fields separated by bars.
 *
 * One string rather than eleven fields is a deliberate trade: the packet is a
 * few bytes bigger and the code is a tenth of the size, and at four packets a
 * second the size does not matter.
 */
public record HudPacket(String line) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HudPacket> TYPE =
			new CustomPacketPayload.Type<>(ShipLifeMod.id("hud"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HudPacket> CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, HudPacket::line, HudPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
