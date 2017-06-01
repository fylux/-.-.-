package P2P.PeerPeer.Server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import P2P.App.Peer;
import P2P.PeerPeer.Client.Downloader;
import P2P.PeerPeer.Message.Message;
import P2P.PeerPeer.Message.MessageChunk;
import P2P.PeerPeer.Message.MessageChunkList;
import P2P.PeerPeer.Message.MessageHash;
import P2P.util.FileInfo;

public class SeederThread extends Thread {

	private Downloader downloader;
	protected DataOutputStream dos;
	protected DataInputStream dis;
	private short chunkSize;
	private String fileHash;

	/* Global buffer for performance reasons */
	private byte[] chunkDataBuf;

	public SeederThread(Socket socket, Downloader downloader, short chunkSize) {
		this.downloader = downloader;
		this.chunkSize = chunkSize;

		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Error creating Seeder Thread");
		}
	}

	public void closeSocket() {
		try {
			dis.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send chunkList of the request file 
	 */
	public void sendChunkList() {
		FileInfo fileData = isFileReachable();
		byte response[];
		MessageChunkList chunkList = null;

		if (fileData != null) {
			//We have the complete file
			chunkList = Message.makeChunkList(-1);
			response = chunkList.toByteArray();
		} else if (downloader == null || !fileHash.equals(downloader.getTargetFile().fileHash)) {
			//Request for a file which cannot be served
			response = Message.makeError().toByteArray();
		} else {
			//We are downloading the file
			int chunks[] = downloader.getChunksDownloadedFromSeeders();
			chunkList = Message.makeChunkList(chunks.length, chunks);
			response = chunkList.toByteArray();
		}

		try {
			dos.write(response);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void sendChunk(int index) {
		FileInfo fileData = isFileReachable();
		byte response[];

		if (fileData != null || (downloader != null && fileHash.equals(downloader.getTargetFile().fileHash))) {

			if (fileData == null)
				fileData = downloader.getTargetFile();

			byte data[] = readDataFile(index, fileData);

			if (data != null)
				response = Message.makeChunkData(data, index).toByteArray();
			else
				response = Message.makeError().toByteArray();
		} else {
			response = Message.makeError().toByteArray();
		}
		try {
			dos.write(response);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			byte buffer;
			try {
				buffer = dis.readByte();
			} catch (IOException e1) {
				break;
			}
			MessageHash fileRequested = null;
			MessageChunk requestedChunk = null;

			Message m = analizeType(buffer);
			if (m == null)
				break;
			
			else if (m instanceof MessageHash) {
				fileRequested = (MessageHash) m;
				fileHash = fileRequested.getHash();

				sendChunkList();

			} else if (m instanceof MessageChunk) {
				requestedChunk = (MessageChunk) m;
				int index = requestedChunk.getIndex();
				sendChunk(index);
			}
		}
	}

	private FileInfo isFileReachable() {
		FileInfo shared[] = Peer.db.getLocalSharedFiles();
		FileInfo file = null;
		for (FileInfo f : shared)
			if (f.fileHash.equals(fileHash)) {
				file = f;
				break;
			}
		if (file == null)
			return null;

		File check = new File(Peer.db.getSharedFolderPath() + "/" + file.fileName);
		if (check.exists() && !check.isDirectory())
			return file;
		else {
			// The file was removed
			return null;
		}

	}

	private byte[] readDataFile(int index, FileInfo fileData) {
		int parts = getTotalChunks(fileData);
		int lastSize = getSizeLastChunk(fileData);

		int pos = (index - 1) * chunkSize;
		int dataSize = chunkSize;
		if (index == parts)
			dataSize = lastSize;

		String path = Peer.db.getSharedFolderPath() + "/" + fileData.fileName;
		byte data[] = new byte[dataSize];
		File file = new File(path);
		RandomAccessFile rfi;

		try {
			rfi = new RandomAccessFile(file, "r");
			rfi.seek(pos);
			rfi.read(data, 0, data.length);
			rfi.close();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	public int getTotalChunks(FileInfo file) {
		int result = (int) Math.ceil(Float.valueOf(file.fileSize) / Float.valueOf(chunkSize));
		if (result == 0)
			return 1;
		return result;
	}

	public int getSizeLastChunk(FileInfo file) {
		int size = (int) (file.fileSize % (long) chunkSize);
		if (size == 0)
			return chunkSize;
		else
			return size;
	}

	
	/**
	 * Analyze if the message received is a request of list, of chunk
	 * or an error.
	 */
	private Message analizeType(byte type) {
		byte[] type_array = new byte[1];
		type_array[0] = type;
		byte[] buffer;
		byte[] buff_final;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		if ((int) type == Message.TYPE_REQ_LIST)
			buffer = new byte[Message.SIZE_REQ_LIST - 1];

		else if ((int) type == Message.TYPE_REQ_DATA)
			buffer = new byte[Message.SIZE_REQ_DATA - 1];
		else
			throw new RuntimeException("Invalid message: Type = " + (int) type);

		try {
			dis.readFully(buffer);
			outputStream.write(type_array);
			outputStream.write(buffer);
		} catch (IOException e) {
			System.err.println("Buffer writing error");
		}

		Message m = null;

		if ((int) type == Message.TYPE_REQ_LIST) {
			buff_final = new byte[Message.SIZE_REQ_LIST];
			buff_final = outputStream.toByteArray();
			m = Message.makeReqList(buff_final);
		}

		else if ((int) type == Message.TYPE_REQ_DATA) {
			buff_final = new byte[Message.SIZE_REQ_DATA];
			buff_final = outputStream.toByteArray();
			m = Message.makeReqData(buff_final);
		}

		return m;

	}

}