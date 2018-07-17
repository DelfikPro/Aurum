package pro.delfik.proxy.data;

import pro.delfik.proxy.permissions.Person;
import pro.delfik.proxy.permissions.PersonInfo;
import pro.delfik.util.Converter;
import pro.delfik.util.Rank;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerDataManager {
	
	public static boolean update(String player, Field field, Object value) {
		int i = Database.sendUpdate("UPDATE Users SET " + field + " = " + adjust(value) + " WHERE name = \'" + player.toLowerCase() + "\'");
		return i != 0;
	}

	public static String parameters = null;

	public static boolean save(PersonInfo info) {
		if (parameters == null) parameters = Converter.merge(Field.values(), Field::toString, ", ");
		String values = Converter.merge(Field.values(), (f) -> adjust(f.extractFrom(info)), ", ");
		String replace = Converter.merge(Field.values(), f -> f.string + " = " + adjust(f.extractFrom(info)), ", ");
		Database.sendUpdate("INSERT INTO Users (" + parameters + ") VALUES (" + values + ") ON DUPLICATE KEY " +
				"UPDATE " + replace);
		return true;
	}

	public static PersonInfo load(String username) {
		PersonInfo i = new PersonInfo(username);
		try {
			Database.Result res = Database.sendQuery("SELECT * FROM Users WHERE name = '" + username + "'");
			ResultSet r = res.set;
			try {
				if (!r.next()) return null;
				for (Field f : Field.values()) f.applyTo(i, r);
			} finally {
				res.st.close();
			}
		} catch (SQLException e) {return i;}
		return i;
	}
	
	private static String adjust(Object value) {
		if (value == null) return "NULL";
		if (value instanceof String)
			if (value.equals("NULL")) return "NULL";
			else return '\'' + value.toString() + '\'';
		else return value.toString();
	}
	
	
	public enum Field {
		NAME("name", PersonInfo::getName, (u, o) -> {}),
		RANK("rank", info -> info.getRank().toString(), (u, o) -> u.rank = Rank.decode(o.getString("rank"))),
		PASSHASH("passhash", PersonInfo::getPassword, (u, o) -> u.password = o.getString("passhash")),
		ONLINE("online", PersonInfo::getOnline, (u, o) -> u.online = o.getLong("online")),
		IP("ip", PersonInfo::getIp, (u, o) -> u.ip = o.getString("ip")),
		MONEY("money", PersonInfo::getMoney, (u, o) -> u.money = o.getInt("money")),
		IPBOUND("ipbound", PersonInfo::isIPBound, (u, o) -> u.ipbound = o.getBoolean("ipbound")),
		PMDISABLED("pmdisabled", PersonInfo::isPmDisabled, (u, o) -> u.pmDisabled = o.getBoolean("pmdisabled")),
		IGNOREDPLAYERS("ignored", i -> Converter.merge(i.getIgnoredPlayers(), s -> s, " "),
							(u, o) -> u.ignoredPlayers = Converter.deserializeList(o.getString("ignored"), " ")),
		FRIENDS("friends", i -> Converter.merge(i.getFriends(), s -> s, " "),
					   		(u, o) -> u.friends = Converter.deserializeList(o.getString("friends"), " "));
		
		private final String string;
		private final Extractor extractor;
		private final Applier applier;
		
		Field(String string, Extractor extractor, Applier applier) {
			this.extractor = extractor;
			this.string = string;
			this.applier = applier;
		}
		@Override
		public String toString() {
			return string;
		}
		
		public Object extractFrom(PersonInfo u) {
			return extractor.extract(u);
		}

		public void applyTo(PersonInfo p, ResultSet r) throws SQLException {
			applier.set(p, r);
		}
		
		@FunctionalInterface
		public interface Extractor {
			Object extract(PersonInfo info);
		}
		
		@FunctionalInterface
		private interface Applier {
			void set(PersonInfo u, ResultSet set) throws SQLException;
		}
	}

	public static void setRank(String username, Rank rank) {
		Person user = Person.get(username);
		if (user == null) {
			Database.sendUpdate("UPDATE Users SET rank = '" + rank + "' WHERE name = '" + Converter.smartLowercase(username) + "'");
		} else {
			user.setRank(rank);
			DataEvent.event(user.getServerInfo(), "pex", username + "/" + rank);
		}
	}
}
