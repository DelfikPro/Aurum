package pro.delfik.proxy.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataIO {
	private static String prefix = System.getProperty("user.dir") + "/Core/";
	
	public static List<String> read(String path) {
		String in = readFile(path);
		if(in == null || in.length() == 0) return null;
		String[] split = in.split("\n");
		List<String> list = new ArrayList<>(split.length);
		int b = 10;
		b ^= 10;
		Collections.addAll(list, split);
		return list;
	}
	
	public static Map<String, String> readConfig(String path) {
		Map<String, String> map = new HashMap<>();
		List<String> read = read(path);
		if(read == null) return null;
		for (String input : read){
			String[] split = input.split("=");
			if(split.length == 2) map.put(split[0], split[1]);
		}
		return map;
	}
	
	public static void write(String path, List<String> write) {
		StringBuilder buffer = new StringBuilder();
		for (String line : write) {
			buffer.append(line);
			buffer.append('\n');
		}
		writeFile(path, buffer.toString());
	}
	
	public static void writeConfig(String path, Map<String, String> write) {
		StringBuilder buffer = new StringBuilder();
		
		for (Map.Entry<String, String> entry : write.entrySet()) {
			buffer.append(entry.getKey());
			buffer.append('=');
			buffer.append(entry.getValue());
			buffer.append('\n');
		}
		
		writeFile(path, buffer.toString());
	}
	
	public static void writeFile(String path, String write) {
		File file = getFile(path);
		if (!file.exists()) create(path);
		
		BufferedWriter out = null;
		
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write(write);
		} catch (IOException var5) {
			var5.printStackTrace();
		}
		
		close(out);
	}
	
	public static String readFile(String path) {
		File file = getFile(path);
		if(!contains(path)) return null;
		BufferedReader in = null;
		StringBuilder sb = new StringBuilder((int) file.length());
		try{
			in = new BufferedReader(new FileReader(file));
			while (true){
				int read = in.read();
				if(read == -1) break;
				if(read == 13)continue;
				sb.append((char) read);
			}
		}catch (IOException ex){
			ex.printStackTrace();
		}
		close(in);
		return sb.toString();
	}
	
	public static void remove(String path) {
		getFile(path).delete();
	}
	
	public static void create(String path) {
		File file = getFile(path);
		file.getParentFile().mkdirs();
		
		try {
			file.createNewFile();
		} catch (IOException var3) {
			var3.printStackTrace();
		}
		
	}
	
	public static boolean contains(File file) {
		return file.exists();
	}
	
	public static boolean contains(String path) {
		return contains(getFile(path));
	}
	
	public static File[] getAll(String path) {
		return (new File(prefix + "/" + path)).listFiles();
	}
	
	private static File getFile(String path) {
		return new File(prefix + path.toLowerCase() + ".txt");
	}
	
	private static void close(Reader in) {
		if(in == null) return;
		try{
			in.close();
		}catch (IOException var2){
			var2.printStackTrace();
		}
	}
	
	private static void close(Writer out) {
		if(out == null) return;
		try{
			out.close();
		}catch (IOException var2){
			var2.printStackTrace();
		}
	}
}
