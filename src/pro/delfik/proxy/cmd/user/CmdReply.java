package pro.delfik.proxy.cmd.user;

import pro.delfik.proxy.cmd.Cmd;
import pro.delfik.proxy.cmd.Command;
import pro.delfik.proxy.cmd.ex.ExCustom;
import pro.delfik.proxy.user.User;
import implario.util.Converter;
import implario.util.Rank;

@Cmd(args = 1, help = "[Сообщение]")
public class CmdReply extends Command{
	public CmdReply(){
		super("reply", Rank.PLAYER, "Ответ на последнее личное сообщение", "r");
	}

	@Override
	protected void run(User user, String[] args) {
		if (user.getLastPenPal() == null) throw new ExCustom("§cВы ещё никому не написали.");
		User dest = User.get(user.getLastPenPal());
		if (dest == null) throw new ExCustom("§6Игрок, с которым вы общались, вышел с сервера.");
		String msg = Converter.mergeArray(args, 0, " ");
		user.sendPM(dest, msg);
	}
}
