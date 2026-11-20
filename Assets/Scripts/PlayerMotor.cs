using UnityEngine;
public class PlayerMotor:MonoBehaviour
{
    CharacterController cc;float vertical;void Start(){cc=GetComponent<CharacterController>();}
    void Update(){var g=RuntimeBootstrap.I;if(g==null||!g.running)return;Vector2 move=MobileStick.axis; // Also supports keyboard for editor testing.
        move+=new Vector2(Input.GetAxisRaw("Horizontal"),Input.GetAxisRaw("Vertical"));move=Vector2.ClampMagnitude(move,1);float speed=RunTouch.held?5.2f:3.1f;
        if(cc.isGrounded)vertical=-1;else vertical-=19*Time.deltaTime;Vector3 v=(transform.right*move.x+transform.forward*move.y)*speed;v.y=vertical;cc.Move(v*Time.deltaTime);
    }
}
