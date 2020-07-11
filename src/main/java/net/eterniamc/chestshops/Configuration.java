package net.eterniamc.chestshops;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Configuration {

	@Setting(value = "ChestShopItemName")
	public static String chestshopitemname = "&6&lChestShop";

	@Setting(value = "ChestShopItemLore")
	public static String chestshopitemlore = "&fPlace chest down to create your chest shop";

	@Setting(value = "PriceMsg")
	public static String PriceMsg = "&fType a price in chat for the item you want to sell.";

	@Setting(value = "AdminMsg")
	public static String AdminMsg = "You have the ability to make this chest shop an Admin Shop, click &aHERE&f to do so";

	@Setting(value = "AdminShopUpdated")
	public static String AdminShopUpdated = "The shop has been updated";

	@Setting(value = "Adminhover")
	public static String Adminhover = "Making this an admin shop will make it have an infinite quantity of whatever item is put in";

	@Setting(value = "PutItem")
	public static String PutItem = "Your shop price is set! Now you can add items by right clicking the chest. To remove items right click the chest with an empty hand.";

	@Setting(value = "AddedItem")
	public static String AddedItem = "Items have been added to the shop!";

	@Setting(value = "AnotherMessage")
	public static String AnotherMessage = "This chest shop can only hold %name% , remove before adding this item";

	@Setting(value = "itemremove")
	public static String itemremove = "Items have been removed from the shop!";

	@Setting(value = "itemempty")
	public static String itemempty = "This chest shop is empty!";

	@Setting(value = "confirm")
	public static String confirm = "&6&lChestShop &7&l>&f This will sell for  %amount%, click &aHERE&f to confirm";

	@Setting(value = "buyamount")
	public static String buyamount = "&2Enter how many you wish to buy in chat.";

	@Setting(value = "notenoughtitems")
	public static String notenoughtitems = "Oh No! This chest doesn't have enough items! Contact the plot owner!";

	@Setting(value = "purchase")
	public static String purchase = "&6&lChestShop &7&l>&f This costs %c%, click &aHERE&f to purchase";

	@Setting(value = "cost")
	public static String cost = "&6&lChestShop &7&l>&f This costs  %c%, click &aHERE&f to purchase";

	@Setting(value = "empty")
	public static String empty = "&c&lUh-Oh! &cThis shop is empty!";

	@Setting(value = "notenoughmoney")
	public static String notenoughmoney = "&c&lTransaction rejected. You don't have enough funds!";

	@Setting(value = "sendmessage")
	public static String sendmessage = "&6&lChestShop &7&l>&f";

	@Setting(value = "sendmsgpayment")
	public static String sendmsgpayment = "&6%quantity% %item% &3for &a$%amt%";

	@Setting(value = "receivedmsg")
	public static String receivedmsg = "You have received &a$%amt%&ffrom your chest shop";

	@Setting(value = "roommsg")
	public static String roommsg = "Don't have enough room!";

	@Setting(value = "confirmsent")
	public static String confirmsent = "You have sent chestshop to %player%";
	
	@Setting(value = "purchaseroom")
	public static String purchaseroom = "&4&lPlease Make room before purchasing!";
	
	@Setting(value = "shopowner")
	public static String shopowner = "&6Owner:&3 %playername%";
	
	@Setting(value = "shopitem")
	public static String shopitem = "&6Item:&3 %itemname%";	
	
	@Setting(value = "shopamount")
	public static String shopamount = "&6Amount:&3 %amount%";	
	
	@Setting(value = "shopprice")
	public static String shopprice = "&6Price Per &3%itemname% - &a$%price%";	
	
	@Setting(value = "shoptitle")
	public static String shoptitle = "&6Shop Information";
	
	@Setting(value = "purchasetitle")
	public static String purchasetitle = "&aPurchase Successfully!";
	
	@Setting(value = "padding", comment = "This here just the padding sort of like the border lines you can customize.")
	public static String padding = "&b-";
	
	@Setting(value = "availableitems")
	public static String availableitems = "&4&lYou cannot break this Chest, you don't have enough space in your inventory to hold the items.";

	@Setting(value = "DisplayItem", comment = "Default is true, but turning this false prevent the item from being display.")
	public static boolean DisplayItem = true;

}