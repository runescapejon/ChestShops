package net.eterniamc.chestshops;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import net.minecraft.block.BlockChest;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.GuiceObjectMapperFactory;

@Plugin(id = "chestshops", name = "ChestShops", description = "The ultimate chest shop", authors = { "Justin" })
public class ChestShops {

	private static final File file = new File("./config/chestShops.nbt");
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static Map<Vector3i, ChestShop> shops = Maps.newConcurrentMap();
	private Map<UUID, Consumer<Text>> chatGuis = Maps.newHashMap();
	private EconomyService es;
	static Object plugin;

	private File configDirectory;
	@SuppressWarnings("unused")
	private Configuration configoptions;
	GuiceObjectMapperFactory factory;
	@Inject
	private Logger logger;
	private ChestShops pl;

	@Inject
	public ChestShops(Logger logger, @ConfigDir(sharedRoot = false) File configDir, GuiceObjectMapperFactory factory) {
		this.logger = logger;
		this.configDirectory = configDir;
		this.factory = factory;
	}

	@Listener
	public void onPreInit(GamePreInitializationEvent event) {
		pl = this;
		loadConfig();
	}

	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		plugin = this;
		pl = this;
		loadConfig();
		Optional<EconomyService> econService = Sponge.getServiceManager().provide(EconomyService.class);

