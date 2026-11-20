package com.chapiodara.turnodemedianoche

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.GLUtils
import android.graphics.BitmapFactory
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
    private lateinit var sounds: SoundPool
    private var ambience: MediaPlayer? = null
    private var sfxStep = 0
    private var sfxGrowl = 0
    private var sfxHit = 0
    private val handler = android.os.Handler()
    private val updater = object : Runnable { override fun run() { if (::game.isInitialized) { hud.text = game.status() }; handler.postDelayed(this, 250) } }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val root = FrameLayout(this); root.setBackgroundColor(Color.BLACK)
        val gl = GLSurfaceView(this); gl.setEGLContextClientVersion(2); game = GameRenderer(this); gl.setRenderer(game); gl.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY; root.addView(gl)
        hud = TextView(this).apply { textSize=15f; setTextColor(Color.WHITE); setShadowLayer(4f,1f,2f,Color.BLACK); text="00:00   •   0/3   •   100%"; setPadding(18,10,18,10); background=panelDrawable(0xb0182022.toInt(),16) }
        val hudLp=FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.START);hudLp.setMargins(dp(14),dp(12),0,0);root.addView(hud,hudLp)
        message = TextView(this).apply { visibility=View.GONE; text="" }
        look = LookView(this, game); val lpLook=FrameLayout.LayoutParams(-1,-1); lpLook.leftMargin=resources.displayMetrics.widthPixels/2; root.addView(look,lpLook)
        stick=StickView(this, game); val lpStick=FrameLayout.LayoutParams(dp(150),dp(150),Gravity.BOTTOM or Gravity.START);lpStick.leftMargin=dp(24);lpStick.bottomMargin=dp(22);root.addView(stick,lpStick)
        run=makeButton("CORRER",Color.rgb(92,46,24)); val lpRun=FrameLayout.LayoutParams(dp(130),dp(58),Gravity.BOTTOM or Gravity.START);lpRun.leftMargin=dp(38);lpRun.bottomMargin=dp(180);root.addView(run,lpRun);run.setOnTouchListener { _,e -> game.runningHeld=e.action!=MotionEvent.ACTION_UP && e.action!=MotionEvent.ACTION_CANCEL; true }
        flashlight=makeButton("LINTERNA",Color.rgb(120,64,22));val lpFlash=FrameLayout.LayoutParams(dp(142),dp(60),Gravity.BOTTOM or Gravity.END);lpFlash.rightMargin=dp(24);lpFlash.bottomMargin=dp(28);root.addView(flashlight,lpFlash);flashlight.setOnClickListener { game.toggleLight();flashlight.text=if(game.flashlightOn())"LUZ • ON" else "LINTERNA" }
        val start=makeButton("COMENZAR TURNO",Color.rgb(130,28,22));val lpStart=FrameLayout.LayoutParams(dp(230),dp(68),Gravity.CENTER);root.addView(start,lpStart);start.setOnClickListener { game.start();root.removeView(start) }
        val restart=makeButton("REINTENTAR",Color.rgb(130,28,22));val lpRestart=FrameLayout.LayoutParams(dp(190),dp(62),Gravity.CENTER);root.addView(restart,lpRestart);restart.visibility=View.GONE;restart.setOnClickListener { game.reset();restart.visibility=View.GONE;start.visibility=View.VISIBLE;root.addView(start,lpStart) }
        val audioAttrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        sounds=SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttrs).build()
        sfxStep=sounds.load(assets.openFd("footstep.mp3"),1);sfxGrowl=sounds.load(assets.openFd("monster_growl.mp3"),1);sfxHit=sounds.load(assets.openFd("stinger.mp3"),1)
        ambience=MediaPlayer().apply { setDataSource(assets.openFd("ambience.mp3"));isLooping=true;setVolume(.20f,.20f);prepare() }
        game.onStep={ sounds.play(sfxStep,.58f,.58f,1,0,1f) };game.onGrowl={sounds.play(sfxGrowl,.82f,.82f,2,0,1f)}
        game.onEnd = { win -> sounds.play(sfxHit,.85f,.85f,3,0,1f);runOnUiThread { restart.text=if(win)"OTRA NOCHE" else "REINTENTAR";restart.visibility=View.VISIBLE } }
        start.setOnClickListener { game.start();ambience?.start();root.removeView(start) }
        setContentView(root);handler.post(updater)
    }
    private fun dp(x:Int)= (x*resources.displayMetrics.density).toInt()
    private fun panelDrawable(color:Int,radius:Int)=GradientDrawable().apply { setColor(color);cornerRadius=dp(radius).toFloat();setStroke(dp(1),0x99b39a76.toInt()) }
    private fun makeButton(s:String,c:Int)=Button(this).apply { text=s; textSize=13f; setTextColor(Color.WHITE); background=panelDrawable(c,18); alpha=.94f; isAllCaps=false;letterSpacing=.08f;stateListAnimator=null; elevation=dp(6).toFloat() }
    override fun onPause(){super.onPause();if(::game.isInitialized) game.pause=true}
    override fun onResume(){super.onResume();if(::game.isInitialized) game.pause=false}
    override fun onDestroy(){handler.removeCallbacks(updater);ambience?.release();sounds.release();super.onDestroy()}
}

