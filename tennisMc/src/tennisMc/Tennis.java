package tennisMc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import Utils.Cuboid;

public class Tennis extends JavaPlugin implements Listener {
	private FallingBlock ball = null;

	private Cuboid Field1;
	private Cuboid Field2;
	private Cuboid Arena;
	private ArrayList<BlockFace> arndBall;
	private ArrayList<Player> spectators;
	private ArrayList<Player> Team1;
	private ArrayList<Player> Team2;

	private File config;

	private ItemStack racket;
	private ItemStack ballItem;

	private int score1 = 0, score2 = 0, currentHit = 0, maxPlayers = 0, nextHit = 0;

	
	public void downloadContent(){
		//TODO add download for Resourcepack
		
		URL website;
		try {
			website = new URL(getConfig().getString("tennis.general.WorldDownloadLink"));
		
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream("information.html");
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onLoad() {

		
		config = new File("./plugins/tennis/tennis.yml");

		try {

			config.getParentFile().mkdir();
			config.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		try {
			getConfig().load(config);
			setupDefaults();
		} catch (Exception e) {
			e.printStackTrace();
		}
		setup();

	}

	@SuppressWarnings("deprecation")
	private void setup() {
		racket = new ItemStack(Material.STICK);
		ItemMeta rMeta = racket.getItemMeta();
		rMeta.setDisplayName(getConfig().getString("tennis.general.Item.name"));
		racket.setItemMeta(rMeta);

		ballItem = new ItemStack(getConfig().getInt("tennis.general.Ball.ItemId"));
		ItemMeta bMeta = ballItem.getItemMeta();
		bMeta.setDisplayName(getConfig().getString("tennis.general.Ball.Itemname"));
		ballItem.setItemMeta(bMeta);

		Team1 = new ArrayList<>();
		Team2 = new ArrayList<>();
		spectators = new ArrayList<>();
		String world = getConfig().getString("tennis.game.world");
		
		
		//TODO add FieldSizes to config instead of fixed coords,
		//Config then contins an |origin| location 
		//->generate relative to |origin| Fields with worldeditApi with the (in config set) correct blocks for each element / net, ground,walls,net pillars
		
		Field1 = new Cuboid(world, getCT("1.x1"), getCT("1.y1"), getCT("1.z1"), getCT("1.x2"), getCT("1.y2"),
				getCT("1.z2"));
		Field2 = new Cuboid(world, getCT("2.x1"), getCT("2.y1"), getCT("2.z1"), getCT("2.x2"), getCT("2.y2"),
				getCT("2.z2"));

		Arena = new Cuboid(Field1.getLowerLocation(), Field2.getUpperLocation());

		maxPlayers = getConfig().getInt("tennis.general.maxPlayers");

		arndBall = new ArrayList<>();

		arndBall.add(BlockFace.DOWN);
		arndBall.add(BlockFace.EAST);
		arndBall.add(BlockFace.WEST);
		arndBall.add(BlockFace.NORTH);
		arndBall.add(BlockFace.SOUTH);
		
		
		
		
		// gameloop
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

			@Override
			public void run() {
				if (ball != null) {
					Block block = ball.getLocation().getBlock();
					Block temp;
					for (BlockFace f : arndBall) {
						if ((temp = block.getRelative(f)).getType() != Material.AIR) {
							if (f != BlockFace.DOWN && temp.getType() == Material.WEB) {
								// bal is in the net
							} else {
								// ball is on ground

							}

							nextRound();
						}
					}

				}

			}
		}, 5, getConfig().getInt("tennis.general.GameTickrate"));

	}

	private void nextRound() {
		//TODO set scoreboard, playeffects & sounds ,set titles
	}

	private double getCT(String get) {
		return getConfig().getDouble("tennis.game.fields.team" + get);
	}

	private void df(String path, Object obj) {
		getConfig().addDefault(path, obj);
	}

	private void setupDefaults() {
		getConfig().options().copyDefaults(true);
		df("tennis.general.enable", true);
		df("tennis.general.maxPlayers", 2);
		df("tennis.game.world", "Arena");
		df("tennis.general.GameTickrate", 5);
		df("tennis.general.enableResourcePack", true);

		df("tennis.general.Ball.Itemname", "§aBall");
		df("tennis.general.Ball.ItemId", 103);
		df("tennis.general.Ball.tickParticle", "BLOCK_ANVIL_STEP");
		df("tennis.general.Item.name", "§aRacket");
		df("tennis.general.Item.Cooldown", 4);
		df("tennis.general.Item.normalThrowstrenght", 0.15);

		df("tennis.game.fields.team1.x1", 0);
		df("tennis.game.fields.team1.z1", 0);
		df("tennis.game.fields.team1.x2", 29);
		df("tennis.game.fields.team1.z2", 28);
		df("tennis.game.fields.team1.y1", 0);
		df("tennis.game.fields.team1.y2", 7);

		df("tennis.game.fields.team2.x1", 31);
		df("tennis.game.fields.team2.z1", 0);
		df("tennis.game.fields.team2.x2", 60);
		df("tennis.game.fields.team2.z2", 28);
		df("tennis.game.fields.team2.y1", 0);
		df("tennis.game.fields.team2.y2", 7);
		df("tennis.general.WorldDownloadLink",""); //TODO SET LINK WHEN UPLOADED
		try {
			getConfig().save(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onInvchange(InventoryInteractEvent e) {
		e.setCancelled(true);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (Bukkit.getServer().getOnlinePlayers().size() >= maxPlayers) {
			e.getPlayer().setGameMode(GameMode.SPECTATOR);

			e.setJoinMessage("§e" + e.getPlayer().getName() + " §ajoined as spectator");
			return;
		}
		e.getPlayer().getInventory().clear();
		String msg = "§e" + e.getPlayer().getName() + " §aJoined Team";
		short teamid = 11;
		if (Team1.size() <= Team2.size()) {
			Team1.add(e.getPlayer());
			msg += " §61";

		} else {
			Team2.add(e.getPlayer());
			msg += " §b2";
			teamid = 14;
		}
		e.getPlayer().getInventory().setItem(8, new ItemStack(35, 1, teamid));
		e.getPlayer().setGameMode(GameMode.ADVENTURE);
		e.setJoinMessage(msg);
		e.getPlayer().getInventory().setItem(0, racket);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		switch (p.getGameMode()) {

		case ADVENTURE:
			if (Team1.contains(p)) {
				if (!Field1.containsLocation(p.getLocation()))
					p.setHealth(p.getHealth() - 1);
			} else if (Team2.contains(p)) {
				if (!Field2.containsLocation(p.getLocation()))
					p.setHealth(p.getHealth() - 1);
			}
			break;

		case CREATIVE:
			resetPlayer(p);
			break;

		case SURVIVAL:
			resetPlayer(p);
			break;
		default:
			break;

		}

	}

	private int getTeam(Player p) {
		return Team1.contains(p) ? 1 : Team2.contains(p) ? 2 : 3;
	}

	@EventHandler
	public void onBallToBlock(BlockPhysicsEvent e) {
		Block block = e.getBlock().getWorld().getBlockAt(e.getBlock().getLocation().add(0, 1, 0));

		if (block.getType() == Material.MELON_BLOCK) {
			e.setCancelled(true);
			block.setType(Material.AIR);
		}

	}

	@EventHandler
	public void onCourt(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR) {
			if (ball == null) {
				if (e.getPlayer().getInventory().getItemInOffHand() == ballItem) {
						//TODO Thow ball ,play in actionbar : ~Next Round starts
				}
			}

		}
	}

	@EventHandler
	public void onHit(EntityDamageByEntityEvent e) {
		e.setCancelled(true);
		if (!(e.getDamager() instanceof Player)) {
			return;
		}

		Player damager = (Player) e.getDamager();

		ItemStack stick = damager.getInventory().getItemInMainHand();

		if (stick.equals(racket)) {
			if (e.getEntity() instanceof FallingBlock) {
				FallingBlock ball = (FallingBlock) e.getEntity();
				// TODO change velocity into players looking direction
			} else if (e.getEntity() instanceof Player) {
				// player hitted another player
				damager.setHealth(damager.getHealth() - e.getDamage());
				damager.sendMessage("§cDont hit other Players §e:)");

			} else {
				// player hitted another entity
				e.getEntity().remove();
			}
		} else {
			db("wrong item");
			e.setCancelled(true);
		}

	}

	private Cuboid getField(int i) {
		return i == 1 ? Field1 : i == 2 ? Field2 : Arena;
	}

	private void resetPlayer(Player p) {
		int team = getTeam(p);
		Cuboid pC = getField(team);
		p.setGameMode(team > 2 ? GameMode.SPECTATOR : GameMode.ADVENTURE);
		p.teleport(pC.getLowerLocation().add(pC.getLowerLocation().subtract(pC.getUpperLocation()).multiply(0.5)));

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player))
			return false;

		Player p = (Player) sender;

		if (command.getName().equals("get") && args.length > 0) {
			try {
				p.sendMessage("" + getConfig().get(args[0]));

			} catch (Exception e) {
				p.sendMessage("there is no such value");
			}
		}

		return false;

	}

	private void db(Object debug) {
		Bukkit.broadcastMessage(debug.toString());
	}

}
