package P2P.App;

import java.io.IOException;
import java.util.Scanner;

public class PeerShell implements PeerShellIface {
	
	private String line;
	
	@Override
	public byte getCommand() {
		// TODO Auto-generated method stub
		if (line.equals("query"))
			return PeerCommands.COM_QUERY;
		else if (line.equals("quit"))
			return PeerCommands.COM_QUIT;
		else
			return 0;
	}

	@Override
	public String[] getCommandArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void readCommand() {
		// TODO Auto-generated method stub
		Scanner in = new Scanner(System.in);
		line = in.next();
		in.close();
	}

}
