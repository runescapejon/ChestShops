package net.eterniamc.chestshops.cmds;

import java.util.Collections;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import net.eterniamc.chestshops.Configuration;

public class ChestShopGiveCommand implements CommandExecutor {

	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
	
		int num = args.<Integer>getOne("quantity").orElse(1);
	
		if (src instanceof Player) {	
			Optional<Player> target = args.getOne("player");
	     	Player targ = target.get();
			Player player = (Player) src;
			if (player.hasPermission("chestshop.give")) {
				org.spongepowered.api.item.inventory.ItemStack item = org.spongepowered.api.item.inventory.ItemStack
						.builder().itemType(ItemTypes.CHEST)
						.add(Keys.DISPLAY_NAME,
								Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.chestshopitemname)))
						.add(Keys.ITEM_LORE,
								Collections.singletonList(
										TextSerializers.FORMATTING_CODE.deserialize(Configuration.chestshopitemlore)))
						.build();
				((net.minecraft.item.ItemStack) (Object) item).getTagCompound().setBoolean("ChestShop", true);
				ItemStack Itemstack = ItemStack.builder().from(item).quantity(num).build();
				if (!targ.getInventory().canFit(Itemstack)) {
					player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
					targ.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
				}
				if (targ.getInventory().canFit(Itemstack)) {
					player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE
							.deserialize(Configuration.confirmsent.replace("%player%", targ.getName().toString()))));
					targ.getInventory().offer(Itemstack).getRejectedItems().isEmpty();
				}
			}
		}
		if (src instanceof ConsoleSource) {
			Player player1 = args.<Player>getOne("player").get();
				org.spongepowered.api.item.inventory.ItemStack item = org.spongepowered.api.item.inventory.ItemStack
						.builder().itemType(ItemTypes.CHEST)
						.add(Keys.DISPLAY_NAME,
								Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.chestshopitemname)))
						.add(Keys.ITEM_LORE,
								Collections.singletonList(
										TextSerializers.FORMATTING_CODE.deserialize(Configuration.chestshopitemlore)))
						.build();
				((net.minecraft.item.ItemStack) (Object) item).getTagCompound().setBoolean("ChestShop", true);
				ItemStack Itemstack = ItemStack.builder().from(item).quantity(num).build();
				if (!player1.getInventory().canFit(Itemstack)) {
					player1.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
				}
				if (player1.getInventory().canFit(Itemstack)) {
					player1.getInventory().offer(Itemstack).getRejectedItems().isEmpty();
				}
		}	
		return CommandResult.success();
	}
}
