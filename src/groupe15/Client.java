package groupe15;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
			e.printStackTrace();
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
			ByteBuffer toWrite = ByteBuffer.allocate(24);
			ByteBuffer toRead = ByteBuffer.allocate(1024);
			SocketChannel sc = this.getSocketChannel();
			toWrite.put((byte) 2);
			toWrite.flip();
			sc.write(toWrite);
			sc.read(toRead);
			toRead.flip();
			System.out.println(toRead);
			System.out.println("> "+toRead.get()+" "+toRead.get());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public void getFilesList() {
		try {
			ByteBuffer toWrite = ByteBuffer.allocate(24);
			ByteBuffer toRead = ByteBuffer.allocate(1024);

		} catch (IOException e) {
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
			client.getPeers();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

