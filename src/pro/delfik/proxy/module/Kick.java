package pro.delfik.proxy.module;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pro.delfik.util.Logger;
import pro.delfik.util.U;

import java.util.Date;

public class Kick {
	public static void kick(ProxiedPlayer p, String moderator, String reason) {
		U.bc(p.getServer(), "§7[§e" + moderator + "§7] §6Игрок ", new U.PlayerWrapper(p, "§e"), "§6 был кикнут по причине: §e" + reason);
		p.disconnect(new TextComponent("§cВы были кикнуты с сервера модератором §e" + moderator + "\n§cПричина: §e" + reason));
		Logger.log("Kick", moderator + " kick " + p.getName() + " time " + new Date().toString() +
				" reason: \"" + reason + "\" minutes ");
	}
}
