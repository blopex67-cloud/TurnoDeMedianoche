package com.chapiodara.turnodemedianoche

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
    private lateinit var game: GameRenderer
    private lateinit var hud: TextView
    private lateinit var message: TextView
    private lateinit var stick: StickView
    private lateinit var look: LookView
    private lateinit var flashlight: Button
    private lateinit var run: Button
    private val handler = android.os.Handler()
    private val updater = object : Runnable { override fun run() { if (::game.isInitialized) { hud.text = game.status(); message.text = game.objective() }; handler.postDelayed(this, 250) } }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val root = FrameLayout(this); root.setBackgroundColor(Color.BLACK)
        val gl = GLSurfaceView(this); gl.setEGLContextClientVersion(2); game = GameRenderer(); gl.setRenderer(game); gl.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY; root.addView(gl)
        hud = TextView(this).apply { textSize=17f; setTextColor(Color.WHITE); setShadowLayer(5f,1f,2f,Color.BLACK); text="NOCHE 1"; setPadding(18,12,12,8) }
        root.addView(hud, FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.START))
        message = TextView(this).apply { textSize=17f; setTextColor(Color.rgb(255,205,130)); gravity=Gravity.CENTER; setShadowLayer(5f,1f,1f,Color.BLACK); text="" }
        root.addView(message, FrameLayout.LayoutParams(-1,64,Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        look = LookView(this, game); val lpLook=FrameLayout.LayoutParams(-1,-1); lpLook.leftMargin=resources.displayMetrics.widthPixels/2; root.addView(look,lpLook)
        stick=StickView(this, game); val lpStick=FrameLayout.LayoutParams(dp(150),dp(150),Gravity.BOTTOM or Gravity.START);lpStick.leftMargin=dp(24);lpStick.bottomMargin=dp(22);root.addView(stick,lpStick)
        run=makeButton("CORRER",Color.rgb(92,46,24)); val lpRun=FrameLayout.LayoutParams(dp(130),dp(58),Gravity.BOTTOM or Gravity.START);lpRun.leftMargin=dp(38);lpRun.bottomMargin=dp(180);root.addView(run,lpRun);run.setOnTouchListener { _,e -> game.runningHeld=e.action!=MotionEvent.ACTION_UP && e.action!=MotionEvent.ACTION_CANCEL; true }
        flashlight=makeButton("LINTERNA",Color.rgb(120,64,22));val lpFlash=FrameLayout.LayoutParams(dp(142),dp(60),Gravity.BOTTOM or Gravity.END);lpFlash.rightMargin=dp(24);lpFlash.bottomMargin=dp(28);root.addView(flashlight,lpFlash);flashlight.setOnClickListener { game.toggleLight() }
        val start=makeButton("COMENZAR TURNO",Color.rgb(130,28,22));val lpStart=FrameLayout.LayoutParams(dp(230),dp(68),Gravity.CENTER);root.addView(start,lpStart);start.setOnClickListener { game.start();root.removeView(start) }
        val restart=makeButton("REINTENTAR",Color.rgb(130,28,22));val lpRestart=FrameLayout.LayoutParams(dp(190),dp(62),Gravity.CENTER);root.addView(restart,lpRestart);restart.visibility=View.GONE;restart.setOnClickListener { game.reset();restart.visibility=View.GONE;start.visibility=View.VISIBLE;root.addView(start,lpStart) }
        game.onEnd = { win -> runOnUiThread { restart.text=if(win)"JUGAR DE NUEVO" else "REINTENTAR";restart.visibility=View.VISIBLE } }
        setContentView(root);handler.post(updater)
    }
    private fun dp(x:Int)= (x*resources.displayMetrics.density).toInt()
    private fun makeButton(s:String,c:Int)=Button(this).apply { text=s; textSize=14f; setTextColor(Color.WHITE); setBackgroundColor(c); alpha=.88f; isAllCaps=false }
    override fun onPause(){super.onPause();if(::game.isInitialized) game.pause=true}
    override fun onResume(){super.onResume();if(::game.isInitialized) game.pause=false}
    override fun onDestroy(){handler.removeCallbacks(updater);super.onDestroy()}
}

