package pro.delfik.proxy.data;

import pro.delfik.proxy.permissions.PersonInfo;
import pro.delfik.proxy.permissions.Rank;
import pro.delfik.util.Converter;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerDataManager {
	
	public static boolean update(String player, Field field, Object value) throws SQLException {
		int i = Database.sendUpdate("UPDATE Users SET " + field + " = " + adjust(value) + " WHERE name = \'" + player.toLowerCase() + "\'");
		return i != 0;
	}
	public static String parameters = null;
	public static boolean save(PersonInfo info) throws SQLException {
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
			Database.Result res = Database.sendQuery("SELECT * FROM Users WHERE name = '" + Converter.smartLowercase(username) + "'");
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
		IP("ip", PersonInfo::getIP, (u, o) -> u.ip = o.getString("ip")),
		MONEY("money", PersonInfo::getMoney, (u, o) -> u.money = o.getInt("money"));
		
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
	
}
