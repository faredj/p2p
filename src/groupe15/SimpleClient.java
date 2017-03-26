package groupe15;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SimpleClient{
	Charset c = Charset.forName("UTF-8");
	private SocketChannel sc;
	List<Tuple<String, Long>> listefiles = new ArrayList<>();
	public SimpleClient(InetSocketAddress adr) {
		try {
			sc = SocketChannel.open();
			if(sc.connect(adr)){
				System.out.println("Connexion réussie !");
			}else{
				System.out.println("Erreur lors de la connexion");
			}
		} catch (IOException e) {
			System.out.println("Erreur lors de la connexion");
		}
	}
	
	public SocketChannel getSc() {
		return sc;
	}

	public void setSc(SocketChannel sc) {
		this.sc = sc;
	}

	public List<Tuple<String, Long>> getListefiles() {
		return listefiles;
	}

	public void setListefiles(List<Tuple<String, Long>> listefiles) {
		this.listefiles = listefiles;
	}

	public void askPeers() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(2048);
			SocketChannel sc = this.sc;
			buffer.put((byte) 2);
			buffer.flip();
			sc.write(buffer);
			buffer.clear();
			sc.read(buffer);
			sc.read(buffer);
			buffer.flip();
			while ((int)buffer.get() != 3 && buffer.hasRemaining()) {}
			List<Tuple<String, Integer>> listePairs = this.desirializePeers(buffer);
			if(listePairs.size() == 0){
			System.out.println("Aucun pair trouvés !");
			}else{
				int nb  = 1;
				for (Tuple<String, Integer> tuple : listePairs) {
				System.out.println("   "+nb+" | "+tuple.getKey()+"    >>   "+tuple.getVal());
				nb++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<Tuple<String, Long>> askListFiles(){
		List<Tuple<String, Long>> listeFiles = new ArrayList<>();
		try {
			ByteBuffer buffer = ByteBuffer.allocate(2048);
			SocketChannel sc = this.sc;
			buffer.put((byte) 4);
			buffer.flip();
			sc.write(buffer);
			buffer.clear();
			sc.read(buffer);
			sc.read(buffer);
			sc.read(buffer);
			buffer.flip();
			while ((int)buffer.get() != 5 && buffer.hasRemaining()) {
			}
			listeFiles = this.desirializeListeFiles(buffer);
			this.setListefiles(listeFiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listeFiles;
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
				int nameSize = bb.getInt();		//taile du nom du fichier
				ByteBuffer fileName = ByteBuffer.allocate(2048);
				int p = bb.position()+nameSize;
				while (bb.position() != p) {		//get nom fichier
					fileName.put(bb.get());
				}
				long fileSize = bb.getLong();		//taile du fichier
				fileName.flip();
				CharBuffer cb = c.decode(fileName);
				listeFiles.add(new Tuple<String, Long>(cb.toString(), fileSize));
				fileName.clear();
			}
		return listeFiles;
	}
	public void getFile(String nomFile) {
		System.out.println();
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		SocketChannel sc = this.sc;
		
		long sizeFile = -1;
		this.askListFiles();
		List<Tuple<String, Long>> listeFiles = this.getListefiles();
		for (Tuple<String, Long> tuple : listeFiles) {
			if(nomFile.equals(tuple.getKey())){
		    	sizeFile = tuple.getVal();
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

					ByteBuffer fileNameByte = c.encode(nomFile);	//nom du fichier
					int nameStringSize = fileNameByte.limit();		//taille du nom du fichier
					long fragmentLength = 0;
					if(finalPos-curentPos >= 65536){
						fragmentLength = 65536;
					}else{
						fragmentLength = finalPos - curentPos;
					}
					buffer.clear();
					buffer.put((byte) 6);							//Id
					buffer.putInt(nameStringSize);					//taille string (nom fichier)
					buffer.put(fileNameByte);						//nom fichier
					buffer.putLong((long)sizeFile);						//taille total du fichier
					
					buffer.putLong((long)curentPos);						//position du debut du fichier
					buffer.putInt((int)fragmentLength);			//taille du fragment demandé
					buffer.flip();

					sc.write(buffer);
					buffer.flip();
		
					int finalPosition = (int)fragmentLength + nameStringSize + 25;
					ByteBuffer bb = ByteBuffer.allocate(finalPosition-1);
					ByteBuffer readtemp = ByteBuffer.allocate(80000);
					bb.clear();
					readtemp.clear();
					sc.read(readtemp);
					if(finalPos > 65536){
						sc.read(readtemp);
						sc.read(readtemp);
						sc.read(readtemp);
					}
					int firstR = readtemp.position();
					readtemp.flip();
					//int idd;
					while(((int)readtemp.get()) != 7){
						System.out.print("-");
					}
					
					while (readtemp.position() < firstR) {
						bb.put(readtemp.get());
					}
					
					while(bb.remaining() > 0){
				    	 sc.read(bb);
				    }
					
					bb.flip();
					
					int stringSize = bb.getInt();		//taile du nom du fichier
					ByteBuffer tempFileName = ByteBuffer.allocate(1024);
					int p = bb.position()+stringSize;
					while (bb.position() != p) {		//get nom fichier
						tempFileName.put(bb.get());
					}
					tempFileName.flip();
					CharBuffer cb = c.decode(tempFileName);
					fileNameChar = cb.toString();
					bb.getLong();
					bb.getLong();
					bb.getInt();
					
						while (bb.hasRemaining()) {
							file.put(bb.get());
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
			ByteBuffer bb = ByteBuffer.allocate(24);
			SocketChannel sc = this.sc;
			bb.put((byte) 1);
			bb.putInt(3344);
			bb.flip();
			sc.write(bb);
			bb.flip();
			bb.get();
			System.out.println("	Port déclaré : "+bb.getInt());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run(){
		ByteBuffer temp = ByteBuffer.allocate(1024);
		try {
			this.sc.read(temp);
			temp.clear();
			this.sc.read(temp);
			
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
				case "1":
					System.out.println(" > Déclaration port");
					this.declarePeer();
					break;
				case "2":
					System.out.println(" > Deande liste des pairs");
					this.askPeers();
					break;
				case "3":
					System.out.println(" > Demande liste des fichiers");
					List<Tuple<String, Long>> listeFiles = this.askListFiles();
					int nb = 1;
					if(listeFiles.size() != 0){
						for (Tuple<String, Long> tuple : listeFiles) {
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
				case "4":
					System.out.println("Téléchargement fichier");
					System.out.print("Nom du fichier : ");
					String nomFichierr = sc.next();
					nomFichierr = nomFichierr.trim();
					this.getFile(nomFichierr);
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
	public static void main(String[] args) {
		InetSocketAddress adr = new InetSocketAddress("prog-reseau-m1.zzzz.io", 443);
		SimpleClient client = new SimpleClient(adr);
		client.run();
	}
}