private class StickView(ctx: android.content.Context, val game: GameRenderer): View(ctx) {
    private val paint=Paint(3); @Volatile var xAxis=0f;@Volatile var yAxis=0f
    override fun onDraw(c:Canvas){super.onDraw(c);val cx=width/2f;val cy=height/2f;paint.style=Paint.Style.FILL;paint.color=0x77332b24; c.drawCircle(cx,cy,min(cx,cy)*.94f,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(2).toFloat();paint.color=0x99c9a775.toInt();c.drawCircle(cx,cy,min(cx,cy)*.94f,paint);paint.style=Paint.Style.FILL;paint.color=0xbbc9a775.toInt();c.drawCircle(cx+xAxis*width*.30f,cy+yAxis*height*.30f,min(cx,cy)*.31f,paint);paint.color=0xff27211c.toInt();c.drawCircle(cx+xAxis*width*.30f,cy+yAxis*height*.30f,min(cx,cy)*.20f,paint);paint.color=0x99e9d6b4.toInt();c.drawCircle(cx+xAxis*width*.30f-dp(4),cy+yAxis*height*.30f-dp(4),min(cx,cy)*.07f,paint)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onTouchEvent(e:MotionEvent):Boolean{val dx=(e.x-width/2f)/(width*.38f);val dy=(e.y-height/2f)/(height*.38f);val m=max(1f,sqrt(dx*dx+dy*dy));if(e.action==MotionEvent.ACTION_UP||e.action==MotionEvent.ACTION_CANCEL){xAxis=0f;yAxis=0f;game.stickX=0f;game.stickY=0f}else{xAxis=(dx/m).coerceIn(-1f,1f);yAxis=(dy/m).coerceIn(-1f,1f);game.stickX=xAxis;game.stickY=-yAxis};invalidate();return true}
}
private class LookView(ctx:android.content.Context,val game:GameRenderer):View(ctx){var px=0f;var py=0f;override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN){px=e.x;py=e.y;return true};if(e.action==MotionEvent.ACTION_MOVE){game.look((e.x-px)*.006f,(e.y-py)*.004f);px=e.x;py=e.y;return true};return true}}

