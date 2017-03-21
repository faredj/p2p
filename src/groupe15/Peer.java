package groupe15;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Peer {
	Selector selector= null;
	ServerSocketChannel server = null;
	SocketChannel sc;
	public Peer(int port) {
		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			InetSocketAddress adr = new InetSocketAddress(port);
			server.socket().bind(adr);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			InetSocketAddress adrSc = new InetSocketAddress("prog-reseau-m1.zzzz.io", 443);
			sc = SocketChannel.open();
			sc.connect(adrSc);
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ);
			
		} catch (Exception e) {
//			e.printStackTrace();
			System.out.println("Erreur démarrage serveur");
		}
	}
	public void sendPeerList(SocketChannel socketChannel) {
		System.out.print("Envoi liste des pairs...");
		ByteBuffer toWrite = ByteBuffer.allocate(70000);
		Charset c = Charset.forName("UTF-8");
		toWrite.clear();
		Map<Integer, String> peerList = new HashMap<>();
		peerList.put(443, "prog-reseau-m1.zzzz.io");
		int nbPeers = peerList.size();
		toWrite.put((byte)3);
		toWrite.putInt(nbPeers);
		
		for (Map.Entry<Integer, String> entry : peerList.entrySet()) {
			int port = (int)entry.getKey();
			ByteBuffer host = c.encode(entry.getValue());
			toWrite.putInt(port);
			toWrite.putInt(host.limit());
			toWrite.put(host);
		}
		toWrite.flip();
		try {
			if(socketChannel.write(toWrite) > 0){
				toWrite.clear();
				System.out.println("   [OK]");
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void sendFilesList(SocketChannel socketChannel) {
		System.out.print("Envoi liste des fichiers...");
		ByteBuffer toWrite = ByteBuffer.allocate(70000);
		Charset c = Charset.forName("UTF-8");
		toWrite.clear();
		Map<String, Long> filesList = new HashMap<>();
//		filesList.put("filefiletest.txt", (long)1029);
		filesList.put("imaz.png", (long)106089);
//		filesList.put("lefth.txt", (long)364);
//		filesList.put("Z_IMAZIGHEN.jpg", (long)4675);
//		filesList.put("Projet2017.pdf", (long)135987);
		toWrite.put((byte)5);
		int nbFiles = filesList.size();
		toWrite.putInt(nbFiles);
		for (Map.Entry<String, Long> entry : filesList.entrySet()) {
			long fileSize = (long)entry.getValue();
			String nameFileT = entry.getKey();
			ByteBuffer nameFile = c.encode(nameFileT);
			int stringSize = nameFile.limit();
//			nameFile.flip();
			
			toWrite.putInt(stringSize);
			toWrite.put(nameFile);
			toWrite.putLong((long)fileSize);
		}
		toWrite.flip();
		try {
			if(socketChannel.write(toWrite) > 0){
				toWrite.clear();
				System.out.println("   [OK]");
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ByteBuffer sliceBuffer(ByteBuffer byteBuffer, long offset, int sizeFragment){
		ByteBuffer bufferReturn = ByteBuffer.allocate(sizeFragment);
		bufferReturn.clear();
		byteBuffer.position((int)offset);
		int lastPosition = (int)offset + sizeFragment;
		while (byteBuffer.position() < lastPosition) {
			bufferReturn.put(byteBuffer.get());
		}
		return bufferReturn;
	}
	public void sendFile(SocketChannel channel, ByteBuffer toRead) throws IOException{
		Charset c = Charset.forName("UTF-8");
		int stringSize = toRead.getInt();		//taile du nom du fichier
		ByteBuffer tempFileName = ByteBuffer.allocate(1024);
		int p = toRead.position()+stringSize;
		while (toRead.position() != p) {		//get nom fichier
			tempFileName.put(toRead.get());
		}
		tempFileName.flip();
		CharBuffer cb = c.decode(tempFileName);
		String fileNameChar = cb.toString();
		System.out.print("Envoi du fichier "+fileNameChar+" en cours...");
		long totalSize = toRead.getLong();
		long posRead = toRead.getLong();
		int fragmentSize = toRead.getInt();
		ByteBuffer toWrite = ByteBuffer.allocate(70000);
		toWrite.clear();
		toWrite.put((byte)7);
		toWrite.putInt(stringSize);
		ByteBuffer fileNameByte = c.encode(fileNameChar);
		toWrite.put(fileNameByte);
		toWrite.putLong(totalSize);
		toWrite.putLong(posRead);
		toWrite.putInt(fragmentSize);
		
		FileInputStream fis = new FileInputStream("/home/faredj/workspace/"+fileNameChar);
		byte[] fileInput = new byte[fis.available()];
		fis.read(fileInput);
		fis.close();
		ByteBuffer bufForAllFile = ByteBuffer.wrap(fileInput);
		ByteBuffer newByteBuffer = ByteBuffer.allocate(fragmentSize);
		newByteBuffer = sliceBuffer(bufForAllFile, posRead, fragmentSize);
		newByteBuffer.flip();
		toWrite.put(newByteBuffer);
		toWrite.flip();
		if(channel.write(toWrite) > 0){
			toWrite.clear();
			System.out.println("   [OK]");
			System.out.println();
		}
		
	}
	public void run() throws IOException {
		while (true) {
			selector.select();
			Iterator<SelectionKey> keyIt = selector.selectedKeys().iterator();
			while (keyIt.hasNext()) {
				SelectionKey key = keyIt.next();
				if(key.isAcceptable()){
					SocketChannel client = server.accept();
					if(client != null){
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ);
						System.out.println("Nouveau client : "+client.getRemoteAddress());
					}
				}
				if(key.isReadable()){
					SocketChannel channel = (SocketChannel) key.channel();
					ByteBuffer toRead = ByteBuffer.allocate(1024);
					toRead.clear();
					channel.read(toRead);
					toRead.flip();
					int id = (int)toRead.get();
					switch (id) {
						case 2:
							System.out.println("Id "+id+" : Demande de liste des paires connus");
							this.sendPeerList(channel);
							break;
						case 4:
							System.out.println("Id "+id+" : Demande de liste des fichiers connus");
							this.sendFilesList(channel);
							break;
						case 6:
							System.out.println("Id "+id+" : Demande fichier");
							this.sendFile(channel, toRead);
							break;
						default:break;
					}
				}
				keyIt.remove();
			}
		}
	}
	public static void main(String[] args) throws IOException{
		System.out.println("Démarrage serveur");
		Peer peer = new Peer(4433);
		peer.run();
	}
}
