package net.eterniamc.chestshops;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Sets;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;

public class Utility {

	private Set<ItemStack> contents;
	private boolean open = false;
	private UUID owner;
	private Chest chest;
	private Item display;
	private ArmorStand title;
	private ArmorStand description;
	private ArmorStand description1;
	private Location<World> location;
	private double price;
	private double buyPrice;
	private boolean admin;

	@SuppressWarnings("unchecked")
	public Utility(Chest chest, UUID owner, double price) {
		this.contents = (Set<ItemStack>) (Object) Sets.newConcurrentHashSet();
		this.chest = chest;
		this.owner = owner;
		location = (Location<World>) chest.getLocation();
		this.price = price;
	}

	public static Utility readFromNbt(NBTTagCompound nbt) {
		Utility shop = new Utility(
				(Chest) Sponge.getServer().getWorld(nbt.getUniqueId("world")).get()
						.getTileEntity(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")).orElse(null),
				nbt.getUniqueId("owner"), nbt.getDouble("price"));
		NBTTagList stacks = nbt.getTagList("stacks", 10);
		for (NBTBase stack : stacks) {
			shop.contents.add((ItemStack) (Object) new net.minecraft.item.ItemStack((NBTTagCompound) stack));
		}
		shop.setAdmin(nbt.hasKey("admin") && nbt.getBoolean("admin"));
		return shop;
	}

	public NBTTagCompound writeToNbt() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setUniqueId("world", (location.getExtent()).getUniqueId());
		nbt.setInteger("x", location.getBlockX());
		nbt.setInteger("y", location.getBlockY());
		nbt.setInteger("z", location.getBlockZ());
		nbt.setUniqueId("owner", owner);
		nbt.setDouble("price", price);
		nbt.setBoolean("admin", admin);
		NBTTagList list = new NBTTagList();
		for (ItemStack content : contents) {
			list.appendTag(((net.minecraft.item.ItemStack) (Object) content).writeToNBT(new NBTTagCompound()));
		}
		nbt.setTag("stacks", list);
		return nbt;
	}

	public void open() {
		TileEntityChest entityChest = (TileEntityChest) chest;
		if (!open)
			entityChest.getWorld().addBlockEvent(new BlockPos(location.getX(), location.getY(), location.getZ()),
					entityChest.getBlockType(), 1, 1);
		open = true;

		if (title == null) {
			location.getExtent()
					.getNearbyEntities(location.getPosition().add(0.5, (buyPrice > 0.0) ? 1.75 : 1.5, 0.5), 0.1)
					.stream().filter(e -> e instanceof ArmorStand).forEach(Entity::remove);
			title = (ArmorStand) location.getExtent().createEntity(EntityTypes.ARMOR_STAND,
					location.getPosition().add(.5, buyPrice > 0 ? 1.75 : 1.5, .5));
			title.offer(Keys.INVISIBLE, true);
			title.offer(Keys.HAS_GRAVITY, false);
			title.offer(Keys.INFINITE_DESPAWN_DELAY, true);
			title.offer(Keys.DISPLAY_NAME,
					Text.of(TextColors.BLUE, !getContents().isEmpty()
							? ((net.minecraft.item.ItemStack) (Object) contents.iterator().next()).getDisplayName()
							: "Empty"));
			title.offer(Keys.CUSTOM_NAME_VISIBLE, true);
			title.offer(Keys.ARMOR_STAND_MARKER, true);
			location.getExtent().spawnEntity(title);
		}
		if (!getContents().isEmpty()) {
			if (display == null) {
				if (Configuration.DisplayItem) {
					display = (Item) location.getExtent().createEntity(EntityTypes.ITEM,
							location.getPosition().add(.5, 1, .5));
					ItemStack snapshot = getContents().iterator().next().copy();
					snapshot.setQuantity(1);
					display.offer(Keys.REPRESENTED_ITEM, snapshot.createSnapshot());
					display.offer(Keys.PERSISTS, true);
					display.offer(Keys.INFINITE_DESPAWN_DELAY, true);
					display.offer(Keys.EXPIRATION_TICKS, Integer.MAX_VALUE);
					display.setVelocity(new Vector3d());
					location.getExtent().spawnEntity(display);
				}
			}
			if (description == null) {
				((World) location.getExtent())
						.getNearbyEntities(location.getPosition().add(0.5, (buyPrice > 0.0) ? 1.5 : 1.25, 0.5), 0.1)
						.stream().filter(e -> e instanceof ArmorStand).forEach(Entity::remove);
				(description = (ArmorStand) ((World) location.getExtent()).createEntity(EntityTypes.ARMOR_STAND,
						location.getPosition().add(0.5, (buyPrice > 0.0) ? 1.5 : 1.25, 0.5))).offer(Keys.INVISIBLE,
								true);
				description.offer(Keys.HAS_GRAVITY, false);
				description.offer(Keys.INFINITE_DESPAWN_DELAY, true);
				description.offer(Keys.CUSTOM_NAME_VISIBLE, true);
				description.offer(Keys.ARMOR_STAND_MARKER, true);
				String price = (getPrice() + "").replaceAll("[.]0.*", "");
				description
						.offer(Keys.DISPLAY_NAME,
								TextSerializers.FORMATTING_CODE.deserialize("&7Price: &e" + price + (admin ? ""
										: (" &8|&7 Available: &a"
												+ getContents().stream().mapToInt(ItemStack::getQuantity).sum()))));
				location.getExtent().spawnEntity(description);
			}
			if (description1 == null && buyPrice > 0.0) {
				((World) location.getExtent()).getNearbyEntities(location.getPosition().add(0.5, 1.25, 0.5), 0.1)
						.stream().filter(e -> e instanceof ArmorStand).forEach(Entity::remove);
				(description1 = (ArmorStand) ((World) location.getExtent()).createEntity(EntityTypes.ARMOR_STAND,
						location.getPosition().add(0.5, 1.25, 0.5))).offer(Keys.INVISIBLE, true);
				description1.offer(Keys.HAS_GRAVITY, false);
				description1.offer(Keys.INFINITE_DESPAWN_DELAY, true);
				description1.offer(Keys.CUSTOM_NAME_VISIBLE, true);
				description1.offer(Keys.ARMOR_STAND_MARKER, true);
				String price = (getBuyPrice() + "").replaceAll("[.]0.*", "");
				description1.offer(Keys.DISPLAY_NAME,
						TextSerializers.FORMATTING_CODE.deserialize("&7Sell to for: &e" + price));
				location.getExtent().spawnEntity(description1);
			}
		}
	}

