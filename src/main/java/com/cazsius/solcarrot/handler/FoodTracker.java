package com.cazsius.solcarrot.handler;

import com.cazsius.solcarrot.SOLCarrot;
import com.cazsius.solcarrot.SOLCarrotConfig;
import com.cazsius.solcarrot.capability.FoodCapability;
import com.cazsius.solcarrot.capability.ProgressInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import squeek.applecore.api.food.FoodEvent;

import static com.cazsius.solcarrot.lib.Localization.localizedComponent;
import static com.cazsius.solcarrot.lib.Localization.localizedQuantityComponent;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID)
public class FoodTracker {
	
	@SubscribeEvent
	public static void onFoodEaten(FoodEvent.FoodEaten event) {
		if (event.player.world.isRemote) return;
		WorldServer world = (WorldServer) event.player.world;
		
		EntityPlayer player = event.player;
		
		FoodCapability foodCapability = FoodCapability.get(player);
		int oldFoodCount = foodCapability.getFoodCount();
		foodCapability.addFood(event.food);
		boolean hasTriedNewFood = foodCapability.getFoodCount() > oldFoodCount;
		CapabilityHandler.syncFoodList(player);
		ProgressInfo progressInfo = foodCapability.getProgressInfo();
		
		boolean newMilestoneReached = MaxHealthHandler.updateFoodHPModifier(player);
		if (newMilestoneReached) {
			// passing the player makes it not play for some reason
			world.playSound(
				null,
				player.posX, player.posY, player.posZ,
				SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
				1.0F, 1.0F
			);
			
			spawnParticles(world, player, EnumParticleTypes.HEART, 12);
			
			if (progressInfo.hasReachedMax()) {
				spawnParticles(world, player, EnumParticleTypes.VILLAGER_HAPPY, 16);
			}
			
			ITextComponent heartsDescription = localizedQuantityComponent("message", "hearts", SOLCarrotConfig.heartsPerMilestone);
			
			if (SOLCarrotConfig.shouldShowProgressAboveHotbar) {
				String messageKey = progressInfo.hasReachedMax() ? "finished.hotbar" : "milestone_achieved";
				player.sendStatusMessage(localizedComponent("message", messageKey, heartsDescription), true);
			} else {
				showChatMessage(player, TextFormatting.DARK_AQUA, localizedComponent("message", "milestone_achieved", heartsDescription));
				if (progressInfo.hasReachedMax()) {
					showChatMessage(player, TextFormatting.GOLD, localizedComponent("message", "finished.chat"));
				}
			}
		} else if (hasTriedNewFood) {
			spawnParticles(world, player, EnumParticleTypes.END_ROD, 12);
		}
	}
	
	private static void spawnParticles(WorldServer world, EntityPlayer player, EnumParticleTypes type, int count) {
		// this overload sends a packet to the client
		world.spawnParticle(
			type,
			player.posX, player.posY + player.getEyeHeight(), player.posZ,
			count,
			0.5F, 0.5F, 0.5F,
			0.0F
		);
	}
	
	private static void showChatMessage(EntityPlayer player, TextFormatting color, ITextComponent message) {
		ITextComponent component = localizedComponent("message", "chat_wrapper", message);
		component.setStyle(new Style().setColor(color));
		player.sendStatusMessage(component, false);
	}
}
