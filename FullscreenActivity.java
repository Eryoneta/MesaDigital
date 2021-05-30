package com.example.masaio.mesadigital;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
public class FullscreenActivity extends Activity{
    public static DrawingView TELA;
    public static List<enviaInputs>INPUTS=new ArrayList<enviaInputs>();
    public static String IP="192.168.0.1";
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(TELA=new DrawingView(this));
        showGetIP();
    }
    public class DrawingView extends View{
        private Paint fundo;
        private Bitmap imagem;
        public Bitmap getImagem(){return imagem;}
        public void setImagem(Bitmap imagem){this.imagem=imagem;}
        public DrawingView(Context c){
            super(c);
            fundo=new Paint();
            fundo.setAntiAlias(true);
            fundo.setFilterBitmap(true);
            fundo.setDither(true);
        }
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            Rect dstRect=new Rect();
            canvas.getClipBounds(dstRect);
            if(getImagem()!=null)canvas.drawBitmap(getImagem(),null,dstRect,fundo);
        }
        public boolean onTouchEvent(MotionEvent event){
            int x=(int)event.getX();
            int y=(int)event.getY();
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    enviaInputs("MP:"+x+","+y);
                    enviaInputs("MBP:16");
                    break;
                case MotionEvent.ACTION_MOVE:
                    enviaInputs("MP:"+x+","+y);
                    break;
                case MotionEvent.ACTION_UP:
                    enviaInputs("MP:"+x+","+y);
                    enviaInputs("MBR:16");
                    break;
            }
            return true;
        }
    }
    private void showGetIP(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Receptor's IP:");
        final EditText input=new EditText(this);
        input.setText(IP);
        builder.setView(input);
        builder.setPositiveButton("Ok",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog,int which){
                IP=input.getText().toString();
                Display display=getWindowManager().getDefaultDisplay();
                Point size=new Point();
                display.getSize(size);
                envioInicial(size);
                loop();
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog,int which){
                dialog.cancel();
                System.exit(0);
            }
        });
        builder.show();
    }
    private void loop(){
        final Handler run=new Handler();
        run.post(new Runnable(){
            long tempoUltimoLoop=System.nanoTime();
            final long tempoOptimizado=1000000000/60;
            public void run(){
                tempoUltimoLoop=System.nanoTime();
                chamaImagens();
                TELA.invalidate();
                run.postDelayed(this,(tempoUltimoLoop-System.nanoTime()+tempoOptimizado)/1000000);
            }
        });
    }
    private void envioInicial(final Point size){
        new envioInicial().execute(size);
    }
    private void enviaInputs(final String dados){
        enviaInputs input=new enviaInputs();
        input.execute(dados);
        INPUTS.add(input);
    }
    private void chamaImagens(){
        new chamaImagens().execute();
    }
}
class envioInicial extends AsyncTask<Point,Void,Void>{
    private int portIni=12;
    protected Void doInBackground(Point... params){
        try{
            //INICIA
            Socket socket=new Socket(FullscreenActivity.IP,portIni);
            ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
            //ENVIA
            output.writeObject(params[0].x+","+params[0].y);
            //ENCERRA
            output.close();
            socket.close();
        }catch(Exception erro){}
        return null;
    }
}
class enviaInputs extends AsyncTask<String,Void,Void>{
    private int portInps=6182;
    protected Void doInBackground(String... params){
        try{
            //INICIA
            Socket socket=new Socket(FullscreenActivity.IP,portInps);
            ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
            //ENVIA
            output.writeObject(params[0]);
            //ENCERRA
            output.close();
            socket.close();
        }catch(Exception erro){}
        FullscreenActivity.INPUTS.remove(this);
        return null;
    }
}
class chamaImagens extends AsyncTask<String,Void,Void>{
    private int portImgs=11272;
    protected Void doInBackground(String... params){
        if(FullscreenActivity.INPUTS.size()!=0)return null;
        try{
            Socket socket=new Socket(FullscreenActivity.IP,portImgs);
            //INICIA
            InputStream input=socket.getInputStream();
            //RECEBE
            FullscreenActivity.TELA.setImagem(BitmapFactory.decodeStream(input));
            //ENCERRA
            input.close();
            socket.close();
        }catch(IOException erro){}
        return null;
    }
}