private class StickView(ctx: android.content.Context, val game: GameRenderer): View(ctx) {
    private val paint=Paint(3); @Volatile var xAxis=0f;@Volatile var yAxis=0f
    override fun onDraw(c:Canvas){super.onDraw(c);val cx=width/2f;val cy=height/2f;paint.color=0x77444444.toInt(); c.drawCircle(cx,cy,min(cx,cy)*.95f,paint);paint.color=0xbbdddddd.toInt();c.drawCircle(cx+xAxis*width*.30f,cy+yAxis*height*.30f,min(cx,cy)*.32f,paint)}
    override fun onTouchEvent(e:MotionEvent):Boolean{val dx=(e.x-width/2f)/(width*.38f);val dy=(e.y-height/2f)/(height*.38f);val m=max(1f,sqrt(dx*dx+dy*dy));if(e.action==MotionEvent.ACTION_UP||e.action==MotionEvent.ACTION_CANCEL){xAxis=0f;yAxis=0f;game.stickX=0f;game.stickY=0f}else{xAxis=(dx/m).coerceIn(-1f,1f);yAxis=-(dy/m).coerceIn(-1f,1f);game.stickX=xAxis;game.stickY=yAxis};invalidate();return true}
}
private class LookView(ctx:android.content.Context,val game:GameRenderer):View(ctx){var px=0f;var py=0f;override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN){px=e.x;py=e.y;return true};if(e.action==MotionEvent.ACTION_MOVE){game.look((e.x-px)*.006f,(e.y-py)*.004f);px=e.x;py=e.y;return true};return true}}

