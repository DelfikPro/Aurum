package pro.delfik.proxy;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pro.delfik.proxy.command.handling.Bans;
import pro.delfik.proxy.command.handling.BansIP;
import pro.delfik.proxy.permissions.Person;
import pro.delfik.proxy.skins.SkinApplier;
import pro.delfik.util.Scheduler;
import pro.delfik.util.StringUtils;

import java.util.concurrent.TimeUnit;

public class OnlineHandler extends Scheduler.Task implements Listener {
	private static volatile int connections = 0;
	
	public OnlineHandler() {
		super(1);
		Scheduler.addTask(this);
	}

	@Override
	public void run() {
		connections = 0;
	}

	@EventHandler
	public void event(ServerConnectEvent event) {
		Person.get(event.getPlayer()).setServer(event.getTarget().getName());
	}
	
	@EventHandler
	public void event(LoginEvent event) {
		connections++;
		if (connections > 20) {
			event.setCancelReason(new TextComponent(""));
			event.setCancelled(true);
		} else {
			String nick = event.getConnection().getName();
			if (nick.length() < 4 || nick.length() > 16 || StringUtils.unContains(nick, StringUtils.allChars)) {
				event.setCancelled(true);
				event.setCancelReason(new TextComponent("Некорректный ник\nСмените ник"));
			} else {
				BansIP.BanIPInfo ipInfo = BansIP.getByAddress(event.getConnection().getAddress().getHostName());
				if (ipInfo != null) {
					event.setCancelled(true);
					event.setCancelReason(BansIP.kickMessage(event.getConnection().getName(), ipInfo.ip, ipInfo.reason, ipInfo.moderator));
					return;
				}
				Bans.BanInfo i = Bans.get(nick);
				if (i != null) {
					if (i.until != 0 && i.until < System.currentTimeMillis()) Bans.clear(nick);
					else {
						event.setCancelled(true);
						event.setCancelReason(Bans.kickMessage(nick, i.reason, i.time, i.until, i.moderator));
					}
				}
			}
			
		}
	}
	
	@EventHandler
	public void onJoin(PostLoginEvent e) {
		ProxyServer.getInstance().getScheduler().runAsync(AurumPlugin.instance, () -> {
			BungeeCord.getInstance().getScheduler().schedule(AurumPlugin.instance, () -> SkinApplier.applySkin(e.getPlayer()), 10L, TimeUnit.MILLISECONDS);
		});
	}

	@EventHandler
	public void event(PostLoginEvent event) {
		String name = event.getPlayer().getName();
		Person p = Person.load(name);
		p.msg(p.getPassword().equals("") ? "§aЗарегистрируйтесь командой§e /reg [Пароль]" : "§aВойдите в игру командой §e/login [Пароль]");
	}
	
	@EventHandler
	public void event(PlayerDisconnectEvent event) {
		String nick = event.getPlayer().getName();
		Person.unload(nick);
	}
}