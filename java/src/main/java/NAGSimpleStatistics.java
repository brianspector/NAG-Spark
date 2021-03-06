import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.spark.api.java.JavaDoubleRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.FlatMapFunction;

import com.nag.routines.Routine;
import com.nag.routines.G01.G01AT;
import com.nag.routines.G01.G01AU;

public class NAGSimpleStatistics {

        private double _xmean;
        private double _xsd;
        private double _xskew;
        private double _xkurt;
        private double _xmin;
        private double _xmax;

        public double getMEAN() {
                return _xmean;
        }
        public double getSD() {
                return _xsd;
        }
        public double getSKEW() {
                return _xskew;
        }
        public double getKURT() {
                return _xkurt;
        }
        public double getMIN() {
                return _xmin;
        }
        public double getMAX() {
                return _xmax;
        }

        static class CombineNAGData implements Function2<double[],double[],double[]> {
                @Override
                public double[] call(double[] data1,double[] data2) throws Exception {
                        
                        for(int i=0;i<20;i++) {
                                data1[i+20] = data2[i];
                        }
                        int IWT = 0, PN = 0, IFAIL = 1;
                        double XMEAN = 0.0, XSD = 0.0, XSKEW= 0.0, 
                        XKURT = 0.0, XMIN = 0.0, XMAX = 0.0;
                        G01AU g01au = new G01AU();                        
                        g01au.eval(2, data1, PN, XMEAN, XSD, XSKEW, XKURT, 
                                                XMIN, XMAX, data1, IFAIL);
        
                        if(g01au.getIFAIL()>0) {
                                System.out.println("Error with g01au!!!");
                                System.exit(1);
                        }            
                        return data1;
                }
        }        


	 static class ParseSet implements FlatMapFunction<Iterator<Double>, double[]> {
	    @Override
	    public Iterable<double []> call(Iterator<Double> iter) throws Exception {
                
              List<Double> mypoints= new ArrayList<Double>();
              while(iter.hasNext()) {
                        mypoints.add(iter.next());                      
               }
                
                double[] data = new double [mypoints.size()];
                for(int i=0;i<mypoints.size();i++)
                        data[i]=mypoints.get(i);
                
                int IWT = 0, PN = 0, IFAIL = 1;
                double XMEAN = 0.0, XSD = 0.0, XSKEW= 0.0, 
                        XKURT = 0.0, XMIN = 0.0, XMAX = 0.0;
                double[] WT = new double[1];
                double[] RCOMM = new double[40];

                G01AT g01at = new G01AT();
                g01at.eval(data.length, data, IWT, WT, PN, XMEAN, XSD, 
                                        XSKEW, XKURT, XMIN, XMAX, RCOMM, IFAIL);
        
                if(g01at.getIFAIL()>0) {
                        System.out.println("Error with g01at!!!");
                        System.exit(1);
                }            

	      return Arrays.asList(RCOMM);
	    }
        }

        public NAGSimpleStatistics(JavaDoubleRDD doublerdd, 
                                        int numPartitions) throws Exception {

                Routine.init();

                double[] dataset = doublerdd.coalesce(numPartitions)
                                        .mapPartitions(new ParseSet())
                                        .reduce(new CombineNAGData());
                
                int IWT = 0, PN = 0, IFAIL = 1;
                double XMEAN = 0.0, XSD = 0.0, XSKEW= 0.0, 
                        XKURT = 0.0, XMIN = 0.0, XMAX = 0.0;

                G01AU g01au = new G01AU();
                g01au.eval(1, dataset, PN, XMEAN, XSD, XSKEW, XKURT, XMIN, 
                                                        XMAX, dataset, IFAIL);
        
                if(g01au.getIFAIL()>0) {
                        System.out.println("Error with g01au!!!");
                        System.exit(1);
                }

                _xmean = g01au.getXMEAN();            
                _xsd = g01au.getXSD();            
                _xskew = g01au.getXSKEW();            
                _xkurt = g01au.getXKURT();            
                _xmin = g01au.getXMIN();            
                _xmax = g01au.getXMAX();            
 
       }
}
