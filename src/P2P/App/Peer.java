package P2P.App;

import P2P.PeerTracker.Client.Reporter;
import P2P.util.PeerDatabase;

public class Peer {
	/**
	 * Database of local files shared by this peer, static to force singleton 
	 */
	public static PeerDatabase db;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java Peer <tracker_hostname> <local_shared_folder>");
			return;
		}
		String trackerHostname = args[0];
		String peerSharedFolder = args[1];

		// Create database of shared files to be used by reporter and seeder
		Peer.db = new PeerDatabase(peerSharedFolder);

		// Create client object on peer side to connect to tracker (reporter)
		Reporter client = new Reporter(trackerHostname);
		
		// Create commander object that will accept and process user commands
		PeerController commander = new PeerController(client);

		// Begin conversation with tracker by getting configuration (chunk size)
		commander.getConfigFromTracker();
		
		// Send list of local files to tracker 
		commander.publishSharedFilesToTracker();
		
		// Begin accepting commands from user using shell 
		commander.readCommandFromShell();
		commander.processCurrentCommand();
		
		
		//do {
			
		//} while (commander.shouldQuit() == false);
		
		// Shutdown this peer's server threads by closing the server socket
		client.end();

		System.out.println("Exiting Peer application");
	}
}
