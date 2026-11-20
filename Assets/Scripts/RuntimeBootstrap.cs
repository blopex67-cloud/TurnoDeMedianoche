using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections.Generic;

public class RuntimeBootstrap : MonoBehaviour
{
    public static RuntimeBootstrap I;
    public Transform player, monster;
    public Camera playerCam;
    public CharacterController controller;
    public Text hud, objective;
    public GameObject titlePanel, endPanel;
    public Text endText;
    public int parts;
    public bool running, ended, flashlightOn;
    public float nightSeconds = 300f, battery = 100f;
    Light torch;
    readonly List<Material> mats = new List<Material>();

    void Awake(){ I=this; Application.targetFrameRate=60; Screen.orientation=ScreenOrientation.LandscapeLeft; }
    void Start(){ BuildScene(); BuildUI(); }
    Material Mat(string name, Color c, bool patterned=false){
        Shader s=Shader.Find("Standard"); if(s==null)s=Shader.Find("Universal Render Pipeline/Lit");
        var m=new Material(s); m.name=name; m.color=c;
        if(patterned){ Texture2D t=new Texture2D(64,64,TextureFormat.RGB24,false); t.wrapMode=TextureWrapMode.Repeat; var r=new System.Random(name.GetHashCode());
            for(int y=0;y<64;y++)for(int x=0;x<64;x++){float n=(float)r.NextDouble()*.12f; bool seam=(x%16<2||y%16<2); Color q=c*(seam?.55f:.86f+n); t.SetPixel(x,y,q);} t.Apply(); m.mainTexture=t; m.mainTextureScale=new Vector2(4,4); }
        mats.Add(m); return m;
    }
    GameObject Box(string name, Vector3 p, Vector3 scale, Material m, bool solid=true){var g=GameObject.CreatePrimitive(PrimitiveType.Cube);g.name=name;g.transform.position=p;g.transform.localScale=scale;g.GetComponent<Renderer>().sharedMaterial=m;if(!solid)Destroy(g.GetComponent<Collider>());return g;}
    GameObject Sphere(string name, Transform parent, Vector3 local, Vector3 scale, Material m){var g=GameObject.CreatePrimitive(PrimitiveType.Sphere);g.name=name;g.transform.SetParent(parent,false);g.transform.localPosition=local;g.transform.localScale=scale;g.GetComponent<Renderer>().sharedMaterial=m;return g;}
    void BuildScene(){
        var floor=Mat("Tile • midnight",new Color(.19f,.20f,.20f),true); var wall=Mat("Paint • faded teal",new Color(.12f,.24f,.24f),true); var red=Mat("Emergency red",new Color(.42f,.035f,.025f)); var metal=Mat("Oxidized metal",new Color(.16f,.20f,.19f),true); var gold=Mat("Dirty brass",new Color(.45f,.30f,.11f)); var black=Mat("Rubber",new Color(.018f,.024f,.026f)); var glow=Mat("Amber eyes",new Color(1,.20f,.018f)); glow.EnableKeyword("_EMISSION");glow.SetColor("_EmissionColor",new Color(1,.13f,.01f)*2.5f);
        Box("Piso",new Vector3(0,-.25f,0),new Vector3(42,.5f,42),floor);
        Box("Techo",new Vector3(0,5.2f,0),new Vector3(42,.3f,42),black);
        Box("Muro norte izq",new Vector3(-12,2.45f,21),new Vector3(18,5,.5f),wall); Box("Muro norte der",new Vector3(12,2.45f,21),new Vector3(18,5,.5f),wall); Box("Muro sur",new Vector3(0,2.45f,-21),new Vector3(42,5,.5f),wall);
        Box("Muro este",new Vector3(21,2.45f,0),new Vector3(.5f,5,42),wall); Box("Muro oeste",new Vector3(-21,2.45f,0),new Vector3(.5f,5,42),wall);
        // Partitions make a readable route while preserving a central passage.
        for(int z=-12;z<=12;z+=12){Box("Tabique izq",new Vector3(-11,2.2f,z),new Vector3(12,4.4f,.35f),wall);Box("Tabique der",new Vector3(11,2.2f,z),new Vector3(12,4.4f,.35f),wall);}
        for(int z=-17;z<19;z+=6){Box("Tubo de techo",new Vector3(0,4.65f,z),new Vector3(1.2f,.12f,.18f),metal,false);}
        for(int i=0;i<7;i++){float z=-15+i*5; var lamp= new GameObject("Luz fluorescente");lamp.transform.position=new Vector3(i%2==0?-7:7,4.3f,z);var l=lamp.AddComponent<Light>();l.type=LightType.Point;l.range=8;l.intensity=.8f;l.color=i%3==0?new Color(.65f,.78f,1):new Color(1,.68f,.38f);}
        // Tables and a stage at the far end.
        for(int i=0;i<4;i++){float x=i%2==0?-15:15,z=-12+(i/2)*9;Box("Mesa comedor",new Vector3(x,1.15f,z),new Vector3(4,.25f,3),red);foreach(float dx in new float[]{-1.6f,1.6f})foreach(float dz in new float[]{-1,1})Box("Pata",new Vector3(x+dx,.55f,z+dz),new Vector3(.18f,1.1f,.18f),metal);}
        Box("Tarima",new Vector3(0,.25f,17),new Vector3(11,.5f,3),metal); Box("Letrero",new Vector3(0,3.5f,20.55f),new Vector3(8,1.2f,.18f),red,false);
        player=new GameObject("Jugador").transform;player.position=new Vector3(0,0,-16);controller=player.gameObject.AddComponent<CharacterController>();controller.height=1.8f;controller.radius=.32f;controller.center=new Vector3(0,.9f,0);player.gameObject.AddComponent<PlayerMotor>();playerCam=new GameObject("Camara jugador").AddComponent<Camera>();playerCam.transform.SetParent(player);playerCam.transform.localPosition=new Vector3(0,1.55f,0);playerCam.fieldOfView=72;playerCam.farClipPlane=55;playerCam.backgroundColor=new Color(.015f,.02f,.027f);playerCam.clearFlags=CameraClearFlags.SolidColor;
        var tg=new GameObject("Linterna");tg.transform.SetParent(playerCam.transform);tg.transform.localPosition=Vector3.zero;tg.transform.localRotation=Quaternion.identity;torch=tg.AddComponent<Light>();torch.type=LightType.Spot;torch.range=19;torch.spotAngle=52;torch.intensity=4.2f;torch.color=new Color(1,.83f,.58f);torch.enabled=false;
        // Original, procedural character: the Rusted Bell Keeper.
        monster=new GameObject("El Campanero").transform;monster.position=new Vector3(0,0,18);BuildMonster(monster,metal,gold,black,glow);monster.gameObject.AddComponent<MonsterAI>();
        Vector3[] spots={new Vector3(-15,0,-7),new Vector3(15,0,-4),new Vector3(-14,0,8)}; for(int i=0;i<spots.Length;i++){var k=Sphere("Fusible antiguo",null,Vector3.zero,new Vector3(.33f,.33f,.33f),gold);k.transform.position=spots[i];k.GetComponent<Collider>().isTrigger=true;var pick=k.AddComponent<Collectible>();pick.index=i;}
        var exit=Box("Puerta de salida",new Vector3(0,2,20.55f),new Vector3(3.4f,4,.45f),red);exit.GetComponent<Collider>().isTrigger=true;exit.AddComponent<ExitDoor>();
        RenderSettings.ambientLight=new Color(.15f,.17f,.2f);RenderSettings.fog=true;RenderSettings.fogColor=new Color(.025f,.03f,.035f);RenderSettings.fogMode=FogMode.Linear;RenderSettings.fogStartDistance=18;RenderSettings.fogEndDistance=42;
    }
    void BuildMonster(Transform root,Material metal,Material brass,Material black,Material eyes){
        Sphere("Cuerpo",root,new Vector3(0,1.25f,0),new Vector3(1.05f,1.25f,.65f),metal);Sphere("Cabeza",root,new Vector3(0,2.35f,.03f),new Vector3(1.15f,1.05f,.9f),metal);
        Sphere("Oreja izq",root,new Vector3(-.48f,2.95f,0),new Vector3(.32f,.48f,.28f),brass);Sphere("Oreja der",root,new Vector3(.48f,2.95f,0),new Vector3(.32f,.48f,.28f),brass);
        Sphere("Hocico",root,new Vector3(0,2.1f,.49f),new Vector3(.58f,.36f,.42f),black);Sphere("Mandibula",root,new Vector3(0,1.82f,.48f),new Vector3(.62f,.18f,.35f),brass);
        foreach(float x in new float[]{-.27f,.27f}){Sphere("Ojo encendido",root,new Vector3(x,2.43f,.46f),new Vector3(.13f,.17f,.1f),eyes);Sphere("Pupila",root,new Vector3(x,2.43f,.55f),new Vector3(.055f,.08f,.035f),black);}
        foreach(float x in new float[]{-.72f,.72f}){Sphere("Hombro",root,new Vector3(x,1.75f,0),new Vector3(.38f,.4f,.4f),brass);Sphere("Brazo",root,new Vector3(x*1.28f,1.25f,.02f),new Vector3(.27f,.72f,.28f),metal);Sphere("Mano",root,new Vector3(x*1.35f,.76f,.12f),new Vector3(.32f,.27f,.32f),black);}
        foreach(float x in new float[]{-.32f,.32f}){Sphere("Pierna",root,new Vector3(x,.42f,0),new Vector3(.32f,.75f,.35f),metal);Sphere("Bota",root,new Vector3(x,.12f,.2f),new Vector3(.43f,.22f,.62f),black);}
        Sphere("Campana",root,new Vector3(0,3.25f,.02f),new Vector3(.27f,.27f,.27f),brass);
    }
    Font GetFont(){return Resources.GetBuiltinResource<Font>("Arial.ttf");}
    void BuildUI(){
        if(FindObjectOfType<EventSystem>()==null)new GameObject("EventSystem",typeof(EventSystem),typeof(StandaloneInputModule));
        var cg=new GameObject("HUD").AddComponent<Canvas>();cg.renderMode=RenderMode.ScreenSpaceOverlay;cg.gameObject.AddComponent<CanvasScaler>().uiScaleMode=CanvasScaler.ScaleMode.ScaleWithScreenSize;cg.gameObject.GetComponent<CanvasScaler>().referenceResolution=new Vector2(1280,720);cg.gameObject.AddComponent<GraphicRaycaster>();
        hud=Label(cg.transform,"",26,TextAnchor.UpperLeft,new Vector2(0,1),new Vector2(0,1),new Vector2(18,-18),new Vector2(650,90),Color.white);
        objective=Label(cg.transform,"",24,TextAnchor.UpperCenter,new Vector2(.5f,1),new Vector2(.5f,1),new Vector2(0,-20),new Vector2(700,48),new Color(1,.8f,.47f));
        // Mobile move pad and controls.
        var pad=Button(cg.transform,"MOVER",new Vector2(.12f,.19f),new Vector2(150,130),new Color(.08f,.10f,.12f,.62f));var stick=pad.gameObject.AddComponent<MobileStick>();
        Button(cg.transform,"MIRAR",new Vector2(.82f,.18f),new Vector2(145,90),new Color(.08f,.10f,.12f,.58f)).gameObject.AddComponent<LookTouch>();
        var f=Button(cg.transform,"LINTERNA",new Vector2(.91f,.4f),new Vector2(145,72),new Color(.48f,.24f,.05f,.85f));f.onClick.AddListener(ToggleTorch);
        var run=Button(cg.transform,"CORRER",new Vector2(.12f,.38f),new Vector2(130,62),new Color(.25f,.12f,.06f,.8f));run.gameObject.AddComponent<RunTouch>();
        titlePanel=Panel(cg.transform,"TURNO DE MEDIANOCHE\n\nReúne los 3 fusibles y escapa antes del amanecer.\nEl Campanero oye tus pasos.","COMENZAR TURNO",()=>{running=true;titlePanel.SetActive(false);});
        endPanel=Panel(cg.transform,"","VOLVER A JUGAR",()=>UnityEngine.SceneManagement.SceneManager.LoadScene(UnityEngine.SceneManagement.SceneManager.GetActiveScene().buildIndex));endPanel.SetActive(false);endText=endPanel.GetComponentInChildren<Text>();
    }
    Text Label(Transform parent,string txt,int size,TextAnchor anchor,Vector2 amin,Vector2 amax,Vector2 pos,Vector2 dim,Color color){var g=new GameObject("Text");g.transform.SetParent(parent,false);var r=g.AddComponent<RectTransform>();r.anchorMin=amin;r.anchorMax=amax;r.pivot=new Vector2(amin.x==amax.x?amin.x:.5f,amin.y==amax.y?amin.y:.5f);r.anchoredPosition=pos;r.sizeDelta=dim;var t=g.AddComponent<Text>();t.font=GetFont();t.text=txt;t.fontSize=size;t.alignment=anchor;t.color=color;t.horizontalOverflow=HorizontalWrapMode.Wrap;return t;}
    Button Button(Transform p,string text,Vector2 anchor,Vector2 size,Color color){var g=new GameObject(text);g.transform.SetParent(p,false);var r=g.AddComponent<RectTransform>();r.anchorMin=r.anchorMax=anchor;r.pivot=anchor;r.anchoredPosition=Vector2.zero;r.sizeDelta=size;var im=g.AddComponent<Image>();im.color=color;var b=g.AddComponent<Button>();b.targetGraphic=im;var tx=Label(g.transform,text,20,TextAnchor.MiddleCenter,new Vector2(0,0),new Vector2(1,1),Vector2.zero,Vector2.zero,Color.white);tx.rectTransform.offsetMin=Vector2.zero;tx.rectTransform.offsetMax=Vector2.zero;return b;}
    GameObject Panel(Transform parent,string message,string buttonText,UnityEngine.Events.UnityAction action){var g=new GameObject("Panel"+buttonText);g.transform.SetParent(parent,false);var r=g.AddComponent<RectTransform>();r.anchorMin=r.anchorMax=new Vector2(.5f,.5f);r.pivot=new Vector2(.5f,.5f);r.anchoredPosition=Vector2.zero;r.sizeDelta=new Vector2(800,430);var im=g.AddComponent<Image>();im.color=new Color(.015f,.02f,.025f,.96f);var t=Label(g.transform,message,30,TextAnchor.MiddleCenter,new Vector2(.08f,.32f),new Vector2(.92f,.94f),Vector2.zero,new Vector2(680,280),Color.white);var b=Button(g.transform,buttonText,new Vector2(.5f,.14f),new Vector2(350,78),new Color(.55f,.08f,.045f,1));b.onClick.AddListener(action);return g;}
    public void ToggleTorch(){if(!running)return;flashlightOn=!flashlightOn;torch.enabled=flashlightOn;}
    public void Collected(){parts++;}
    public void Finish(bool win){if(ended)return;ended=true;running=false;endPanel.SetActive(true);endText.text=win?"¡ESCAPASTE!\nLa noche terminó.":"TE ENCONTRÓ\nEl Campanero te atrapó.";}
    void Update(){if(!running||ended)return;nightSeconds-=Time.deltaTime;if(flashlightOn){battery-=Time.deltaTime*2.1f;if(battery<=0){battery=0;flashlightOn=false;torch.enabled=false;}}if(nightSeconds<=0){Finish(false);return;}hud.text=$"NOCHE 1     {Mathf.FloorToInt((300-nightSeconds)/60):00}:{Mathf.FloorToInt(nightSeconds%60):00}\nFUSIBLES {parts}/3     LINTERNA {Mathf.CeilToInt(battery)}%";objective.text=parts>=3?"¡Corre a la puerta de salida!":"Busca los fusibles por el comedor y los pasillos.";}
}
public class Collectible:MonoBehaviour{public int index;void Update(){if(RuntimeBootstrap.I&&RuntimeBootstrap.I.player)transform.Rotate(0,85*Time.deltaTime,0); }void OnTriggerEnter(Collider c){if(c.transform==RuntimeBootstrap.I.player){RuntimeBootstrap.I.Collected();Destroy(gameObject);}}}
public class ExitDoor:MonoBehaviour{void OnTriggerEnter(Collider c){if(c.transform==RuntimeBootstrap.I.player){if(RuntimeBootstrap.I.parts>=3)RuntimeBootstrap.I.Finish(true);else RuntimeBootstrap.I.objective.text="La puerta no cede. Faltan fusibles.";}}}
public class MobileStick:MonoBehaviour,IPointerDownHandler,IDragHandler,IPointerUpHandler{public static Vector2 axis;public void OnPointerDown(PointerEventData e){OnDrag(e);}public void OnDrag(PointerEventData e){RectTransformUtility.ScreenPointToLocalPointInRectangle((RectTransform)transform,e.position,e.pressEventCamera,out var p);axis=Vector2.ClampMagnitude(p/((RectTransform)transform).sizeDelta*2,1);}public void OnPointerUp(PointerEventData e){axis=Vector2.zero;}}
public class LookTouch:MonoBehaviour,IPointerDownHandler,IDragHandler{Vector2 last;bool held;public void OnPointerDown(PointerEventData e){last=e.position;held=true;}public void OnDrag(PointerEventData e){if(!held)return;var d=e.position-last;last=e.position;var p=RuntimeBootstrap.I.player;var cam=RuntimeBootstrap.I.playerCam.transform;p.Rotate(0,d.x*.10f,0);float x=cam.localEulerAngles.x;if(x>180)x-=360;x=Mathf.Clamp(x-d.y*.09f,-55,55);cam.localEulerAngles=new Vector3(x,0,0);} }
public class RunTouch:MonoBehaviour,IPointerDownHandler,IPointerUpHandler{public static bool held;public void OnPointerDown(PointerEventData e){held=true;}public void OnPointerUp(PointerEventData e){held=false;}}