	public void close() {
		TileEntityChest entityChest = (TileEntityChest) chest;
		if (open) {
			entityChest.getWorld().addBlockEvent(new BlockPos(location.getX(), location.getY(), location.getZ()),
					entityChest.getBlockType(), 1, 0);
		}
		open = false;
		if (display != null) {
			display.remove();
			display = null;
		}
		if (title != null) {
			title.remove();
			title = null;
		}
		if (description != null) {
			description.remove();
			description = null;
		}
		if (description1 != null) {
			description1.remove();
			description1 = null;
		}
	}

	public void update() {
		if (display != null) {
			display.remove();
		}
		display = null;
		if (title != null) {
			title.remove();
		}
		title = null;
		if (description != null) {
			description.remove();
		}
		description = null;
		if (description1 != null) {
			description1.remove();
		}
		description1 = null;
	}

	public Set<ItemStack> getContents() {
		return contents;
	}

	public int sumContents() {
		return contents.stream().mapToInt(ItemStack::getQuantity).sum();
	}

	public void add(ItemStack stack) {
		for (ItemStack content : contents) {
			if (content.getQuantity() < content.getMaxStackQuantity()) {
				int toAdd = Math.min(stack.getQuantity(), content.getMaxStackQuantity() - content.getQuantity());
				content.setQuantity(content.getQuantity() + toAdd);
				stack.setQuantity(stack.getQuantity() - toAdd);
				if (stack.getQuantity() == 0)
					return;
			}
		}
		contents.add(stack);
	}

	public Set<ItemStack> withdraw(int amount) {
		Set<ItemStack> set = Sets.newHashSet();
		if (admin) {
			while (amount > 0) {
				final ItemStack clone = contents.iterator().next().copy();
				if (amount <= 64) {
					clone.setQuantity(amount);
				} else {
					clone.setQuantity(64);
				}
				set.add(clone);
				amount -= 64;
			}
			return set;
		}
		for (ItemStack content : contents) {
			Iterator<ItemStack> iterator = contents.iterator();
			while (iterator.hasNext() && amount > 0) {
				ItemStack content1 = iterator.next();
				if (content1.getQuantity() > amount) {
					ItemStack copy = content1.copy();
					copy.setQuantity(amount);
					set.add(copy);
					content1.setQuantity(content1.getQuantity() - amount);
					amount = 0;
				} else {
					amount -= content1.getQuantity();
					set.add(content1.copy());
					iterator.remove();
				}
			}
			break;
		}
		if (sumContents() <= 0) {
			contents.clear();
		}
		update();
		return set;
	}

	public Location<World> getLocation() {
		return location;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public UUID getOwner() {
		return owner;
	}

	public Item getDisplay() {
		return display;
	}

	public double getBuyPrice() {
		return buyPrice;
	}
	
	public static void givechestshop(Player p, int num) {
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
		p.getInventory().offer(Itemstack).getRejectedItems().isEmpty();
	}

	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isAdmin() {
		return admin;
	}
}