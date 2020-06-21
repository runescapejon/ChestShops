package net.eterniamc.chestshops.cmds;

import java.util.Collections;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
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
		Player player = args.<Player>getOne("player").get();
		int num = args.<Integer>getOne("quantity").orElse(1);
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
			if (!player.getInventory().canFit(Itemstack)) {
				player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
			}
			if (player.getInventory().canFit(Itemstack)) {
				player.getInventory().offer(Itemstack).getRejectedItems().isEmpty();
			}
		}
		return CommandResult.success();
	}
}
