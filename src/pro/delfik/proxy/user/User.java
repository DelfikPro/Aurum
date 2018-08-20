package pro.delfik.proxy.user;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import pro.delfik.net.packet.PacketAuth;
import pro.delfik.net.packet.PacketPex;
import pro.delfik.net.packet.PacketUser;
import pro.delfik.proxy.Aurum;
import pro.delfik.proxy.Proxy;
import pro.delfik.proxy.cmd.ex.ExCustom;
import pro.delfik.proxy.data.DataIO;
import pro.delfik.proxy.data.Database;
import pro.delfik.proxy.data.Server;
import pro.delfik.util.ByteUnzip;
import pro.delfik.util.ByteZip;
import pro.delfik.util.Byteable;
import pro.delfik.util.Converter;
import pro.delfik.util.CryptoUtils;
import pro.delfik.util.Rank;
import pro.delfik.util.TimedHashMap;
import pro.delfik.util.TimedList;
import pro.delfik.util.U;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class User implements Byteable {
	public static final TimedHashMap<String, String> outAuth = new TimedHashMap<>(60);

	public static final TimedList<String> allowedIP = new TimedList<>(60);

	public static final String path = "players";
	private String lastIP;

	public static String getPath(String nick){
		return path + "/" + Converter.smartLowercase(nick) + "/";
	}
	
	private static final HashMap<String, User> list = new HashMap<>();

	static{
		UserInfo info = new UserInfo("CONSOLE");
		info.rank = Rank.DEV;
		list.put("CONSOLE", new User(info, null, true));
	}
	
	public static User get(String name) {
		return list.get(Converter.smartLowercase(name));
	}

	public static User get(CommandSender sender) {
		return list.get(Converter.smartLowercase(sender.getName()));
	}
	
	public static User load(String name) {
		User u = DataIO.readByteable(getPath(name) + "player", User.class);
		if(u == null)u = new User(name);
		list.put(Converter.smartLowercase(name), u);
		System.out.println(u);
		return u;
	}

	public static void unload(String name) {
		User user = get(name);
		if (user == null) return;
		list.remove(Converter.smartLowercase(name));
		if (!user.authorized) return;
		if (user.mute != null) user.mute.write(name);
		DataIO.writeByteable(getPath(name) + "player", user);
	}

	public static Collection<User> getAll() {
		return list.values();
	}
	
	// non-static
	
	public final String name; // Ник игрока
	private final int connectedAt; // Время, в которое игрок зашёл на сервер (Нужно для подсчёта онлайна)
	private boolean authorized = false; // Авторизован ли игрок
	private String password ; // Hash пароля
	private final int online; // Онлайн до захода на сервер
	private int money; // Баланс игрока
	private Rank rank; // Ранг игрока
	private String server = ""; // Сервер, на котором находится игрок
	private boolean ipbound; // Разрешён ливход только с сохранённого IP
	private boolean pmDisabled; // Включён ли ЛС
	private List<String> friends;
	private List<String> ignoredPlayers;
	
	private Mute mute;

	private String last = "", lastLast;

	public void setLast(String last){
		lastLast = this.last;
		this.last = last;
	}

	public String getLast() {
		return last;
	}

	public String getLastLast() {
		return lastLast;
	}

	public String lastWriter;

	public User(String nick){
		name = nick;
		rank = Rank.PLAYER;
		online = 0;

		connectedAt = (int) (System.currentTimeMillis() / 60000);
	}

	public User(ByteUnzip unzip) {
		name = unzip.getString();
		password = unzip.getString();
		rank = Rank.byChar.get((char) unzip.getByte());
		online = unzip.getInt();
		String lastSeenIP = unzip.getString();
		money = unzip.getInt();
		ipbound = unzip.getBoolean();
		pmDisabled = unzip.getBoolean();
		ignoredPlayers = unzip.getList();
		friends = unzip.getList();

		connectedAt = (int) (System.currentTimeMillis() / 60000);
		mute = Mute.get(name);

		if (lastSeenIP != null && ipbound) {
			ProxiedPlayer p = Proxy.getPlayer(name);
			String ip = outAuth.get(name);
			if (lastSeenIP.equals(p.getAddress().getHostName()) || (ip != null && ip.equals(p.getAddress().getHostName()))) {
				outAuth.remove(name);
				authorize();
				U.msg(p, "§aАвтоматическая авторизация прошла успешно.");
			} else if (!allowedIP.contains(name.toLowerCase())) throw new DifferentIPException(name);
		}


	}

	@Deprecated
	public User(UserInfo userInfo, Mute mute, boolean auth) {
		this.name = userInfo.getName();
		this.rank = userInfo.getRank();
		this.password = userInfo.getPassword();
		this.online = (int) userInfo.getOnline();
		this.money = userInfo.getMoney();
		this.mute = mute;
		this.ipbound = userInfo.isIPBound();
		this.ignoredPlayers = userInfo.getIgnoredPlayers();

		if (auth) this.authorize();
		this.connectedAt = (int) (System.currentTimeMillis() / 60000);
		list.put(Converter.smartLowercase(name), this);
	}
	
	// Implementation
	public void msg(Object... o) {
		U.msg(getSender(), o);
	}

	public void kick(String reason) {
		getHandle().disconnect(new TextComponent(reason));
	}

	public String getIP() {
		ProxiedPlayer p = getHandle();
		return p == null ? "" : p.getAddress().getAddress().getHostAddress();
	}
	public String getLastIP() {
		return lastIP;
	}

	public ServerInfo getServerInfo() {return getHandle().getServer().getInfo();}

	public void updateTab(ProxiedPlayer handle){
		Proxy.i().getScheduler().schedule(Aurum.instance, () -> {
			PlayerListItem item = getTab(handle);
			for (ProxiedPlayer player : handle.getServer().getInfo().getPlayers()){
				User user = get(player);
				player.unsafe().sendPacket(item);
				if(user != null)
					handle.unsafe().sendPacket(user.getTab(player));
			}
		}, 1, TimeUnit.SECONDS);
	}

	public void tell(User dest, String msg) {
		if (isIgnoring(dest.getName())) throw new ExCustom("§cВы не можете писать игроку, который находится у вас в игноре.");
		if (dest.isIgnoring(getName())) throw new ExCustom("§cВы находитесь в чёрном списке у игрока §e" + dest.getName() + "§c.");
		lastWriter = dest.getName();
		msg(U.simple("§e[§fВы §e-> §f" + dest.getName() + "§e] " + msg, "§f>> §e§lОтветить §f<<", "/msg " + dest.getName()));
		dest.telled(this, msg);
	}

	private void telled(User user, String msg){
		lastWriter = user.getName();
		msg(U.simple("§e[§f" + user.getName() + "§e -> §fВы§e] " + msg, "§f>> §e§lОтветить §f<<", "/msg " + user.getName()));
	}

	// Getters & Setters
	public CommandSender getSender(){
		return name.equals("CONSOLE") ? Proxy.getConsole() : getHandle();
	}

	public ProxiedPlayer getHandle() {
		return Proxy.getPlayer(name);
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public void authorize() {
		authorized = true;
		Server server = server();
		if (server != null) server.send(new PacketAuth(name));
	}

	public void setRank(Rank rank) {
		this.rank = rank;
		server().send(new PacketPex(name, rank));
		updateTab(getHandle());
	}

	public Rank getRank() {
		return rank;
	}

	public boolean hasRank(Rank rank) {
		return isAuthorized() && this.rank.ordinal() <= rank.ordinal();
	}

	public Server server(){
		return Server.get(getServer());
	}

	public String getServer() {
		return this.server.equals("") ? "LOBBY_1" : this.server;
	}

	public void setServer(String server) {
		this.server = server;
		server().send(new PacketUser(name, rank, authorized, online, money));
		updateTab(getHandle());
	}

	public void earn(int money) {
		this.money += money;
		updateMoney();
	}
	public void disburse(int money) {
		this.money -= money;
		updateMoney();
	}

	private void updateMoney() {
		Database.sendUpdate("UPDATE Users SET money = " + money + " WHERE name = " + Converter.smartLowercase(name));
	}

	public long getMoney() {
		return money;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password.length() == 0 ? "" : CryptoUtils.getHash(password);
	}

	public long getOnline() {
		return System.currentTimeMillis() - connectedAt + online;
	}

	public UserInfo getInfo() {
		return new UserInfo(name, password, money, rank, getOnline(), getIP(), ipbound, ignoredPlayers, pmDisabled, friends);
	}

	public Mute getActiveMute() {
		return mute;
	}

	public void clearMute(String moderator) {
		mute = null;
		msg("§aТы снова можешь писать в чат. Поблагодари §f" + moderator + "§a за размут.");
	}

	public void mute(Mute muteInfo) {
		mute = muteInfo;
	}

	public String getName() {
		return name;
	}

	public boolean isIPBound() {
		return ipbound;
	}

	private PlayerListItem getTab(ProxiedPlayer player){
		PlayerListItem.Item item = new PlayerListItem.Item();
		item.setUsername(name);
		item.setDisplayName(rank.getNameColor() + name);
		item.setUuid(player.getUniqueId());
		PlayerListItem list = new PlayerListItem();
		list.setItems(new PlayerListItem.Item[]{item});
		list.setAction(PlayerListItem.Action.UPDATE_DISPLAY_NAME);
		return list;
	}

	public boolean setIPBound(boolean IPBound) {
		return this.ipbound = IPBound;
	}

	public void ignore(String player) {
		ignoredPlayers.add(Converter.smartLowercase(player));
	}

	public boolean unignore(String player) {
		return ignoredPlayers.remove(Converter.smartLowercase(player));
	}

	public boolean isIgnoring(String player) {
		return ignoredPlayers.contains(Converter.smartLowercase(player));
	}
	public List<String> getIgnoredPlayers() {
		return ignoredPlayers;
	}

	@Override
	public ByteZip zip() {
		return new ByteZip().add(name).add(password).add(rank.getByte()).add(getOnline()).add(getIP())
					   .add(getMoney()).add(ipbound).add(pmDisabled).add(ignoredPlayers).add(friends);
	}

}
