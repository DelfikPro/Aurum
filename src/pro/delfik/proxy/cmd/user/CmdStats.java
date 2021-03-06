package pro.delfik.proxy.cmd.user;

import implario.util.ServerType;
import pro.delfik.proxy.cmd.Cmd;
import pro.delfik.proxy.cmd.Command;
import pro.delfik.proxy.cmd.ex.ExCustom;
import pro.delfik.proxy.user.User;
import implario.util.Rank;
import pro.delfik.proxy.stats.GameStats;
import pro.delfik.proxy.stats.Top;

@Cmd(args = 1, help = "[Игра]")
public class CmdStats extends Command{
	public CmdStats() {
		super("stats", Rank.PLAYER, "Просмотр статистики");
	}

	@Override
	protected void run(User user, String args[]) {
		if (user.getServer().startsWith("UHC_")) {
			user.getHandle().chat("/est");
			return;
		}
		Top top = Top.get(ServerType.getType(args[0].toUpperCase()));
		if(top == null)throw new ExCustom("§cИгра не найдена");
		GameStats stats = top.read(user.getName());
		if(stats == null)throw new ExCustom("§cТы ещё не играл на этой игре");
		for(String line : stats.toReadableString())
			user.msg(line);
	}
}
