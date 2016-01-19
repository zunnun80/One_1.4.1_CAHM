/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package core;

import java.util.Random;


/**
 *
 * @author zunnun
 */
public class powerLawRNG {

    private Random rng;
    private double alpha;
    private double max;
    public powerLawRNG(Random rng,double alpha,double max)
    {
        this.rng=rng;
        this.alpha=alpha;
        this.max=max;
//        this.max=1-Math.pow(max,(1-alpha));
//        System.out.println("Max:"+ max);
    }
//    public double getDouble()
//    {
//        double x;
//        do
//        {
//            x=rng.nextDouble();
//        }while(x<max);
//        return Math.pow((1-x), 1/(1-alpha));
//    }
    public double getDouble()
    {
       double y;
//       do
//       {
            y=Math.pow((1-rng.nextDouble()),1/(1-alpha))%max;
            if(y<1)
                y=1;
//       }while(y>max);
       return y;
    }
}
