package groupe15;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Client {
	private SocketChannel socketChannel;
	private InetSocketAddress adr;
	private boolean connected;
	public Client(InetSocketAddress adr) {
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(true);
			this.setConnected(socketChannel.connect(adr));
		} catch (IOException e) {
			System.out.println("Erreur lors de la connexion");
			//e.printStackTrace();
		}
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
	
	public InetSocketAddress getAdr() {
		return adr;
	}

	public void setAdr(InetSocketAddress adr) {
		this.adr = adr;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
	public void getPeers() {
		try {
			ByteBuffer toWrite = ByteBuffer.allocate(1024);
			ByteBuffer toRead = ByteBuffer.allocate(1024);
			Charset c = Charset.forName("UTF-8");
			SocketChannel sc = this.getSocketChannel();
			toWrite.put((byte) 2);
			toWrite.flip();
			sc.write(toWrite);
			toRead.clear();
			sc.read(toRead);

//			////////////DATA TEST
//			ByteBuffer bb = c.encode("prog-reseau-m1.lacl.fr");
//			toRead.putInt(8800);
//			toRead.putInt(bb.limit());
//			toRead.put(bb);
//			
//			ByteBuffer bb2 = c.encode("prog-reseau-m1.zzzz.com");
//			toRead.putInt(2233);
//			toRead.putInt(bb2.limit());
//			toRead.put(bb2);
//			/////////////////
			
			toRead.flip();
			System.out.print("ID : "+toRead.get());
			int nbPairs = toRead.getInt();
			System.out.println(" | Nombre de pairs : "+nbPairs);
			System.out.println("Liste des paires :");
			for (int i = 1; i <= nbPairs; i++){
				int port = toRead.getInt();
				int hostLength = toRead.getInt();
				ByteBuffer tempHost = ByteBuffer.allocate(1024);
				int p = toRead.position()+hostLength;
				while (toRead.position() != p) {
					tempHost.put(toRead.get());
				}
				tempHost.flip();
				CharBuffer cb = c.decode(tempHost);
				System.out.println("	Port : "+port+"  Host : "+cb.toString());
				tempHost.clear();
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public HashMap<Long, String> getFilesList(){
		HashMap<Long, String> listeFiles = new HashMap<Long, String>();
		try {
			ByteBuffer toWrite = ByteBuffer.allocate(24);
			ByteBuffer toRead = ByteBuffer.allocate(1024);
			Charset c = Charset.forName("UTF-8");
			SocketChannel sc = this.getSocketChannel();
			toWrite.put((byte) 4);
			toWrite.flip();
			sc.write(toWrite);
			toRead.clear();
			sc.read(toRead);
			toRead.flip();
			toRead.get();
			int nbFiles = toRead.getInt();
			for (int i = 0; i < nbFiles; i++) {
				int stringSize = toRead.getInt();//taile du nom du fichier
				ByteBuffer tempFileName = ByteBuffer.allocate(1024);
				int p = toRead.position()+stringSize;
				while (toRead.position() != p) {//get nom fichier
					tempFileName.put(toRead.get());
				}
				long sizeFile = toRead.getLong();//taile du fichier
				tempFileName.flip();
				CharBuffer cb = c.decode(tempFileName);
				listeFiles.put(sizeFile, cb.toString());
				tempFileName.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}			
		return listeFiles;
	}
	public void getFile(String nomFile) {
		
		ByteBuffer toWrite = ByteBuffer.allocate(2048);
		ByteBuffer toRead = ByteBuffer.allocate(700000);
		Charset c = Charset.forName("UTF-8");
		SocketChannel sc = this.getSocketChannel();
		
		HashMap<Long, String> listeFiles = this.getFilesList();
		long sizeFile = -1;
		for (Map.Entry<Long, String> entry : listeFiles.entrySet()) {
		    if(nomFile.equals(entry.getValue()))
		    	sizeFile = entry.getKey();
		}
		if(sizeFile != -1){
			try {
				ByteBuffer temp = c.encode(nomFile);
				toWrite.clear();
				toWrite.put((byte) 6);//Id 
				int pos = temp.limit();
				toWrite.putInt(pos);//taille string (nom fichier)
				toWrite.put(temp);//nom fichier
				toWrite.putLong(sizeFile);//taille total du fichier
				toWrite.putLong(0);//position du debut du fichier
				
				toWrite.putInt((int)sizeFile);//taille du fragment demandé
				toWrite.flip();
				
				sc.write(toWrite);
				
				toRead.clear();
				sc.read(toRead);
				toRead.flip();
								
				System.out.println("   ID : "+toRead.get());
				
				int stringSize = toRead.getInt();//taile du nom du fichier
				ByteBuffer tempFileName = ByteBuffer.allocate(1024);
				int p = toRead.position()+stringSize;
				while (toRead.position() != p) {//get nom fichier
					tempFileName.put(toRead.get());
				}
				tempFileName.flip();
				CharBuffer cb = c.decode(tempFileName);
				String fileName = cb.toString();
				System.out.println("	Nom du fichier  : "+fileName);
				System.out.println("	Taille total    : "+toRead.getLong());
				System.out.println("	Pos demandée    : "+toRead.getLong());
				int t = toRead.getInt();
				System.out.println("	Taille fragment : "+t);
				ByteBuffer file = ByteBuffer.allocate(t);
				file.clear();
				while (toRead.hasRemaining()) {
					file.put(toRead.get());
				}
				System.out.println(toRead);
				file.flip();
				FileOutputStream fos = new FileOutputStream("/home/faredj/workspace/groupe15/"+fileName);
				fos.write(file.array());
				fos.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("Fichier introuvable !");
			toWrite.clear();
		}
	}
	public void testUpload() throws IOException{
		ByteBuffer toWrite = ByteBuffer.allocate(700000);
		ByteBuffer toRead = ByteBuffer.allocate(700000);
		Charset c = Charset.forName("UTF-8");
		SocketChannel sc = this.getSocketChannel();
		FileInputStream fis;
		try {
			String s = "jsk.txt";
			fis = new FileInputStream("/home/faredj/workspace/groupe15/"+s);
			byte[] toWriteFile = new byte[fis.available()];
			ByteBuffer nomFile = c.encode(s);
			fis.read(toWriteFile);
			toWrite.clear();
			
			toWrite.put((byte)7);
			toWrite.putInt(7);
			toWrite.put(nomFile);
			long lf = toWriteFile.length;
			toWrite.putLong(lf);
			long pp = 0;
			toWrite.putLong(pp);
			toWrite.putInt(toWriteFile.length);
			toWrite.put(toWriteFile);
			
			toWrite.flip();
			sc.write(toWrite);
			toRead.clear();
			sc.read(toRead);
			toRead.flip();
			System.out.println(toRead);
			System.out.println(toRead.get());
			
			
			ByteBuffer bbb = ByteBuffer.allocate(1024);
			int p = toRead.position()+toRead.getInt();
			while (toRead.position() != p) {//get nom fichier
				bbb.put(toRead.get());
			}
			bbb.flip();
			CharBuffer cb = c.decode(bbb);
			System.out.println(cb.toString());
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public static void main(String[] args) {
		ByteBuffer temp;
		temp = ByteBuffer.allocate(1024);
		InetSocketAddress adr = new InetSocketAddress("prog-reseau-m1.zzzz.io", 443);
		Client client = new Client(adr);
		if(client.isConnected()){
			System.out.println("Connexion réussie !");
		}else{
			System.out.println("Erreur lors de la connexion");
		}
		try {
			client.getSocketChannel().read(temp);
			temp.clear();
			client.getSocketChannel().read(temp);
			
			
			//client.getPeers();
			
			HashMap<Long, String> listeFiles = client.getFilesList();
			System.out.println("Liste des fichiers :");
			for (Map.Entry<Long, String> entry : listeFiles.entrySet()) {
			    Object value = entry.getValue();
			    System.out.println(entry.getValue()+"     "+entry.getKey());
			}
//			
			client.getFile("jsk.txt");
			client.testUpload();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