		if (econService.isPresent()) {
			es = econService.get();
		} else {
			getLogger().error("No economy plugin was found! GUI Shop will not work correctly!");
		}
		es = Sponge.getServiceManager().provide(EconomyService.class)
				.orElseThrow(() -> new Error("Economy service not found!"));
		if (file.exists()) {
			try {
				NBTTagCompound nbt = CompressedStreamTools.read(file);
				NBTTagList list = nbt.getTagList("shops", Constants.NBT.TAG_COMPOUND);
				for (NBTBase base : list) {
					try {
						ChestShop shop = ChestShop.readFromNbt((NBTTagCompound) base);
						shops.put(shop.getLocation().getBlockPosition(), shop);
					} catch (Exception e) {
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		executor.scheduleAtFixedRate(() -> {
			Map<Vector3i, ChestShop> copy = Maps.newHashMap(shops);
			Collection<ChestShop> close = copy.values();
			Collection<ChestShop> open = Sets.newHashSet();
			for (Player player : Sponge.getServer().getOnlinePlayers()) {
				try {
					BlockRay<World> ray = BlockRay.from(player).distanceLimit(5).narrowPhase(false).build();
					while (ray.hasNext()) {
						BlockRayHit<World> hit = ray.next();
						if (copy.containsKey(hit.getBlockPosition())) {
							ChestShop shop = copy.get(hit.getBlockPosition());
							if (shop.getLocation().getExtent().getName().equals(player.getWorld().getName())) {
								close.remove(shop);
								open.add(shop);
							}
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Task.builder().execute(() -> {
				close.forEach(ChestShop::close);
				open.forEach(ChestShop::open);
			}).submit(ChestShops.plugin);
		}, 0L, 250, TimeUnit.MILLISECONDS);
		Task.builder().interval(5, TimeUnit.MINUTES).execute(this::save).submit(this);

		CommandSpec chestshop = CommandSpec.builder()
				.description(Text.of("Allow you to give yourself a chestshop with the quantity"))
				.arguments(GenericArguments.optional(GenericArguments.integer(Text.of("quantity"))))
				.permission("chestshop.command").executor(new ChestShopCommand()).build();
		Sponge.getCommandManager().register(this, chestshop, "chestshop");

		CommandSpec chestshopgive = CommandSpec.builder()
				.description(Text.of("Give the amount you want ot another player"))
				.arguments(GenericArguments.playerOrSource(Text.of("player")),
						GenericArguments.optional(GenericArguments.integer(Text.of("quantity"))))
				.permission("chestshop.give").executor(new ChestShopGiveCommand()).build();
		Sponge.getCommandManager().register(this, chestshopgive, "chestshopgive", "csgive");

	}

	@Listener
	public void onServiceProviderChange(ChangeServiceProviderEvent event) {
		if (event.getNewProvider() instanceof EconomyService) {
			es = (EconomyService) event.getNewProvider();
		}
	}

	@Listener
	public void onChestPlaced(ChangeBlockEvent.Place event, @First Player player) {
		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			if (transaction.getDefault().getState().getType() instanceof BlockChest) {
				net.minecraft.item.ItemStack na = (net.minecraft.item.ItemStack) (Object) player
						.getItemInHand(HandTypes.MAIN_HAND).get();
				if (na.getTagCompound() != null && na.getTagCompound().hasKey("ChestShop")) {
					Optional<TileEntity> tile = player.getWorld().getTileEntity(transaction.getDefault().getPosition());
					if (tile.isPresent() && tile.get() instanceof Chest) {
						Chest c = (Chest) tile.get();
						// I figure without having some weird crash that players can have
						// a double chest to store items and to sell on another side of the chest or
						// have fun with double chestshop xD..
						// sort of a feature idk.. it seem a good idea and good feedback on it, feel
						// free to fix this..
						/*
						 * if (c.getConnectedChests().size() == 1) { BlockType type =
						 * transaction.getDefault().getState().getType() == BlockTypes.CHEST ?
						 * BlockTypes.TRAPPED_CHEST : BlockTypes.CHEST;
						 * c.getLocation().setBlock(type.getDefaultState().with(Keys.DIRECTION,
						 * transaction.getDefault().getState().get(Keys.DIRECTION).orElse(Direction.NONE
						 * )) .get()); tile =
						 * player.getWorld().getTileEntity(transaction.getDefault().getPosition()); c =
						 * (Chest) tile.get(); }
						 */
						Chest chest = c; // lambdas >:(
						sendMessage(player, Configuration.PriceMsg);
						chatGuis.put(player.getUniqueId(), text -> {
							ChestShop shop = new ChestShop(chest, player.getUniqueId(),
									Double.parseDouble(text.toPlain().replaceAll("[^0-9.]*", "")));
							if (player.hasPermission("chestshop.admin")) {
								sendMessage(player, Text.builder()
										.append(TextSerializers.FORMATTING_CODE.deserialize(Configuration.AdminMsg))
										.onClick(TextActions.executeCallback(src -> {
											shop.setAdmin(true);
											sendMessage(src, Configuration.AdminShopUpdated);
											return;
										}))
										.onHover(TextActions.showText(Text.of(
												TextSerializers.FORMATTING_CODE.deserialize(Configuration.Adminhover))))
										.build());
							}
							ChestShop old = shops.put(chest.getLocation().getBlockPosition(), shop);
							if (old != null) {
								old.close();
								sendMessage(player, Configuration.PutItem);
							}
						});
						return;
					}
				}
			}
		}
	}

	@Listener
	public void onChestBreak(ChangeBlockEvent.Break event, @First Player player) {
		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			ChestShop shop = shops.get((transaction.getDefault()).getPosition());
			if (shop != null) {
				if (event.getSource() instanceof Player
						&& !shop.getOwner().equals(((Player) event.getSource()).getUniqueId())) {
					event.setCancelled(true);
				} else {
					if (shop.getOwner().equals(((Player) event.getSource()).getUniqueId())) {
						// ItemStack snapshot = shop.getContents().iterator().next();
						// this line above had an issue where if element next return to nothing the
						// hologram "empty" is staying. This is how player break an empty chestshop and
						// yet still active chestshop but without the chest. Below should fix that behavior 
						List<Set<ItemStack>> list = Arrays.asList(shop.getContents());
						Iterator<Set<ItemStack>> iter = list.iterator();
						while (iter.hasNext()) {
							Set<ItemStack> snapshot = iter.next();
							if (snapshot.isEmpty()) {
								shops.remove((transaction.getDefault()).getPosition());
								shop.close();
								return;
							}
							//making sure that empty element doesn't return an error..
							if (!player.getInventory().canFit(snapshot.iterator().next()) && !snapshot.isEmpty()) {
								event.setCancelled(true);
								player.sendMessage((Text.of(
										TextSerializers.FORMATTING_CODE.deserialize(Configuration.availableitems))));
								player.playSound(SoundTypes.BLOCK_ANVIL_PLACE, player.getLocation().getPosition(), 1);
							}
							if (player.getInventory().canFit(snapshot.iterator().next())) {
								shops.remove((transaction.getDefault()).getPosition());
								shop.close();

								Sponge.getServer().getPlayer(shop.getOwner()).ifPresent(player1 -> {
									Sponge.getCommandManager().process((CommandSource) Sponge.getServer().getConsole(),
											"chestshopgive " + player1.getName() + " 1");
									shop.withdraw(shop.sumContents()).forEach(player1.getInventory()::offer);
								});
							}
						}
					}
				}
			}
		}
	}

	// really don't need this...
	/*
	 * @Listener public void onPlayerInteractBlock(InteractBlockEvent.Secondary
	 * event, @First Player player) { if
	 * (shops.containsKey(event.getTargetBlock().getPosition())) { ChestShop shop =
	 * shops.get(event.getTargetBlock().getPosition()); if (((World)
	 * shop.getLocation().getExtent()).getName().equals(player.getWorld().getName())
	 * ) { event.setUseBlockResult(Tristate.FALSE);
	 * event.setUseItemResult(Tristate.FALSE); } } }
	 */

	@Listener(order = Order.PRE)
	public void prePlayerInteractBlock(InteractBlockEvent.Secondary event, @First Player player) {
		ChestShop shop = shops.get(event.getTargetBlock().getPosition());
		if (shops.containsKey(event.getTargetBlock().getPosition())) {

			if (shop.getLocation().getExtent().getName().equals(player.getWorld().getName())) {
				event.setUseBlockResult(Tristate.FALSE);
				event.setUseItemResult(Tristate.FALSE);
				ItemStack stack = shop.getContents().isEmpty() ? null : shop.getContents().iterator().next();
				if (shop.getOwner().equals(player.getUniqueId())) {
					Optional<ItemStack> held = player.getItemInHand(HandTypes.MAIN_HAND);
					if (held.isPresent() && held.get().getType() != ItemTypes.AIR) {
						if (stack == null || (stack.getType() == held.get().getType()
								&& stack.getValues().equals(held.get().getValues()))) {
							shop.add(held.get());
							shop.update();
							player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
							sendMessage(player, Configuration.AddedItem);
						} else {
							sendMessage(player,
									Configuration.AnotherMessage.replace("%name%",
											String.valueOf(stack.get(Keys.DISPLAY_NAME)
													.orElse(Text.of(stack.getType().getName())).toPlainSingle())));

						}
					} else if (!shop.getContents().isEmpty()) {
						ItemStack remove = shop.getContents().iterator().next();
						shop.getContents().remove(remove);
						shop.update();
						player.setItemInHand(HandTypes.MAIN_HAND, remove);
						sendMessage(player, Configuration.itemremove);
					} else {
						sendMessage(player, Configuration.itemempty);
					}
				} else if (stack != null && shop.getBuyPrice() > 0
						&& player.getItemInHand(HandTypes.MAIN_HAND).map(
								held -> stack.getType() == held.getType() && stack.getValues().equals(held.getValues()))
								.orElse(false)) {
					Optional<ItemStack> held = player.getItemInHand(HandTypes.MAIN_HAND);
					double amount = shop.getBuyPrice() * held.get().getQuantity();
					player.sendMessage(Text.builder()
							.append(TextSerializers.FORMATTING_CODE
									.deserialize(Configuration.confirm.replace("%amount%", String.valueOf(amount))))
							.onClick(TextActions.executeCallback(src -> {
								if (withdraw(getUser(shop.getOwner()), amount)) {
									deposit(player, amount);
									shop.add(held.get());  
									player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
								}
							})).build());
				} else if (shop.isAdmin() || shop.sumContents() >= 0) {
					//>=0 is allowing players to have a set of 1 item in chestshop. Essentially sort of fixing the issue with 1x withdraw disappear
					sendMessage(player, Configuration.buyamount);
					chatGuis.put(player.getUniqueId(), text -> {
						int amount = Integer.parseInt(text.toPlain().replaceAll("[^0-9]", ""));
						Optional<UniqueAccount> acc = es.getOrCreateAccount(player.getUniqueId());
						if (!shop.isAdmin() && shop.sumContents() < amount) {
							sendMessage(player, Configuration.notenoughtitems);
						} else {
							player.sendMessage(Text.builder()
									.append(TextSerializers.FORMATTING_CODE.deserialize(Configuration.purchase
											.replace("%c%", String.valueOf(amount * shop.getPrice()))))
									.onClick(TextActions.executeCallback(src -> {
										BigDecimal price = new BigDecimal(shop.getPrice());
										BigDecimal bal = acc.get().getBalance(es.getDefaultCurrency());
										if (bal.compareTo(price) < 0) {
											player.sendMessage(TextSerializers.FORMATTING_CODE
													.deserialize(Configuration.notenoughmoney));
											player.playSound(SoundTypes.BLOCK_ANVIL_PLACE,
													player.getLocation().getPosition(), 1);
											return;
										}
										if (bal.compareTo(price) > 0) {
											// this should prevent any other issues like scamming when a player cannot
											// obtain the item even if the item cannot fit but have the inventory clear
											// like if they have clear inventory but trying to purchase something that
											// is OVER 9000 .-.
											ItemStack is = ItemStack.builder().from(stack).quantity(amount).build();
											if (!player.getInventory().canFit(is)) {
												player.sendMessage(TextSerializers.FORMATTING_CODE
														.deserialize(Configuration.purchaseroom));
												player.playSound(SoundTypes.BLOCK_ANVIL_PLACE,
														player.getLocation().getPosition(), 1);
											}
											if (player.getInventory().canFit(is)) {
												if (shop.getContents().isEmpty()) {
													player.sendMessage(TextSerializers.FORMATTING_CODE
															.deserialize(Configuration.empty));
												}
												if (!shop.getContents().isEmpty()) {
												 
											 
													if (withdraw(player, amount * shop.getPrice())) {
													 
														deposit(getUser(shop.getOwner()), amount * shop.getPrice());
														Set<ItemStack> withdrawn = shop.withdraw(amount);
														withdrawn.forEach(player.getInventory()::offer);
													}
												 
												}
											}
										}
									})).build());
						}
					});
				}
			} else if (shop.sumContents() == 1) {
			 
				player.sendMessage(Text.builder()
						.append(TextSerializers.FORMATTING_CODE
								.deserialize(Configuration.cost.replace("%c%", String.valueOf(shop.getPrice()))))
						.onClick(TextActions.executeCallback(src -> {
							if (shop.getContents().isEmpty()) {
								player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(Configuration.empty));
							}
							if (!shop.getContents().isEmpty()) {
							 
								if (withdraw(player, shop.getPrice())) {
									deposit(getUser(shop.getOwner()), shop.getPrice());
									Set<ItemStack> withdrawn = shop.withdraw(1);
									withdrawn.forEach(player.getInventory()::offer);
								}
							}
						})).build());
			} else {
				sendMessage(player, Configuration.empty);
			}
		}
	}

	@Listener
	public void onPlayerSendMessage(MessageChannelEvent.Chat event, @Root Player player) {
		if (chatGuis.containsKey(player.getUniqueId())) {
			chatGuis.remove(player.getUniqueId()).accept(event.getRawMessage());
			event.setMessageCancelled(true);
		}
	}

	@Listener
	public void onPlayerCollectsItem(ChangeInventoryEvent.Pickup.Pre event) {
		event.setCancelled(shops.values().stream().anyMatch(s -> s.getDisplay() == event.getTargetEntity()));
	}

	@Listener(order = Order.PRE)
	public void onServerStopping(GameStoppingServerEvent event) {
		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);
		shops.values().forEach(ChestShop::close);
		save();
	}

	private void save() {
		NBTTagList list = new NBTTagList();
		shops.values().forEach(shop -> list.appendTag(shop.writeToNbt()));
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("shops", list);
		File backup = new File("./config/chestShops.back");
		try {
			if (!file.exists())
				file.createNewFile();
			CompressedStreamTools.write(nbt, file);
		} catch (IOException e) {
			logger.error("Error while saving chest shop data", e);
			try {
				if (backup.exists()) {
					if (file.exists())
						file.delete();
					backup.renameTo(file);
				}
			} catch (Exception e1) {
				logger.error("Error while restoring to backup", e1);
			}
		}
		try {
			if (!backup.exists())
				backup.createNewFile();
			CompressedStreamTools.write(nbt, backup);
		} catch (IOException e) {
			logger.error("Error while saving backup chest shop data", e);
		}
	}

	private void sendMessage(MessageReceiver receiver, String text) {
		sendMessage(receiver, TextSerializers.FORMATTING_CODE.deserialize(text));
	}

	private void sendMessage(MessageReceiver receiver, Text text) {
		receiver.sendMessage(Text.join(TextSerializers.FORMATTING_CODE.deserialize(Configuration.sendmessage), text));
	}

	private boolean withdraw(User user, double amount) {
		TransactionResult result = es.getOrCreateAccount(user.getUniqueId())
				.orElseThrow(() -> new Error("No account found for " + user.getName()))
				.withdraw(es.getDefaultCurrency(), new BigDecimal(amount), Cause.of(EventContext.empty(), this));
		if (result.getResult() == ResultType.SUCCESS) {
			user.getPlayer().ifPresent(
					p -> sendMessage(p, Configuration.sendmsgpayment.replace("%amt%", String.valueOf(amount))));

			return result.getResult() == ResultType.SUCCESS;
		} else if (result.getResult() == ResultType.ACCOUNT_NO_FUNDS) {
			user.getPlayer().ifPresent(p -> sendMessage(p, Configuration.notenoughmoney));
			user.getPlayer()
					.ifPresent(p -> p.playSound(SoundTypes.BLOCK_ANVIL_PLACE, p.getLocation().getPosition(), 1));
			return result.getResult() == ResultType.ACCOUNT_NO_FUNDS;
		}
		return false;

	}

	private boolean deposit(User user, double amount) {
		TransactionResult result = es.getOrCreateAccount(user.getUniqueId())
				.orElseThrow(() -> new Error("No account found for " + user.getName()))
				.deposit(es.getDefaultCurrency(), new BigDecimal(amount), Cause.of(EventContext.empty(), this));
		user.getPlayer().ifPresent(
				p -> this.sendMessage(p, Configuration.receivedmsg.replace("%amt%", String.valueOf(amount))));
		return result.getResult() == ResultType.SUCCESS;
	}

	private User getUser(UUID uuid) {
		return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid)
				.orElseThrow(() -> new Error("No user found with uuid: " + uuid));
	}

	public File getConfigDirectory() {
		return configDirectory;
	}

	public GuiceObjectMapperFactory getFactory() {
		return factory;
	}

	public boolean loadConfig() {
		if (!pl.getConfigDirectory().exists()) {
			pl.getConfigDirectory().mkdirs();
		}
		try {
			File configFile = new File(getConfigDirectory(), "configuration.conf");
			if (!configFile.exists()) {
				configFile.createNewFile();
				logger.info("Creating Config for ChestShops");
			}
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
					.setFile(configFile).build();
			CommentedConfigurationNode config = loader.load(ConfigurationOptions.defaults()
					.setObjectMapperFactory(pl.getFactory()).setShouldCopyDefaults(true));
			configoptions = config.getValue(TypeToken.of(Configuration.class), new Configuration());
			loader.save(config);
			return true;
		} catch (Exception error) {
			getLogger().error("coudnt make the config", error);
			return false;
		}
	}

	private Logger getLogger() {
		return logger;
	}
}
