package net.eterniamc.chestshops.cmds;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import net.eterniamc.chestshops.Configuration;

public class ChestShopGiveCommand implements CommandExecutor {

	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
	
		int quantity = args.<Integer>getOne("quantity").orElse(1);
		Player target = (Player) args.getOne("player").get();
		net.minecraft.item.ItemStack forgeStack = new net.minecraft.item.ItemStack(Blocks.CHEST, quantity);
		forgeStack.setStackDisplayName(Configuration.chestshopitemname.replace("&", "§"));
		NBTTagList nbtLore = new NBTTagList();
		nbtLore.appendTag(new NBTTagString(Configuration.chestshopitemlore.replace("&", "§")));
		forgeStack.getOrCreateSubCompound("display").setTag("Lore", nbtLore);
		forgeStack.getTagCompound().setBoolean("ChestShop", true);
		ItemStack spongeStack = (ItemStack) (Object) forgeStack;
		if (target.getInventory().offer(spongeStack).getType().equals(InventoryTransactionResult.Type.SUCCESS)) {
			src.sendMessage(Text.of(TextSerializers.FORMATTING_CODE
					.deserialize(Configuration.confirmsent.replace("%player%", target.getName().toString()))));

		} else {
			target.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
			if (!src.equals(target))
			src.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(Configuration.roommsg)));
		}	
		return CommandResult.success();
	}
}
