package groupe15;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
	private SocketChannel socketChannel;
	private InetSocketAddress adr;
	private boolean connected;
	public Client(InetSocketAddress adr, boolean blockingMode) {
		try {
			socketChannel = SocketChannel.open();
			this.setConnected(socketChannel.connect(adr));
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println("Erreur lors de la connexion");
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
	
	public HashMap<Integer, String> getPeers() {
		HashMap<Integer, String> listePeers = new HashMap<Integer, String>();
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
			toRead.flip();
			System.out.println();
			int id;
			while ((id = (int)toRead.get()) != 3 && toRead.hasRemaining()) {
			}
			if(id == 3){
				int nbPairs = toRead.getInt();
				for (int i = 1; i <= nbPairs; i++){
					int port = toRead.getInt();
					int hostLength = toRead.getInt();
					ByteBuffer tempHost = ByteBuffer.allocate(1024);
					int p = toRead.position()+hostLength;
					while (toRead.position() != p){
						tempHost.put(toRead.get());
					}
					tempHost.flip();
					CharBuffer cb = c.decode(tempHost);
					listePeers.put(port, cb.toString());
				}
			}else{
				System.out.println("Erreur dans la réponse");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listePeers;
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
			int id;
			while ((id = (int)toRead.get()) != 5 && toRead.hasRemaining()){}
			if(id == 5){
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
			}else{
				System.out.println("Erreur dans la réponse");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listeFiles;
	}
	
	public void getFile(String nomFile) {
		System.out.println();
		ByteBuffer toWrite = ByteBuffer.allocate(2048);
		Charset c = Charset.forName("UTF-8");
		SocketChannel sc = this.getSocketChannel();
		
		HashMap<Long, String> listeFiles = this.getFilesList();
		long sizeFile = -1;
		for (Map.Entry<Long, String> entry : listeFiles.entrySet()) {
		    if(nomFile.equals(entry.getValue())){
		    	sizeFile = entry.getKey();
		    }
		    	
		}
		if(sizeFile != -1){
			try {
				long curentPos = 0;
				long finalPos = sizeFile;
				ByteBuffer file = ByteBuffer.allocate(((int)sizeFile));
				String fileNameChar = "";
				file.clear();
				
				System.out.println("Téléchargement de : "+nomFile);
				long startTime = System.nanoTime();
				while(curentPos != finalPos){
					System.out.println(file);

					ByteBuffer fileNameByte = c.encode(nomFile);	//nom du fichier
					int nameStringSize = fileNameByte.limit();		//taille du nom du fichier
					long fragmentLength = 0;
					if(finalPos-curentPos >= 65536){
						fragmentLength = 65536;
					}else{
						fragmentLength = finalPos - curentPos;
					}
					toWrite.clear();
					toWrite.put((byte) 6);							//Id
					toWrite.putInt(nameStringSize);					//taille string (nom fichier)
					toWrite.put(fileNameByte);						//nom fichier
					toWrite.putLong((long)sizeFile);						//taille total du fichier
					
					toWrite.putLong((long)curentPos);						//position du debut du fichier
					toWrite.putInt((int)fragmentLength);			//taille du fragment demandé
					toWrite.flip();

					sc.write(toWrite);
					toWrite.flip();
		
					int finalPosition = (int)fragmentLength + nameStringSize + 25;
					ByteBuffer toRead = ByteBuffer.allocate(finalPosition-1);
					ByteBuffer toReadTest = ByteBuffer.allocate(80000);
					toRead.clear();
					toReadTest.clear();
					sc.read(toReadTest);
					if(finalPos > 65536){
						sc.read(toReadTest);
						sc.read(toReadTest);
					}
						
					
					int firstR = toReadTest.position();
					toReadTest.flip();
					int idd;
					while((idd = (int)toReadTest.get()) != 7){
						System.out.print("-");
					}
					while (toReadTest.position() < firstR) {
						toRead.put(toReadTest.get());
					}
					
					while(toRead.remaining() > 0){
				    	 sc.read(toRead);
				     }
					toRead.flip();
		
					
					int stringSize = toRead.getInt();		//taile du nom du fichier
					ByteBuffer tempFileName = ByteBuffer.allocate(1024);
					int p = toRead.position()+stringSize;
					while (toRead.position() != p) {		//get nom fichier
						tempFileName.put(toRead.get());
					}
					tempFileName.flip();
					CharBuffer cb = c.decode(tempFileName);
					fileNameChar = cb.toString();
					toRead.getLong();
					toRead.getLong();
					toRead.getInt();
					
						while (toRead.hasRemaining()) {
							file.put(toRead.get());
						}
						long poooo = (long)file.position();
						curentPos = poooo;
					System.out.print(":");
				}
				long endTime = System.nanoTime();
				long duration = (endTime - startTime)/1000000000;
				System.out.println();
				System.out.println("Durée du téléchargement : "+duration+" sec");

				file.flip();
				FileOutputStream fos = new FileOutputStream("/home/faredj/"+fileNameChar);
				fos.write(file.array());
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("Fichier introuvable !");
		}
	}	
	
	public void declarePeer(){
		try {
			ByteBuffer toWrite = ByteBuffer.allocate(24);
			SocketChannel sc = this.getSocketChannel();
			toWrite.put((byte) 1);
			toWrite.putInt(4433);
			toWrite.flip();
			sc.write(toWrite);
			toWrite.flip();
			toWrite.get();
			System.out.println("	Port déclaré : "+toWrite.getInt());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		ByteBuffer temp = ByteBuffer.allocate(1024);
		InetSocketAddress adr = new InetSocketAddress("prog-reseau-m1.zzzz.io", 443);
//		InetSocketAddress adr = new InetSocketAddress("192.168.43.121", 1996);
//		InetSocketAddress adr = new InetSocketAddress("192.168.43.19", 4433);
		Client client = new Client(adr, false);
		if(client.isConnected()){
			System.out.println("Connexion réussie !");
		}else{
			System.out.println("Erreur lors de la connexion");
		}
		try {
			client.getSocketChannel().read(temp);
			temp.clear();
			client.getSocketChannel().read(temp);
			
			System.out.println("------------------------------------------------------------------------------");
			System.out.println("                       >>Peer to Peer v1.0<<");
			System.out.println("------------------------------------------------------------------------------");
			System.out.println("Menu principal :");
			System.out.println("    1 > Déclarer le port");
			System.out.println("    2 > Récupérer liste des pairs");
			System.out.println("    3 > Récupérer liste des fichiers");
			System.out.println("    4 > Télécharger un fichier");
			System.out.println("    5 > Quitter");
			System.out.println("------------------------------------------------------------------------------");
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(System.in);
			String choix;
			do  {
				System.out.print("Action : ");
				choix = sc.nextLine();
			}while((!(choix.equals("1") || choix.equals("2") || choix.equals("3") || choix.equals("4") || choix.equals("5"))));
			while(choix.equals("1") || choix.equals("2") || choix.equals("3") || choix.equals("4")){//Boucle du menu principal
				switch (choix) {
				case "1"://déclarer port
					System.out.println("ID 1 : déclaration port");
					client.declarePeer();
					break;
				case "2"://recup liste des pairs
					System.out.println("ID 2 : Demande liste des pairs");
					HashMap<Integer, String> listePairs = client.getPeers();
					if(listePairs.size() == 0){
						System.out.println("Aucun pair trouvés !");
					}else{
						int nb  = 1;
						for (Map.Entry<Integer, String> entry : listePairs.entrySet()) {
					    System.out.println("   "+nb+" | "+entry.getValue()+"    >>   "+entry.getKey());
						nb++;
						}
					}
					
					break;
				case "3"://récupérer liste des fichiers 
					System.out.println("ID 4 : Demande liste des fichirs");
					HashMap<Long, String> listeFiles = client.getFilesList();
					int nb = 1;
					if(listeFiles.size() != 0){
						for (Map.Entry<Long, String> entry : listeFiles.entrySet()) {
						String space = "";
						for (int i = entry.getValue().length(); i < 25; i++)
							space = space+" ";
					    System.out.println("     "+nb+" | "+entry.getValue()+space+entry.getKey());
					    nb++;
						}
					}else{
						System.out.println("Aucun fichier trouvé");
					}
					break;
				case "4"://télécharger fichier
					System.out.println("ID 4 : Téléchargement fichier");
					System.out.print("Entrez le nom du fichier à télécharger : ");
					String nomFichierr = sc.nextLine();
					nomFichierr = nomFichierr.replaceAll("[\n\r]", "");
					client.getFile("Do.tif");
					break;
				default:break;
				}
				System.out.println("------------------------------------------------------------------------------");
				System.out.println("                       >>Peer to Peer v1.0<<");
				System.out.println("------------------------------------------------------------------------------");
				System.out.println("Menu principal :");
				System.out.println("    1 > Déclarer le port");
				System.out.println("    2 > Récupérer liste des pairs");
				System.out.println("    3 > Récupérer liste des fichiers");
				System.out.println("    4 > Télécharger un fichier");
				System.out.println("    5 > Quitter");
				System.out.println("------------------------------------------------------------------------------");
				do  {
					System.out.print("Action : ");
					choix = sc.nextLine();
				}while((!(choix.equals("1") || choix.equals("2") || choix.equals("3") || choix.equals("4") || choix.equals("5"))));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

