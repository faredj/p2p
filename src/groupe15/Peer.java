package groupe15;

import java.io.FileInputStream;
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
	public static void sendPeerList(SocketChannel socketChannel) {
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
			System.out.println(toWrite);
			socketChannel.write(toWrite);
			toWrite.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void sendFilesList(SocketChannel socketChannel) {
		ByteBuffer toWrite = ByteBuffer.allocate(70000);
		Charset c = Charset.forName("UTF-8");
		toWrite.clear();
		Map<String, Long> filesList = new HashMap<>();
//		filesList.put("serveurTa3Ch3er.txt", (long)487);
//		filesList.put("JSK.txt", (long)535);
		filesList.put("hohohoho.txt", (long)299);
//		filesList.put("Z_IMAZIGHEN.jpg", (long)4675);
//		filesList.put("MATOUB.jpg.tar.gz", (long)4301);
		toWrite.put((byte)5);
		int nbFiles = filesList.size();
		toWrite.putInt(nbFiles);
		for (Map.Entry<String, Long> entry : filesList.entrySet()) {
			long fileSize = (long)entry.getValue();
			String nameFileT = entry.getKey();
			System.out.println(nameFileT);
			ByteBuffer nameFile = c.encode(nameFileT);
			System.out.println("name file   :   "+nameFile);
			int stringSize = nameFile.limit();
//			nameFile.flip();
			
			System.out.println(stringSize);
			toWrite.putInt(stringSize);
			toWrite.put(nameFile);
			toWrite.putLong((long)fileSize);
		}
		toWrite.flip();
		System.out.println("writerrr : "+toWrite);
		try {
			socketChannel.write(toWrite);
			toWrite.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ByteBuffer sliceBuffer(ByteBuffer byteBuffer, long offset, int sizeFragment){
		System.out.println("origin  "+byteBuffer);

//		byteBuffer.flip();
		ByteBuffer bufferReturn = ByteBuffer.allocate(sizeFragment);
		bufferReturn.clear();
		byteBuffer.position((int)offset);
		int lastPosition = (int)offset + sizeFragment;
		while (byteBuffer.position() < lastPosition) {
			System.out.println("origin  "+byteBuffer);
			System.out.println("temp  "+bufferReturn);
			bufferReturn.put(byteBuffer.get());
		}
		return bufferReturn;
	}
	
	public void run() throws IOException {
		while (true) {
			selector.select();
			//selector.selectNow();
			for (SelectionKey key : selector.selectedKeys()) {
				if(key.isAcceptable()){
					SocketChannel client = server.accept();
					System.out.println(client);
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
				}
				
				if(key.isReadable()){
					Charset c = Charset.forName("UTF-8");
					SocketChannel channel = (SocketChannel) key.channel();
					ByteBuffer toRead = ByteBuffer.allocate(1024);
					toRead.clear();
					channel.read(toRead);
					toRead.flip();
					int id = (int)toRead.get();
					System.out.println(">>>>>>>>    "+id);
					switch (id) {
						case 2:
							System.out.println("Id "+id+" : Demande de liste des paires connus");
							sendPeerList(channel);
							break;
						case 4:
							System.out.println("Id "+id+" : Demande de liste des fichiers connus");
							sendFilesList(channel);
							break;
						case 6:
							System.out.println("Id "+id+" : Demande fichier");
							System.out.println();
							int stringSize = toRead.getInt();		//taile du nom du fichier
							ByteBuffer tempFileName = ByteBuffer.allocate(1024);
							int p = toRead.position()+stringSize;
							while (toRead.position() != p) {		//get nom fichier
								tempFileName.put(toRead.get());
							}
							tempFileName.flip();
							CharBuffer cb = c.decode(tempFileName);
							String fileNameChar = cb.toString();
							System.out.println("nom size : "+stringSize);
							System.out.println("file "+fileNameChar);
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
							ByteBuffer bufForAllFile = ByteBuffer.wrap(fileInput);
							ByteBuffer newByteBuffer = ByteBuffer.allocate(fragmentSize);
							newByteBuffer = sliceBuffer(bufForAllFile, posRead, fragmentSize);
							System.out.println("final   "+newByteBuffer);
							newByteBuffer.flip();
							toWrite.put(newByteBuffer);
							toWrite.flip();
							System.out.println(toWrite);
							channel.write(toWrite);
							break;
						default:break;
					}
				}
			}
		}
	}
	public static void main(String[] args) throws IOException{
		System.out.println("Démarrage serveur");
		Peer peer = new Peer(4433);
		peer.run();
		System.out.println("Serveur démarré");
	}
}
