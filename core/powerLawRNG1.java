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
public class powerLawRNG1 {

    private Random rng;
    private double alpha;
    private double min=1;
    private double max;
    public powerLawRNG1(Random rng,double alpha,double max)
    {
        this.rng=rng;
        this.alpha=alpha;
        this.max=max;
//        this.max=1-Math.pow(max,(1-alpha));
//        System.out.println("Max:"+ max);
    }
    public powerLawRNG1(Random rng,  double alpha, double min, double max){
        this.rng=rng;
        this.alpha=alpha;
        this.min=min;
        this.max=max;
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
//    public double getDouble()
//    {
//       double y,tmp;
//       do{
//           tmp=rng.nextDouble();
//       }while(tmp==1);
////       do
////       {
//            y=Math.pow((1-tmp),1/(1-alpha))%max;
////            if(y<1)
////                y=1;
////       }while(y>max);
//       return y;
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
    public double getDouble1(){
        return Math.pow(((Math.pow(max, 1-alpha)-Math.pow(min, 1-alpha))*(1-rng.nextDouble()))+Math.pow(min, 1-alpha),1/(1-alpha));
    }
}