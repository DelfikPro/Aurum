package pro.delfik.proxy.ev;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pro.delfik.proxy.Aurum;
import pro.delfik.proxy.user.Ban;
import pro.delfik.proxy.user.BanIP;
import pro.delfik.proxy.user.DifferentIPException;
import pro.delfik.proxy.user.User;
import pro.delfik.proxy.skins.SkinApplier;
import pro.delfik.util.Scheduler;
import pro.delfik.util.StringUtils;
import pro.delfik.util.U;

import java.util.concurrent.TimeUnit;

public class EvJoin implements Listener{
	private volatile int connections = 0;

	public EvJoin() {
		Scheduler.addTask(new Scheduler.RunTask(1, () -> connections = 0));
	}

	@EventHandler
	public void event(LoginEvent event) {
		if(checkDDOS(event))return;
		String nick = event.getConnection().getName();
		if(checkNick(event, nick))return;
		if(checkBanIP(event))return;
		if(checkBan(event, nick))return;
	}

	private boolean checkDDOS(LoginEvent event){
		connections++;
		if(connections > 20){
			event.setCancelReason(new TextComponent(""));
			event.setCancelled(true);
			return true;
		}
		return false;
	}

	private boolean checkNick(LoginEvent event, String nick){
		if(nick.length() < 4 || nick.length() > 16 || StringUtils.unContains(nick, StringUtils.allChars)){
			event.setCancelled(true);
			event.setCancelReason(new TextComponent("Некорректный ник\nСмените ник"));
			return true;
		}
		return false;
	}

	private boolean checkBanIP(LoginEvent event){
		String ip = event.getConnection().getAddress().getAddress().getHostAddress();
		BanIP ban = BanIP.get(ip);
		if(ban != null){
			event.setCancelled(true);
			event.setCancelReason(ban.kickMessage(ip));
			return true;
		}
		return false;
	}

	private boolean checkBan(LoginEvent event, String nick){
		Ban ban = Ban.get(nick);
		if(ban != null){
			if(ban.until != 0 && ban.until < System.currentTimeMillis()){
				Ban.unban(nick, null);
				return false;
			}
			event.setCancelled(true);
			event.setCancelReason(ban.kickMessage(nick));
			return true;
		}
		return false;
	}

	@EventHandler
	public void onJoin(PostLoginEvent e) {
		ProxyServer.getInstance().getScheduler().runAsync(Aurum.instance, () ->
				BungeeCord.getInstance().getScheduler().schedule(Aurum.instance, () -> SkinApplier.applySkin(e.getPlayer()), 10L, TimeUnit.MILLISECONDS));
	}

	@EventHandler
	public void event(PostLoginEvent event) {
		String name = event.getPlayer().getName();
		User p;
		try {
			p = User.load(name);
		} catch (DifferentIPException ex) {
			U.msg(event.getPlayer(), "§cIP не опознан.");
			event.getPlayer().disconnect(U.toComponent("§cIP-адрес не совпадает с последним сохранённым.\n§cНапишите боту ВКонтакте команду §fipchange§c, и проблема решится."));
			return;
		}
		if (!p.isIPBound()) p.msg(p.getPassword().equals("") ? "§aЗарегистрируйтесь командой§e /reg [Пароль]" : "§aВойдите в игру командой §e/login [Пароль]");
	}
}
