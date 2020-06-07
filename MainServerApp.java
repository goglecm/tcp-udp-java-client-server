package mytcp.mainpack;

public class MainServerApp {

	public static void main(String[] args) {
		new Server("1448", "1449", "main").startServer();
	}
}