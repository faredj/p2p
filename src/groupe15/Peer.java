package groupe15;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Peer implements Runnable{
	Charset c = Charset.forName("UTF-8");
	Selector selector= null;
	ServerSocketChannel server = null;
	SocketChannel sc;
	Client client;
	int port;
	List<Tuple<String, Integer>> listePairs = new ArrayList<>();
	List<Tuple<String, Long>> listefiles = new ArrayList<>();
	public Peer(int port) {
		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			InetSocketAddress adr = new InetSocketAddress(port);
			this.port = port;
			server.socket().bind(adr);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			InetSocketAddress adrSc = new InetSocketAddress("prog-reseau-m1.zzzz.io", 443);
			client = new Client(adrSc);
			client.setPeer(this);
			Thread tClient = new Thread(client);
			sc = client.getSc();
			this.setClient(client);
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ);
			tClient.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erreur démarrage serveur");
		}
	}
	public Selector getSelector() {
		return selector;
	}
	public void setSelector(Selector selector) {
		this.selector = selector;
	}
	public ServerSocketChannel getServer() {
		return server;
	}
	public void setServer(ServerSocketChannel server) {
		this.server = server;
	}
	public SocketChannel getSc() {
		return sc;
	}
	public void setSc(SocketChannel sc) {
		this.sc = sc;
	}
	public Client getClient() {
		return client;
	}
	public void setClient(Client client) {
		this.client = client;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public List<Tuple<String, Integer>> getListePairs() {
		return listePairs;
	}
	public void setListePairs(List<Tuple<String, Integer>> listePairs) {
		this.listePairs = listePairs;
	}
	public List<Tuple<String, Long>> getListefiles() {
		return listefiles;
	}
	public void setListefiles(List<Tuple<String, Long>> listefiles) {
		this.listefiles = listefiles;
	}
	public void sendPeers(SocketChannel socketChannel) {
		ByteBuffer buffer = ByteBuffer.allocate(65000);
		buffer.clear();
		this.getClient().askPeers();

		List<Tuple<String, Integer>> listePeers = this.getListePairs();
		int nbPeers = listePeers.size();
		buffer.put((byte)3);
		buffer.putInt(nbPeers);
		for (Tuple<String, Integer> tuple : listePeers) {
			int port = (int)tuple.getVal();
			ByteBuffer host = c.encode(tuple.getKey());
			buffer.putInt(port);
			buffer.putInt(host.limit());
			buffer.put(host);
		}
		buffer.flip();
		try {
			while(buffer.hasRemaining()) {
				socketChannel.write(buffer);		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendListeFiles(SocketChannel socketChannel) {
		ByteBuffer bb = ByteBuffer.allocate(70000);
		bb.clear();
		Map<String, Long> filesLists = new HashMap<>();
		File[] files = new File("/home/faredj/filesTest/").listFiles();
		for (File file : files) {
		    if (file.isFile()) {
		    	filesLists.put(file.getName(), file.length());
		    }
		}
		bb.put((byte)5);
		int nbFiles = filesLists.size();
		bb.putInt(nbFiles);
		for (Map.Entry<String, Long> entry : filesLists.entrySet()) {
			long fileSize = (long)entry.getValue();
			String nameFileT = entry.getKey();
			ByteBuffer nameFile = c.encode(nameFileT);
			int stringSize = nameFile.limit();
			
			bb.putInt(stringSize);
			bb.put(nameFile);
			bb.putLong((long)fileSize);
		}
		bb.flip();
		try {
			while(bb.hasRemaining()) {
				socketChannel.write(bb);		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendFragment(SocketChannel channel, ByteBuffer bb) throws IOException{
		int stringSize = bb.getInt();
		ByteBuffer tempFileName = ByteBuffer.allocate(1024);
		int p = bb.position()+stringSize;
		while (bb.position() != p) {
			tempFileName.put(bb.get());
		}
		tempFileName.flip();
		CharBuffer cb = c.decode(tempFileName);
		String fileNameChar = cb.toString();
		long totalSize = bb.getLong();
		long posRead = bb.getLong();
		int fragmentSize = bb.getInt();

		ByteBuffer buffer = ByteBuffer.allocate(100000);
		buffer.clear();
		buffer.put((byte)7);
		buffer.putInt(stringSize);
		ByteBuffer fileNameByte = c.encode(fileNameChar);
		buffer.put(fileNameByte);
		buffer.putLong(totalSize);
		buffer.putLong(posRead);
		buffer.putInt(fragmentSize);
		int position = (int)posRead;
		FileInputStream fis = new FileInputStream("/home/faredj/filesTest/"+fileNameChar);
		byte[] fileInput = new byte[fis.available()];
		fis.read(fileInput);
		fis.close();
		buffer.put(fileInput, position, fragmentSize);
		buffer.flip();
		while(buffer.hasRemaining()) {
			channel.write(buffer);		
		}		
	}
	
	public List<Tuple<String, Integer>> desirializePeers(ByteBuffer bb){
		List<Tuple<String, Integer>> listePeers = new ArrayList<Tuple<String, Integer>>();
		int nbPairs = bb.getInt();
		for (int i = 1; i <= nbPairs; i++){
			int port = bb.getInt();
			int hostLength = bb.getInt();
			ByteBuffer tempHost = ByteBuffer.allocate(1024);
			int p = bb.position()+hostLength;
			while (bb.position() != p){
				tempHost.put(bb.get());
			}
			tempHost.flip();
			CharBuffer cb = c.decode(tempHost);
			listePeers.add(new Tuple<String, Integer>(cb.toString(), port));
		}
		return listePeers;
	}
	
	public List<Tuple<String, Long>> desirializeListeFiles(ByteBuffer bb) throws IOException{
		List<Tuple<String, Long>> listeFiles = new ArrayList<Tuple<String, Long>>();
			int nbF = bb.getInt();
			for (int i = 1; i <= nbF; i++){
				int nameSize = bb.getInt();
				ByteBuffer fileName = ByteBuffer.allocate(2048);
				int p = bb.position()+nameSize;
				while (bb.position() != p) {
					fileName.put(bb.get());
				}
				long fileSize = bb.getLong();
				fileName.flip();
				CharBuffer cb = c.decode(fileName);
				listeFiles.add(new Tuple<String, Long>(cb.toString(), fileSize));
				fileName.clear();
			}
		return listeFiles;
	}
	
	@Override
	public void run(){
		while (true) {
			try {
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
						ByteBuffer bb = ByteBuffer.allocate(85000);
						bb.clear();
						while(channel.read(bb) > 0){}
						bb.flip();
						if(bb.limit() != 0){
							int id = (int)bb.get();
							switch (id) {
								case 2:
									System.out.println("Id "+id+" : Envoi liste des paires");
									this.sendPeers(channel);
									break;
								case 3:
									System.out.println("Id "+id+" : Reception liste paires");
									List<Tuple<String, Integer>> listeOfPeers = this.desirializePeers(bb);
									this.setListePairs(listeOfPeers);
									if(listeOfPeers.size() == 0){
										System.out.println("Id "+id+" : Aucun pair trouvés !");
									}else{
										int nb  = 1;
										for (Tuple<String, Integer> tuple : listeOfPeers) {
										System.out.println("   "+nb+" | "+tuple.getKey()+"    >>   "+tuple.getVal());
										nb++;
										}
									}
									break;									
								case 4:
									System.out.println("Id "+id+" : Demande de liste des fichiers connus");
									this.sendListeFiles(channel);
									break;
								case 5:
									System.out.println("Id "+id+" : Reception liste fichiers");
									List<Tuple<String, Long>> listeOfFiles = this.desirializeListeFiles(bb);
									this.setListefiles(listeOfFiles);
									int nb = 1;
									if(listeOfFiles.size() != 0){
										for (Tuple<String, Long> tuple : listeOfFiles) {
											String space = "";
											for (int i = tuple.getKey().length(); i < 25; i++)
												space = space+" ";
										    System.out.println("     "+nb+" | "+tuple.getKey()+space+tuple.getVal());
											nb++;
										}
									}else{
										System.out.println("Aucun fichier trouvé");
									}
									break;
								case 6:
									System.out.println("Id "+id+" : Demande fichier");
									this.sendFragment(channel, bb);
									break;
								case 7:
									bb.clear();
									break;
								default:break;
							}
						}

					}
					keyIt.remove();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	public static void main(String[] args) throws IOException{
		System.out.println("Démarrage serveur");
		Thread t1 = new Thread(new Peer(3344));
		t1.start();
	}
}