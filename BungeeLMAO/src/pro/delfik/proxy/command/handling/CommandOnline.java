package pro.delfik.proxy.command.handling;

import net.md_5.bungee.api.CommandSender;
import pro.delfik.proxy.Proxy;
import pro.delfik.proxy.command.Command;
import pro.delfik.util.U;

import java.util.Collection;
import java.util.Collections;

public class CommandOnline extends Command {
	public CommandOnline() {
		super("online", null, "Мониторинг онлайна.");
	}
	
	@Override
	protected void run(CommandSender sender, String[] args) {
		if (args.length > 0) {
			U.msg(sender, "§dmd5custom: §d" + CryptoUtils.md5Custom(args[0]));
			U.msg(sender, "§ahash: §a" + CryptoUtils.getHash(args[0]));
			U.msg(sender, "§bstring: §b" + args[0]);
			return;
		}
		Proxy.getServers().forEach((name, server) -> {
			Collection c = server.getPlayers();
			msg(sender, server, " §e" + (c.isEmpty() ? "§7§oсервер пуст." : Converter.merge(server.getPlayers(), p -> {return p.getDisplayName();}, "§f, §e")));
		});
	}
	
	@Override
	protected Iterable<String> tabComplete(CommandSender sender, String arg, int number) {
		return Collections.emptySet();
	}
}
