/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

/**
 *
 * @author zunnun
 */
public class coordinate {
int x,y;

@Override
public boolean equals(Object obj)
{
    if(obj==null)
                 return false;
          if(obj==this)
                 return true;
          if(!(obj instanceof coordinate)) return false;

          coordinate coord = (coordinate)obj;
          if(this.x==coord.x && this.y==coord.y)
              return true;
          else
              return false;
}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.x;
        hash = 53 * hash + this.y;
        return hash;
    }
}
