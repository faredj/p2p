package groupe15;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable{
	Charset c = Charset.forName("UTF-8");
	private SocketChannel sc;
	Peer peer;
	public Client(InetSocketAddress adr) {
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

	public Peer getPeer() {
		return peer;
	}

	public void setPeer(Peer peer) {
		this.peer = peer;
	}

	public void askPeers() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(2048);
			SocketChannel sc = this.sc;
			buffer.put((byte) 2);
			buffer.flip();
			sc.write(buffer);
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void askListFiles(){
		try {
			ByteBuffer buffer = ByteBuffer.allocate(2048);
			SocketChannel sc = this.sc;
			buffer.put((byte) 4);
			buffer.flip();
			sc.write(buffer);
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void declarePeer(){
		try {
			ByteBuffer bb = ByteBuffer.allocate(24);
			SocketChannel sc = this.sc;
			bb.put((byte) 1);
			bb.putInt(this.getPeer().getPort());
			bb.flip();
			sc.write(bb);
			bb.flip();
			bb.get();
			System.out.println("	Port déclaré : "+bb.getInt());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
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
					System.out.println("CLIENT > Déclaration port");
					this.declarePeer();
					break;
				case "2":
					System.out.println("CLIENT > Deande liste des pairs");
					this.askPeers();
					break;
				case "3":
					System.out.println("CLIENT > Demande liste des fichiers");
					this.askListFiles();
					break;
				case "4":
					System.out.println("CLIENT > Téléchargement fichier");
					System.out.print("Nom du fichier : ");
					String nomFichierr = sc.next();
					nomFichierr = nomFichierr.trim();
					//this.getFile(nomFichierr);
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

