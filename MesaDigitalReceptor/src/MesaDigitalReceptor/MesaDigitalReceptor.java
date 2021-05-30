package MesaDigitalReceptor;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
@SuppressWarnings("serial")
public class MesaDigitalReceptor{
	private Dimension telaPC=Toolkit.getDefaultToolkit().getScreenSize();
	private Dimension telaAndroid;
	private int portIni=12;
	private int portInps=6182;
	private int portImgs=11272;
	private ServerSocket serverIni;
	private ServerSocket serverInps;
	private ServerSocket serverImgs;
	public static void main(String[]vars){new MesaDigitalReceptor();}
	public MesaDigitalReceptor(){
		try{
			JOptionPane.showMessageDialog(null,InetAddress.getLocalHost(),"Transmitter's IP",JOptionPane.INFORMATION_MESSAGE);
		}catch(HeadlessException|UnknownHostException erro){
			System.exit(0);
		}
		inicio();
		recebeInputs();
		enviaImagens();
		try{
			SystemTray.getSystemTray().add(new TrayIcon(ImageIO.read(getClass().getResource("Receptor.png")),"Receptor",new PopupMenu(){{
				add(new MenuItem("Exit"){{addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){System.exit(0);}});}});
			}}));
		}catch(AWTException|IOException erro){
			JOptionPane.showMessageDialog(null,"Error: Can't load icon!\n"+erro,"Error!",JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	private void inicio(){
		new Thread(new Runnable(){
			public void run(){
				try{
					serverIni=new ServerSocket(portIni);
					while(true){
						//INICIA
						Socket socket=serverIni.accept();
						ObjectInputStream input=new ObjectInputStream(socket.getInputStream());
						//RECEBE
						final String size=(String)input.readObject();
						final int width=Integer.parseInt(size.substring(0,size.indexOf(",")));
						final int height=Integer.parseInt(size.substring(size.indexOf(",")+1,size.length()));
						telaAndroid=new Dimension(width,height);
						//ENCERRA
						input.close();
						socket.close();
					}
				}catch(IOException|ClassNotFoundException erro){
					JOptionPane.showMessageDialog(null,"Error: Can't connect!\n"+erro,"Error!",JOptionPane.ERROR_MESSAGE);
					try{
						serverIni.close();
					}catch(IOException error){}
					System.exit(0);
				}
			}
		}).start();
	}
	private void recebeInputs(){
		new Thread(new Runnable(){
			public void run(){
				try{
					serverInps=new ServerSocket(portInps);
					Robot Bot=null;
					try{
						Bot=new Robot();
					}catch(AWTException erro){
						System.exit(0);
					}
					Toolkit.getDefaultToolkit().setLockingKeyState(KeyEvent.VK_NUM_LOCK,false);
					while(true){
						//INICIA
						Socket socket=serverInps.accept();
						ObjectInputStream input=new ObjectInputStream(socket.getInputStream());
						//RECEBE
						String dados=(String)input.readObject();
						int tam=(dados.equals("X")?0:dados.length());
						if(dados.startsWith("MP:")){
							final int x=Integer.parseInt(dados.substring(3,dados.indexOf(",")));
							final int y=Integer.parseInt(dados.substring(dados.indexOf(",")+1,tam));
							final int realX=(telaPC.width*x)/telaAndroid.width;
							final int realY=(telaPC.height*y)/telaAndroid.height;
							Bot.mouseMove(realX,realY);
						}else if(dados.startsWith("MBP:")){//MOUSE BUTTON PRESS
							Bot.mousePress(Integer.parseInt(dados.substring(4,tam)));
						}else if(dados.startsWith("MBR:")){//MOUSE BUTTON RELEASE
							Bot.mouseRelease(Integer.parseInt(dados.substring(4,tam)));
						}else if(dados.startsWith("MW:")){//MOUSE WHEEL
							Bot.mouseWheel(Integer.parseInt(dados.substring(3,tam)));
						}else if(dados.startsWith("KP:")){//KEY PRESS
							Bot.keyPress(Integer.parseInt(dados.substring(3,tam)));
						}else if(dados.startsWith("KR:")){//KEY RELEASE
							Bot.keyRelease(Integer.parseInt(dados.substring(3,tam)));
						}
						//ENCERRA
						input.close();
						socket.close();
						if(dados.equals("X"))break;
					}
				}catch(IOException|ClassNotFoundException erro){
					JOptionPane.showMessageDialog(null,"Error: Can't receive images!\n"+erro,"Error!",JOptionPane.ERROR_MESSAGE);
				}finally{
					try{
						serverInps.close();
						serverImgs.close();
					}catch(IOException error){}
					System.exit(0);
				}
			}
		}).start();
	}
	private void enviaImagens(){
		new Thread(new Runnable(){
			public void run(){
				try{
					serverImgs=new ServerSocket(portImgs);
					Robot Bot=null;
					try{
						Bot=new Robot();
					}catch(AWTException erro){
						System.exit(0);
					}
					while(true){
						//INICIA
						Socket socket=serverImgs.accept();
						OutputStream output=socket.getOutputStream();
						//ENVIA
						ImageIO.write(Bot.createScreenCapture(new Rectangle(0,0,telaPC.width,telaPC.height)),"jpg",output);
						output.flush();
						//ENCERRA
						output.close();
						socket.close();
					}
				}catch(IOException erro){
					JOptionPane.showMessageDialog(null,"Error: Can't send images!\n"+erro,"Error!",JOptionPane.ERROR_MESSAGE);
					try{
						serverInps.close();
						serverImgs.close();
					}catch(IOException error){}
					System.exit(0);
				}
			}
		}).start();
	}
}