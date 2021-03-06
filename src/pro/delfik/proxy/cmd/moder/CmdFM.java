package pro.delfik.proxy.cmd.moder;

import pro.delfik.proxy.cmd.Cmd;
import pro.delfik.proxy.cmd.Command;
import pro.delfik.proxy.module.Mute;
import pro.delfik.proxy.user.User;
import implario.util.Rank;

import java.util.HashMap;

@Cmd(args = 1, help = "[Игрок]")
public class CmdFM extends Command{
	private final Rule rule;

	public CmdFM(String command, String description, String... aliases){
		super(command, Rank.RECRUIT, description, aliases);
		rule = rules.get(command);
	}
	
	private static final HashMap<String, Rule> rules = new HashMap<>();

	static {
		rules.put("osk", new Rule("Оскорбление", 60));
		rules.put("flood", new Rule("Флуд", 30));
		rules.put("mt", new Rule("Мат", 60));
		rules.put("caps", new Rule("Капс", 30));
		rules.put("amoral", new Rule("Аморал", 120));
	}
	
	@Override
	protected void run(User user, String args[]) {
		Mute.mute(args[0], rule.reason, rule.time, user.getName());
	}
	
	private static class Rule {
		private final String reason;
		private final int time;
		
		public Rule(String reason, int time) {
			this.reason = reason;
			this.time = time;
		}
	}
}
