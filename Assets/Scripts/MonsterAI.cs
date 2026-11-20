using UnityEngine;
public class MonsterAI:MonoBehaviour
{
    float think;Vector3 start;void Start(){start=transform.position;}
    void Update(){var g=RuntimeBootstrap.I;if(g==null||!g.running)return;var p=g.player.position;float d=Vector3.Distance(transform.position,p);if(d<1.45f){g.Finish(false);return;}
        // Patient stalker; flashlight briefly makes it recoil when caught in the beam.
        bool lit=g.flashlightOn&&Vector3.Distance(g.playerCam.transform.position,transform.position)<14&&Vector3.Dot(g.playerCam.transform.forward,(transform.position-g.playerCam.transform.position).normalized)>.94f;
        float pace=lit?.18f:(d<8?1.9f:.72f);if(d<14||Random.value<.002f){Vector3 dir=p-transform.position;dir.y=0;if(dir.sqrMagnitude>.1f){var target=Quaternion.LookRotation(dir);transform.rotation=Quaternion.Slerp(transform.rotation,target,Time.deltaTime*2);transform.position+=dir.normalized*pace*Time.deltaTime;}}
        think+=Time.deltaTime;if(think>1.3f){think=0;if(d<11&&Random.value<.09f)transform.position+=transform.forward*.7f;}
        transform.position=new Vector3(Mathf.Clamp(transform.position.x,-19,19),0,Mathf.Clamp(transform.position.z,-19,19));
    }
}
