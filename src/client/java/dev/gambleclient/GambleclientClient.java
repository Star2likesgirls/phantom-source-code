package dev.gambleclient;

import net.fabricmc.api.ClientModInitializer;

public class GambleclientClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		new Gamble();
	}
}