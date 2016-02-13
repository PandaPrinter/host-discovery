import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class discovery {

	static Set<String> set = new HashSet<String>();

	private static final Pattern validIP = Pattern
			.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	private static final Pattern validHostname = Pattern
			.compile("^(([a-z]|[a-z][a-z0-9-]*[a-z0-9]).)*([a-z]|[a-z][a-z0-9-]*[a-z0-9])$", Pattern.CASE_INSENSITIVE);

	public static void main(String[] args) {
		findHostNameLocal();
		findHostNameForUser();
		for (String s : set) {
			System.out.println(s);
		}
	}

	public static void findHostNameLocal() {
		File f;
		String[] strs = { "/etc/hosts", "/etc/ssh/ssh_config", "/etc/ssh/ssh_known_hosts" };
		try {
			for (String s : strs) {
				f = new File(s);
				if (f.canRead()) {
					FileReader fileReader = new FileReader(f);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String line;
					while ((line = bufferedReader.readLine()) != null) {

						// read /etc/hosts file

						if (s == "/etc/hosts") {
							if (!line.trim().isEmpty() && Character.isDigit(line.trim().charAt(0))) {
								String[] temp = line.trim().split("\\s+");
								for (int i = 1; i < temp.length; i++) {
									if (Character.isLetter(temp[i].trim().charAt(0))
											&& Character.isLetter(temp[i].trim().charAt(temp[i].length() - 1)))
										set.add(temp[i].trim());
								}
							}
						}

						else if (s == "/etc/ssh/ssh_config") {
							findConfig(line);
						}

						else if (s == "/etc/ssh/ssh_known_hosts") {
							findKnownHosts(line);
						}
					}
					fileReader.close();
				}
			}
		} catch (IOException e) {
		}
	}

	public static void findHostNameForUser() {
		File f;
		List<String> list = new ArrayList<String>();
		try {
			f = new File("/etc/passwd");
			if (f.canRead()) {
				FileReader fileReader = new FileReader(f);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				String path = null;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.indexOf(":") >= 0 && line.trim().charAt(0) != '#') {
						String[] temp = line.trim().split(":");
						list.add(temp[0]);
					}
				}
				for (int i = 0; i < list.size(); i++) {

					String[] str = { "/home/" + list.get(i) + "/.ssh/config",
							"/home/" + list.get(i) + "/.ssh/authorized_keys",
							"/home/" + list.get(i) + "/.ssh/known_hosts" };
					for (String s : str) {
						f = new File(s);
						if (f.canRead()) {
							fileReader = new FileReader(f);
							bufferedReader = new BufferedReader(fileReader);
							line = null;
							while ((line = bufferedReader.readLine()) != null) {

								// read ~/.ssh/config file
								if (s.equals("/home/" + list.get(i) + "/.ssh/config")) {
									findConfig(line);
								}

								// read ~/.ssh/known_hosts file
								else if (s.equals("/home/" + list.get(i) + "/.ssh/known_hosts")) {
									findKnownHosts(line);
								}

								// read ~/.ssh/authorized_keys file
								else if (s.equals("/home/" + list.get(i) + "/.ssh/authorized_keys")) {
									findKeys(line);
								}
							}
						}
					}
				}
				fileReader.close();
			}
		} catch (IOException e) {
		}
	}

	public static void findKeys(String line) {
		if (line.indexOf('@') >= 0) {
			String temp = line.trim().substring(line.indexOf('@') + 1, line.trim().length());
			if (!validateIP(temp)) {
				if (validateHostName(temp))
					set.add(temp);
			}
		}
		if (line.indexOf("from=\"") >= 0) {
			String temp = line.trim().substring(line.indexOf("from=\"") + 6,
					line.indexOf('"', line.indexOf("from=\"") + 6));
			if (temp.contains(",")) {
				String[] temp2 = temp.split(",");
				for (String e : temp2) {
					if (!validateIP(e)) {
						if (validateHostName(e))
							set.add(e);
					}
				}
			} else {
				if (!validateIP(temp)) {
					if (validateHostName(temp))
						set.add(temp);
				}
			}
		}
		if (line.indexOf("permitopen=\"") >= 0) {
			String findStr = "permitopen=\"";
			int lastIndex = 0;
			while (lastIndex != -1) {
				lastIndex = line.trim().indexOf(findStr, lastIndex);
				if (lastIndex != -1) {
					String host = line.trim().substring(lastIndex + 12, line.indexOf('"', lastIndex + 12));
					if (host.indexOf(":") >= 0) {
						String[] temphost = host.split(":");
						if (!validateIP(temphost[0])) {
							if (validateHostName(temphost[0]))
								set.add(temphost[0]);
						}
					}
					lastIndex += findStr.length();
				}
			}
		}
	}

	public static void findKnownHosts(String line) {
		if (!line.trim().isEmpty()) {
			int i = line.trim().charAt(0) == '@' ? 1 : 0;
			String[] temp = line.trim().split("\\s+");
			if (temp.length >= i + 1) {
				if (temp[i].contains(",")) {
					String[] temp2 = temp[i].split(",");
					for (String e : temp2) {
						if (e.indexOf(":") <= 0) {
							if (!validateIP(e.trim())) {
								if (validateHostName(e.trim()))
									set.add(e);
							}
						} else {
							String temp3[] = e.split(":");
							if (!validateIP(temp3[0].trim())) {
								if (validateHostName(temp3[0].trim()))
									set.add(temp3[0]);
							}
						}
					}
				} else {
					if (temp[i].indexOf(":") <= 0) {
						if (!validateIP(temp[i].trim())) {
							if (validateHostName(temp[i].trim()))
								set.add(temp[i]);
						}
					} else {
						String temp3[] = temp[i].split(":");
						if (!validateIP(temp3[0].trim())) {
							if (validateHostName(temp3[0].trim()))
								set.add(temp3[0]);
						}
					}
				}
			}
		}
	}

	public static void findConfig(String line) {
		if (line.trim().length() > 8) {
			if (line.trim().substring(0, 8).equals("HostName")) {
				String result = line.trim().substring(9, line.trim().length());
				String[] temp = result.trim().split("\\s");
				for (String t : temp) {
					if (!validateIP(t.trim())) {
						if (validateHostName(t.trim()))
							set.add(t);
					}
				}
			}
		}
		if (line.trim().length() > 4) {
			if (line.trim().substring(0, 4).equals("Host")) {
				String result = line.trim().substring(5, line.trim().length());
				String[] temp = result.trim().split("\\s");
				for (String t : temp) {
					if (!validateIP(t.trim())) {
						if (validateHostName(t.trim()))
							set.add(t);
					}
				}

			}
		}
		if (line.trim().length() > 12) {
			if (line.trim().substring(0, 12).equals("HostKeyAlias")) {
				String result = line.trim().substring(5, line.trim().length());
				String[] temp = result.trim().split("\\s");
				for (String t : temp) {
					if (!validateIP(t.trim())) {
						if (validateHostName(t.trim()))
							set.add(t);
					}
				}
			}
		}
	}

	public static boolean validateIP(final String ip) {
		return validIP.matcher(ip).matches();
	}

	public static boolean validateHostName(final String hostName) {
		return validHostname.matcher(hostName).matches();
	}
}