private data class Part(val mesh:Mesh,val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val r:Float,val g:Float,val b:Float,val pattern:Float=0f)
private class Mesh(val data:FloatArray){ val buffer:FloatBuffer=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(data);position(0)};val count=data.size/6 }
private class GameRenderer: GLSurfaceView.Renderer {
    @Volatile var stickX=0f;@Volatile var stickY=0f;@Volatile var yaw=0f;@Volatile var pitch=0f;@Volatile var runningHeld=false;@Volatile var pause=false
    @Volatile private var started=false;@Volatile private var ended=false;@Volatile private var won=false;@Volatile private var flashlight=false
    private var battery=100f;private var seconds=300f;private var collected=0;private var px=0f;private var pz=-15f;private var monsterX=0f;private var monsterZ=17f;private var last=System.nanoTime();private var program=0
    var onEnd:((Boolean)->Unit)?=null
    private lateinit var cube:Mesh;private lateinit var sphere:Mesh;private val items=mutableListOf<Part>();private val props=mutableListOf<Part>();private val view=FloatArray(16);private val proj=FloatArray(16);private val model=FloatArray(16);private val vp=FloatArray(16);private val mvp=FloatArray(16)
    override fun onSurfaceCreated(gl:GL10?,cfg:EGLConfig?){GLES20.glClearColor(.012f,.018f,.025f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);cube=makeCube();sphere=makeSphere();program=linkProgram();buildWorld();last=System.nanoTime()}
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,70f,w.toFloat()/max(1,h),.1f,90f)}
    fun buildWorld(){props.clear();val wall=Part(cube,0f,0f,0f,1f,1f,1f,.12f,.22f,.22f,2f);props.add(Part(cube,0f,-.28f,0f,42f,.5f,42f,.25f,.24f,.19f,1f));props.add(Part(cube,0f,5.25f,0f,42f,.25f,42f,.035f,.045f,.052f,0f))
        props.add(Part(cube,-12f,2.4f,21f,18f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,12f,2.4f,21f,18f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,0f,2.4f,-21f,42f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,-21f,2.4f,0f,.5f,5f,42f,.10f,.21f,.21f,2f));props.add(Part(cube,21f,2.4f,0f,.5f,5f,42f,.10f,.21f,.21f,2f))
        // Low corridor dividers with wide gaps, so the player and pursuer can navigate.
        for(z in listOf(-12f,0f,12f)){props.add(Part(cube,-12f,1.65f,z,14f,3.3f,.35f,.10f,.18f,.18f,2f));props.add(Part(cube,12f,1.65f,z,14f,3.3f,.35f,.10f,.18f,.18f,2f))}
        for(i in 0..3){val x=if(i%2==0)-15f else 15f;val z=-12f+(i/2)*9f;props.add(Part(cube,x,1.05f,z,4f,.22f,3f,.40f,.035f,.025f,2f));for(dx in listOf(-1.6f,1.6f))for(dz in listOf(-1f,1f))props.add(Part(cube,x+dx,.50f,z+dz,.16f,1f,.16f,.19f,.22f,.20f,2f))}
        props.add(Part(cube,0f,.2f,17f,11f,.4f,3f,.20f,.22f,.19f,2f));props.add(Part(cube,0f,2.2f,20.65f,3.2f,4f,.35f,.44f,.045f,.025f,2f))
        // Original character assembled from procedural geometry.
        fun sph(x:Float,y:Float,z:Float,a:Float,b:Float,c:Float,r:Float,g:Float,bl:Float,pat:Float=2f){props.add(Part(sphere,x,y,z,a,b,c,r,g,bl,pat))}
        // Monster parts are tagged separately with a stable local offset then transformed in draw.
        monsterParts.clear();fun mp(y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float){monsterParts.add(Part(sphere,0f,y,z,sx,sy,sz,r,g,b,2f))}
        mp(1.3f,0f,1.0f,1.2f,.65f,.20f,.24f,.23f);mp(2.35f,.02f,1.12f,1.0f,.85f,.24f,.28f,.25f);mp(3.0f,0f,.30f,.44f,.27f,.45f,.29f,.08f);mp(3f,.0f,.30f,.44f,.27f,.45f,.29f,.08f)
        mp(2.08f,.47f,.56f,.32f,.38f,.03f,.035f,.035f);mp(1.82f,.43f,.58f,.15f,.3f,.47f,.30f,.10f)
        mp(1.78f,0f,.75f,.33f,.4f,.42f,.30f,.10f);mp(1.18f,0f,.31f,.72f,.32f,.2f,.24f,.23f);mp(.40f,0f,.62f,.67f,.36f,.18f,.21f,.2f);mp(.14f,.16f,.8f,.2f,.55f,.025f,.03f,.03f)
        // Eyes glow in the dark
        monsterParts.add(Part(sphere,-.25f,2.43f,.48f,.13f,.15f,.11f,1f,.16f,.018f,3f));monsterParts.add(Part(sphere,.25f,2.43f,.48f,.13f,.15f,.11f,1f,.16f,.018f,3f))
        for(i in 0..2){val ix=if(i==0)-15f else if(i==1)15f else -14f;val iz=if(i==0)-7f else if(i==1)-4f else 8f;items.add(Part(sphere,ix,.32f,iz,.33f,.33f,.33f,.70f,.42f,.13f,3f))}
        items.add(Part(cube,0f,2f,20.35f,3.3f,4f,.4f,.55f,.055f,.03f,2f))
    }
    private val monsterParts=mutableListOf<Part>()
    @Synchronized override fun onDrawFrame(gl:GL10?){val now=System.nanoTime();val dt=((now-last)/1e9).toFloat().coerceIn(0f,.05f);last=now;if(started&&!ended&&!pause) update(dt)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);val ex=px;val ez=pz;val eyeY=1.63f;val dx=sin(yaw)*cos(pitch);val dy=sin(pitch);val dz=cos(yaw)*cos(pitch);Matrix.setLookAtM(view,0,ex,eyeY,ez,ex+dx,eyeY+dy,ez+dz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0);GLES20.glUseProgram(program)
        for(p in props) draw(p);for(p in items)draw(p)
        val bob=sin(now/220_000_000.0).toFloat()*.035f;for(p in monsterParts)draw(p.copy(x=p.x+monsterX,y=p.y+bob,z=p.z+monsterZ))
        // Flashlight cone represented by a warm point light effect shader at view forward; geometry remains visible in low ambient light.
    }
    private fun update(dt:Float){seconds-=dt;if(seconds<=0){finish(false);return};if(flashlight){battery=(battery-dt*2f).coerceAtLeast(0f);if(battery<=0)flashlight=false}
        val speed=if(runningHeld)5f else 2.8f;val mx=stickX.coerceIn(-1f,1f);val my=stickY.coerceIn(-1f,1f);val vx=(sin(yaw)*my+cos(yaw)*mx)*speed*dt;val vz=(cos(yaw)*my-sin(yaw)*mx)*speed*dt
        px=(px+vx).coerceIn(-19f,19f);pz=(pz+vz).coerceIn(-19f,19f)
        // Pickups are collected by proximity.
        val iterator=items.iterator();while(iterator.hasNext()){val p=iterator.next();if(p.mesh===sphere&&p.pattern==3f&&abs(p.x)<18f&&hypot((p.x-px).toDouble(),(p.z-pz).toDouble())<1.15){iterator.remove();collected++}}
        val dx=px-monsterX;val dz=pz-monsterZ;val d=hypot(dx.toDouble(),dz.toDouble()).toFloat();if(d<1.3f){finish(false);return}
        val lit=flashlight&&d<12&&sin(yaw)*dx/d+cos(yaw)*dz/d>.92f;val pace=if(lit) 0.12f else if(d<11) 1.45f else 0.42f; if(d>0.01f){monsterX+=dx/d*pace*dt;monsterZ+=dz/d*pace*dt}
        if(abs(px)<2.1f&&pz>18.5f){if(collected>=3)finish(true)else pz=18.35f}
    }
    fun start(){started=true;seconds=300f};@Synchronized fun reset(){started=false;ended=false;won=false;collected=0;battery=100f;flashlight=false;px=0f;pz=-15f;monsterX=0f;monsterZ=17f;items.clear();buildWorld()}
    fun finish(w:Boolean){if(ended)return;ended=true;won=w;onEnd?.invoke(w)}
    fun toggleLight(){flashlight=!flashlight&&battery>0}
    fun look(dx:Float,dy:Float){val pi=Math.PI.toFloat();yaw=(yaw+dx).let{if(it>pi)it-2f*pi else if(it< -pi)it+2f*pi else it};pitch=(pitch-dy).coerceIn(-.65f,.65f)}
    fun status():String="NOCHE 1   ${((300-seconds)/60).toInt().coerceAtLeast(0)}:${(seconds.toInt()%60).toString().padStart(2,'0')}     FUSIBLES $collected/3     BATERÍA ${battery.toInt()}%"
    fun objective():String=if(ended){if(won)"¡ESCAPASTE!" else "EL CAMPANERO TE ATRAPÓ"}else if(collected>=3)"¡Llega a la puerta roja del fondo!" else "Busca los 3 fusibles por el comedor y los pasillos."
    private fun draw(p:Part){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,p.x,p.y,p.z);Matrix.scaleM(model,0,p.sx,p.sy,p.sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);val ml=GLES20.glGetUniformLocation(program,"uMvp");val mo=GLES20.glGetUniformLocation(program,"uModel");GLES20.glUniformMatrix4fv(ml,1,false,mvp,0);GLES20.glUniformMatrix4fv(mo,1,false,model,0);GLES20.glUniform4f(GLES20.glGetUniformLocation(program,"uColor"),p.r,p.g,p.b,1f);GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uPattern"),p.pattern);GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uEye"),px,1.63f,pz);GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uAim"),sin(yaw)*cos(pitch),sin(pitch),cos(yaw)*cos(pitch));GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uFlash"),if(flashlight)1f else 0f);val pos=GLES20.glGetAttribLocation(program,"aPosition");val norm=GLES20.glGetAttribLocation(program,"aNormal");p.mesh.buffer.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,24,p.mesh.buffer);p.mesh.buffer.position(3);GLES20.glEnableVertexAttribArray(norm);GLES20.glVertexAttribPointer(norm,3,GLES20.GL_FLOAT,false,24,p.mesh.buffer);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,p.mesh.count);GLES20.glDisableVertexAttribArray(pos);GLES20.glDisableVertexAttribArray(norm)}
    private fun makeCube():Mesh{val out=ArrayList<Float>();val faces=arrayOf(floatArrayOf(0f,0f,1f, -1f,-1f,1f, 1f,-1f,1f, 1f,1f,1f, -1f,-1f,1f,1f,1f,1f,-1f,1f,1f),floatArrayOf(0f,0f,-1f, 1f,-1f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f),floatArrayOf(0f,1f,0f,-1f,1f,1f,1f,1f,1f,1f,1f,-1f,-1f,1f,1f,1f,1f,-1f,-1f,1f,-1f),floatArrayOf(0f,-1f,0f,-1f,-1f,-1f,1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,1f),floatArrayOf(1f,0f,0f,1f,-1f,1f,1f,-1f,-1f,1f,1f,-1f,1f,-1f,1f,1f,1f,-1f,1f,1f,1f),floatArrayOf(-1f,0f,0f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,-1f,-1f,-1f,1f,1f,-1f,1f,-1f));for(f in faces){val n=f.copyOfRange(0,3);val idx=intArrayOf(3,4,5,0,1,2);for(i in idx){out.add(f[3+i*3]);out.add(f[4+i*3]);out.add(f[5+i*3]);out.add(n[0]);out.add(n[1]);out.add(n[2])}};return Mesh(out.toFloatArray())}
    private fun makeSphere():Mesh{val a=ArrayList<Float>();for(lat in 0 until 12){val t0=Math.PI*lat/12;val t1=Math.PI*(lat+1)/12;for(lon in 0 until 16){val p0=2*Math.PI*lon/16;val p1=2*Math.PI*(lon+1)/16;val pts=arrayOf(point(t0,p0),point(t1,p0),point(t1,p1),point(t0,p0),point(t1,p1),point(t0,p1));for(v in pts){a.add(v[0]);a.add(v[1]);a.add(v[2]);a.add(v[0]);a.add(v[1]);a.add(v[2])}}};return Mesh(a.toFloatArray())}
    private fun point(t:Double,p:Double)=floatArrayOf((sin(t)*cos(p)).toFloat(),cos(t).toFloat(),(sin(t)*sin(p)).toFloat())
    private fun linkProgram():Int{val vs="""attribute vec3 aPosition;attribute vec3 aNormal;uniform mat4 uMvp;uniform mat4 uModel;varying vec3 vWorld;varying vec3 vNorm;void main(){vec4 w=uModel*vec4(aPosition,1.0);vWorld=w.xyz;vNorm=mat3(uModel)*aNormal;gl_Position=uMvp*vec4(aPosition,1.0);}""";val fs="""precision mediump float;uniform vec4 uColor;uniform float uPattern;uniform vec3 uEye;uniform vec3 uAim;uniform float uFlash;varying vec3 vWorld;varying vec3 vNorm;void main(){float light=0.34+0.66*max(dot(normalize(vNorm),normalize(vec3(-0.4,0.85,0.32))),0.0);vec3 ray=normalize(vWorld-uEye);float cone=step(0.90,dot(ray,normalize(uAim)));float reach=1.0-smoothstep(3.0,20.0,length(vWorld-uEye));float beam=uFlash*cone*reach;float pat=1.0;if(uPattern<1.5){float tile=mod(floor(vWorld.x*0.5)+floor(vWorld.z*0.5),2.0);pat=mix(0.67,1.08,tile);float grit=fract(sin(dot(floor(vWorld.xz*8.0),vec2(12.9898,78.233)))*43758.5453);pat*=0.92+grit*0.12;}else if(uPattern<2.5){float seam=step(0.94,fract(vWorld.y*1.3))*0.18;float grit=fract(sin(dot(floor(vWorld.xz*9.0),vec2(17.1,31.7)))*15731.9);pat=0.79+grit*0.17-seam;}else if(uPattern>2.5){pat=1.22;}gl_FragColor=vec4(uColor.rgb*light*pat+vec3(1.0,0.72,0.40)*beam*0.82,uColor.a);}""";fun shader(type:Int,src:String):Int{val s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);val ok=IntArray(1);GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw RuntimeException("Shader: "+GLES20.glGetShaderInfoLog(s));return s};val p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(p);val ok=IntArray(1);GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw RuntimeException("Link: "+GLES20.glGetProgramInfoLog(p));return p}
}