private data class Part(val mesh:Mesh,val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val r:Float,val g:Float,val b:Float,val pattern:Float=0f)
private class Mesh(val data:FloatArray,val strideFloats:Int=6){ val buffer:FloatBuffer=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(data);position(0)};val count=data.size/strideFloats }
private class GameRenderer(private val context:android.content.Context): GLSurfaceView.Renderer {
    @Volatile var stickX=0f;@Volatile var stickY=0f;@Volatile var yaw=0f;@Volatile var pitch=0f;@Volatile var runningHeld=false;@Volatile var pause=false
    @Volatile private var started=false;@Volatile private var ended=false;@Volatile private var won=false;@Volatile private var flashlight=false
    private var battery=100f;private var seconds=300f;private var collected=0;private var px=0f;private var pz=-15f;private var monsterX=0f;private var monsterZ=17f;private var last=System.nanoTime();private var program=0
    var onEnd:((Boolean)->Unit)?=null;var onStep:(()->Unit)?=null;var onGrowl:(()->Unit)?=null;private var stepClock=0f;private var growlClock=0f;private var animClock=0f
    private lateinit var cube:Mesh;private lateinit var sphere:Mesh;private lateinit var creature:Mesh;private lateinit var idleMesh:Mesh;private var creatureFrames:Array<FloatArray> = emptyArray();private var idleFrames:Array<FloatArray> = emptyArray();private var texFloor=0;private var texWall=0;private var texCreature=0;private val items=mutableListOf<Part>();private val props=mutableListOf<Part>();private val view=FloatArray(16);private val proj=FloatArray(16);private val model=FloatArray(16);private val vp=FloatArray(16);private val mvp=FloatArray(16)
    override fun onSurfaceCreated(gl:GL10?,cfg:EGLConfig?){GLES20.glClearColor(.012f,.018f,.025f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);cube=makeCube();sphere=makeSphere();program=linkProgram();texFloor=loadTexture("floor_concrete.jpg");texWall=loadTexture("aged_brick.jpg");texCreature=loadTexture("werewolf.png");loadCreature();buildWorld();last=System.nanoTime()}
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,70f,w.toFloat()/max(1,h),.1f,90f)}
    fun buildWorld(){props.clear();val wall=Part(cube,0f,0f,0f,1f,1f,1f,.12f,.22f,.22f,2f);props.add(Part(cube,0f,-.28f,0f,42f,.5f,42f,.25f,.24f,.19f,1f));props.add(Part(cube,0f,5.25f,0f,42f,.25f,42f,.035f,.045f,.052f,0f))
        props.add(Part(cube,-12f,2.4f,21f,18f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,12f,2.4f,21f,18f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,0f,2.4f,-21f,42f,5f,.5f,.10f,.21f,.21f,2f));props.add(Part(cube,-21f,2.4f,0f,.5f,5f,42f,.10f,.21f,.21f,2f));props.add(Part(cube,21f,2.4f,0f,.5f,5f,42f,.10f,.21f,.21f,2f))
        // Low corridor dividers with wide gaps, so the player and pursuer can navigate.
        for(z in listOf(-12f,0f,12f)){props.add(Part(cube,-12f,1.65f,z,14f,3.3f,.35f,.10f,.18f,.18f,2f));props.add(Part(cube,12f,1.65f,z,14f,3.3f,.35f,.10f,.18f,.18f,2f))}
        for(i in 0..3){val x=if(i%2==0)-15f else 15f;val z=-12f+(i/2)*9f;props.add(Part(cube,x,1.05f,z,4f,.22f,3f,.40f,.035f,.025f,2f));for(dx in listOf(-1.6f,1.6f))for(dz in listOf(-1f,1f))props.add(Part(cube,x+dx,.50f,z+dz,.16f,1f,.16f,.19f,.22f,.20f,2f))}
        props.add(Part(cube,-4f,.2f,17f,3.8f,.4f,3f,.20f,.22f,.19f,2f));props.add(Part(cube,4f,.2f,17f,3.8f,.4f,3f,.20f,.22f,.19f,2f));props.add(Part(cube,-1.92f,2.05f,20.65f,.20f,2.05f,.24f,.58f,.055f,.025f,2f));props.add(Part(cube,1.92f,2.05f,20.65f,.20f,2.05f,.24f,.58f,.055f,.025f,2f));props.add(Part(cube,0f,4.04f,20.65f,2.1f,.18f,.24f,.58f,.055f,.025f,2f))
        // Pizzeria dressing: wall trim, service counter, booth seats, ceiling panels and menu boards.
        for(z in listOf(-18f,-6f,6f,18f)){props.add(Part(cube,-20.45f,.22f,z,.16f,.36f,2.4f,.32f,.16f,.055f,2f));props.add(Part(cube,20.45f,.22f,z,.16f,.36f,2.4f,.32f,.16f,.055f,2f))}
        for(z in listOf(-16f,-8f,0f,8f,16f)){props.add(Part(cube,0f,4.68f,z,2.3f,.09f,.58f,.78f,.57f,.31f,3f));props.add(Part(cube,0f,4.56f,z,.12f,.12f,.68f,.16f,.17f,.16f,2f))}
        for(side in listOf(-1f,1f)){val x=side*8f;props.add(Part(cube,x,1.08f,5f,3.1f,.24f,1.1f,.34f,.055f,.035f,2f));props.add(Part(cube,x,1.70f,5.9f,3.1f,1.05f,.25f,.30f,.055f,.04f,2f));props.add(Part(cube,x,1.05f,8.8f,3.2f,.25f,1.15f,.38f,.06f,.035f,2f));props.add(Part(cube,x,1.72f,8.0f,3.1f,1.0f,.24f,.30f,.055f,.04f,2f))}
        props.add(Part(cube,-7f,1.15f,14f,5f,1.25f,1.1f,.28f,.12f,.055f,2f));props.add(Part(cube,-7f,1.84f,14f,5.1f,.16f,1.2f,.53f,.32f,.13f,2f))
        props.add(Part(cube,7f,1.3f,14f,5f,1.35f,1.1f,.22f,.25f,.22f,2f));props.add(Part(cube,7f,2.0f,14f,5.1f,.15f,1.2f,.72f,.45f,.19f,3f))
        for(x in listOf(-19f,19f))for(z in listOf(-16f,-4f,8f,18f)){props.add(Part(cube,x,2.25f,z,.28f,2.5f,.28f,.28f,.22f,.14f,2f))}
        // picture/menu plaques
        for(side in listOf(-1f,1f))for(z in listOf(-7f,2f,11f)){props.add(Part(cube,side*20.68f,2.8f,z,.06f,.85f,1.25f,.48f,.12f,.045f,2f));props.add(Part(cube,side*20.60f,2.8f,z,.05f,.67f,1.02f,.18f,.32f,.22f,3f))}
        // Original character assembled from procedural geometry.
        fun sph(x:Float,y:Float,z:Float,a:Float,b:Float,c:Float,r:Float,g:Float,bl:Float,pat:Float=2f){props.add(Part(sphere,x,y,z,a,b,c,r,g,bl,pat))}
        // Monster parts are tagged separately with a stable local offset then transformed in draw.
        monsterParts.clear()
        for((ix,iz) in listOf(-3.0f to -7.0f,3.0f to 3.0f,-3.0f to 11.0f)){items.add(Part(sphere,ix,.32f,iz,.33f,.33f,.33f,.70f,.42f,.13f,3f))}
        // Exit is visibly open; collision gates it until all three fuses are collected.
    }
    private val monsterParts=mutableListOf<Part>()
    @Synchronized override fun onDrawFrame(gl:GL10?){val now=System.nanoTime();val dt=((now-last)/1e9).toFloat().coerceIn(0f,.05f);last=now;if(started&&!ended&&!pause) update(dt)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);val ex=px;val ez=pz;val eyeY=1.63f;val dx=sin(yaw)*cos(pitch);val dy=sin(pitch);val dz=cos(yaw)*cos(pitch);Matrix.setLookAtM(view,0,ex,eyeY,ez,ex+dx,eyeY+dy,ez+dz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0);GLES20.glUseProgram(program)
        for(p in props) draw(p);for(p in items)draw(p)
        val runFrame=if(hypot((px-monsterX).toDouble(),(pz-monsterZ).toDouble())<10.0)creatureFrames else idleFrames;val runMesh=if(runFrame===idleFrames)idleMesh else creature;draw(p=Part(runMesh,monsterX,0f,monsterZ,10.5f,10.5f,10.5f,.80f,.85f,.78f,4f), creatureFrame=(animClock*if(runMesh===creature)11f else 8f).toInt()%max(1,runFrame.size),frames=runFrame)
        // Flashlight cone represented by a warm point light effect shader at view forward; geometry remains visible in low ambient light.
    }
    private fun blocked(x:Float,z:Float,isPlayer:Boolean=true):Boolean{
        if(x < -20f||x>20f||z < -20f||z>20f)return true
        // Outer north wall retains a centered doorway.
        if(z>20f&&abs(x)>11.5f)return true
        for(wz in listOf(-12f,0f,12f)){
            if(abs(z-wz)<.48f){if(x in -19.6f..-4.5f||x in 4.5f..19.6f)return true}
        }
        // dining tables and fixed counter/booth zones
        for(tx in listOf(-15f,15f))for(tz in listOf(-12f,-3f))if(abs(x-tx)<2.25f&&abs(z-tz)<1.75f)return true
        for(tx in listOf(-8f,8f))if(abs(x-tx)<1.8f&&z in 3.5f..10.2f)return true
        if(x in -9.8f..-4.2f&&z in 12.4f..15.7f)return true
        if(x in 4.2f..9.8f&&z in 12.4f..15.7f)return true
        // raised stage sections leave the exit lane clear
        if(z in 15.7f..18.4f&&(x in -5.9f..-2.1f||x in 2.1f..5.9f))return true
        if(isPlayer&&z>18.6f&&abs(x)<1.65f){return collected<3}
        return false
    }
    private fun update(dt:Float){seconds-=dt;animClock+=dt;if(seconds<=0){finish(false);return};if(flashlight){battery=(battery-dt*2f).coerceAtLeast(0f);if(battery<=0)flashlight=false}
        val speed=if(runningHeld)5f else 2.8f;val mx=stickX.coerceIn(-1f,1f);val my=stickY.coerceIn(-1f,1f);val vx=(sin(yaw)*my+cos(yaw)*mx)*speed*dt;val vz=(cos(yaw)*my-sin(yaw)*mx)*speed*dt
        // Circle-vs-AABB movement with axis sliding; prevents walking through walls, booths, tables and counters.
        val nx=(px+vx).coerceIn(-20f,20f);if(!blocked(nx,pz))px=nx
        val nz=(pz+vz).coerceIn(-20f,20f);if(!blocked(px,nz))pz=nz
        if(abs(mx)+abs(my)>.12f){stepClock+=dt;if(stepClock>.56f){stepClock=0f;onStep?.invoke()}}else stepClock=0f
        // Pickups are collected by proximity.
        val iterator=items.iterator();while(iterator.hasNext()){val p=iterator.next();if(p.mesh===sphere&&p.pattern==3f&&abs(p.x)<18f&&hypot((p.x-px).toDouble(),(p.z-pz).toDouble())<1.15){iterator.remove();collected++}}
        val dx=px-monsterX;val dz=pz-monsterZ;val d=hypot(dx.toDouble(),dz.toDouble()).toFloat();if(d<1.3f){finish(false);return}
        val lit=flashlight&&d<12&&sin(yaw)*dx/d+cos(yaw)*dz/d>.92f;growlClock+=dt;if(d<12&&growlClock>7f){growlClock=0f;onGrowl?.invoke()};val pace=if(lit) 0.12f else if(d<11) 1.45f else 0.42f; if(d>0.01f){val tx=monsterX+dx/d*pace*dt;val tz=monsterZ+dz/d*pace*dt;if(!blocked(tx,monsterZ,false))monsterX=tx;if(!blocked(monsterX,tz,false))monsterZ=tz}
        if(abs(px)<2.1f&&pz>18.5f){if(collected>=3)finish(true)else pz=18.35f}
    }
    fun start(){started=true;seconds=300f;stepClock=0f;growlClock=0f};@Synchronized fun reset(){started=false;ended=false;won=false;collected=0;battery=100f;flashlight=false;px=0f;pz=-15f;monsterX=0f;monsterZ=17f;stepClock=0f;growlClock=0f;animClock=0f;items.clear();buildWorld()}
    fun finish(w:Boolean){if(ended)return;ended=true;won=w;onEnd?.invoke(w)}
    fun toggleLight(){flashlight=!flashlight&&battery>0};fun flashlightOn()=flashlight
    fun look(dx:Float,dy:Float){val pi=Math.PI.toFloat();yaw=(yaw+dx).let{if(it>pi)it-2f*pi else if(it< -pi)it+2f*pi else it};pitch=(pitch-dy).coerceIn(-.65f,.65f)}
    fun status():String="NOCHE 1   ${((300-seconds)/60).toInt().coerceAtLeast(0)}:${(seconds.toInt()%60).toString().padStart(2,'0')}     FUSIBLES $collected/3     BATERÍA ${battery.toInt()}%"
    fun objective():String=if(ended){if(won)"¡ESCAPASTE!" else "EL CAMPANERO TE ATRAPÓ"}else if(collected>=3)"¡Llega a la puerta roja del fondo!" else "Busca los 3 fusibles por el comedor y los pasillos."
    private fun draw(p:Part,creatureFrame:Int=0,frames:Array<FloatArray> = creatureFrames){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,p.x,p.y,p.z);if(p.pattern==4f){val toX=px-monsterX;val toZ=pz-monsterZ;Matrix.rotateM(model,0,Math.toDegrees(atan2(toX,toZ).toDouble()).toFloat(),0f,1f,0f);if(frames.isNotEmpty()){val frame=frames[creatureFrame%frames.size];p.mesh.buffer.clear();p.mesh.buffer.put(frame);p.mesh.buffer.position(0)}};Matrix.scaleM(model,0,p.sx,p.sy,p.sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);val ml=GLES20.glGetUniformLocation(program,"uMvp");val mo=GLES20.glGetUniformLocation(program,"uModel");GLES20.glUniformMatrix4fv(ml,1,false,mvp,0);GLES20.glUniformMatrix4fv(mo,1,false,model,0);GLES20.glUniform4f(GLES20.glGetUniformLocation(program,"uColor"),p.r,p.g,p.b,1f);GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uPattern"),p.pattern);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texFloor);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uFloorTex"),0);GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texWall);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uWallTex"),1);GLES20.glActiveTexture(GLES20.GL_TEXTURE2);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,if(p.pattern==4f)texCreature else texWall);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uCreatureTex"),2);GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uEye"),px,1.63f,pz);GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uAim"),sin(yaw)*cos(pitch),sin(pitch),cos(yaw)*cos(pitch));GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uFlash"),if(flashlight)1f else 0f);val pos=GLES20.glGetAttribLocation(program,"aPosition");val norm=GLES20.glGetAttribLocation(program,"aNormal");val uv=GLES20.glGetAttribLocation(program,"aUV");val stride=p.mesh.strideFloats*4;p.mesh.buffer.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,stride,p.mesh.buffer);p.mesh.buffer.position(3);GLES20.glEnableVertexAttribArray(norm);GLES20.glVertexAttribPointer(norm,3,GLES20.GL_FLOAT,false,stride,p.mesh.buffer);if(p.mesh.strideFloats>=8){p.mesh.buffer.position(6);GLES20.glEnableVertexAttribArray(uv);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,stride,p.mesh.buffer)};GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,p.mesh.count);GLES20.glDisableVertexAttribArray(pos);GLES20.glDisableVertexAttribArray(norm);if(p.mesh.strideFloats>=8)GLES20.glDisableVertexAttribArray(uv)}
    private fun loadTexture(file:String):Int{val ids=IntArray(1);GLES20.glGenTextures(1,ids,0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,ids[0]);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);context.assets.open(file).use{stream->val bm=BitmapFactory.decodeStream(stream);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bm,0);bm.recycle()};GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);return ids[0]}
    private fun readFrames(name:String):Array<FloatArray>{context.assets.open(name).use{input->val h=ByteArray(8);input.read(h);val bb=ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);val frames=bb.int;val verts=bb.int;return Array(frames){val bytes=ByteArray(verts*8*4);var off=0;while(off<bytes.size){val n=input.read(bytes,off,bytes.size-off);if(n<0)throw IllegalStateException("Incomplete animation asset: $name");off+=n};val f=FloatArray(verts*8);ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(f);f}}};private fun loadCreature(){creatureFrames=readFrames("werewolf_run.bin");idleFrames=readFrames("werewolf_idle.bin");creature=Mesh(creatureFrames[0],8);idleMesh=Mesh(idleFrames[0],8)}
    private fun makeCube():Mesh{val out=ArrayList<Float>();val faces=arrayOf(floatArrayOf(0f,0f,1f, -1f,-1f,1f, 1f,-1f,1f, 1f,1f,1f, -1f,-1f,1f,1f,1f,1f,-1f,1f,1f),floatArrayOf(0f,0f,-1f, 1f,-1f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f),floatArrayOf(0f,1f,0f,-1f,1f,1f,1f,1f,1f,1f,1f,-1f,-1f,1f,1f,1f,1f,-1f,-1f,1f,-1f),floatArrayOf(0f,-1f,0f,-1f,-1f,-1f,1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,1f),floatArrayOf(1f,0f,0f,1f,-1f,1f,1f,-1f,-1f,1f,1f,-1f,1f,-1f,1f,1f,1f,-1f,1f,1f,1f),floatArrayOf(-1f,0f,0f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,-1f,-1f,-1f,1f,1f,-1f,1f,-1f));for(f in faces){val n=f.copyOfRange(0,3);val idx=intArrayOf(3,4,5,0,1,2);for(i in idx){out.add(f[3+i*3]);out.add(f[4+i*3]);out.add(f[5+i*3]);out.add(n[0]);out.add(n[1]);out.add(n[2])}};return Mesh(out.toFloatArray())}
    private fun makeSphere():Mesh{val a=ArrayList<Float>();for(lat in 0 until 12){val t0=Math.PI*lat/12;val t1=Math.PI*(lat+1)/12;for(lon in 0 until 16){val p0=2*Math.PI*lon/16;val p1=2*Math.PI*(lon+1)/16;val pts=arrayOf(point(t0,p0),point(t1,p0),point(t1,p1),point(t0,p0),point(t1,p1),point(t0,p1));for(v in pts){a.add(v[0]);a.add(v[1]);a.add(v[2]);a.add(v[0]);a.add(v[1]);a.add(v[2])}}};return Mesh(a.toFloatArray())}
    private fun point(t:Double,p:Double)=floatArrayOf((sin(t)*cos(p)).toFloat(),cos(t).toFloat(),(sin(t)*sin(p)).toFloat())
    private fun linkProgram():Int{val vs="""attribute vec3 aPosition;attribute vec3 aNormal;attribute vec2 aUV;uniform mat4 uMvp;uniform mat4 uModel;varying vec3 vWorld;varying vec3 vNorm;varying vec2 vUV;void main(){vec4 w=uModel*vec4(aPosition,1.0);vWorld=w.xyz;vNorm=mat3(uModel)*aNormal;vUV=aUV;gl_Position=uMvp*vec4(aPosition,1.0);}""";val fs="""precision mediump float;uniform vec4 uColor;uniform float uPattern;uniform sampler2D uFloorTex;uniform sampler2D uWallTex;uniform sampler2D uCreatureTex;varying vec2 vUV;uniform vec3 uEye;uniform vec3 uAim;uniform float uFlash;varying vec3 vWorld;varying vec3 vNorm;void main(){float light=0.34+0.56*max(dot(normalize(vNorm),normalize(vec3(-0.4,0.85,0.32))),0.0);vec3 ray=normalize(vWorld-uEye);float aimDot=dot(ray,normalize(uAim));float cone=1.0-smoothstep(0.925,0.975,aimDot);float reach=1.0-smoothstep(2.0,17.0,length(vWorld-uEye));float beam=uFlash*cone*reach*max(dot(normalize(vNorm),normalize(uEye-vWorld)),0.18);float pat=1.0;vec3 texColor=vec3(1.0);if(uPattern<1.5){texColor=texture2D(uFloorTex,vWorld.xz*0.20).rgb;pat=0.88;}else if(uPattern<2.5){vec3 n=abs(normalize(vNorm));vec3 tx=texture2D(uWallTex,vWorld.zy*0.18).rgb*n.x+texture2D(uWallTex,vWorld.xy*0.18).rgb*n.z+texture2D(uWallTex,vWorld.xz*0.18).rgb*n.y;texColor=tx*vec3(0.66,0.79,0.76);pat=0.95;}else if(uPattern<3.5){pat=1.12;}else{texColor=texture2D(uCreatureTex,vUV).rgb;pat=1.25;}gl_FragColor=vec4(uColor.rgb*texColor*light*pat+vec3(1.0,0.72,0.40)*beam*1.65+uColor.rgb*step(2.5,uPattern)*0.35,uColor.a);}""";fun shader(type:Int,src:String):Int{val s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);val ok=IntArray(1);GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw RuntimeException("Shader: "+GLES20.glGetShaderInfoLog(s));return s};val p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(p);val ok=IntArray(1);GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw RuntimeException("Link: "+GLES20.glGetProgramInfoLog(p));return p}
}
