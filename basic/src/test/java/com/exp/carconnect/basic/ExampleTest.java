package com.exp.carconnect.basic;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ExampleTest {


    public class T1 {

    }

    public class T2 extends T1 {

    }

    public class T3 extends T2 {

    }

    @Test
    public void test1() {
        String[] as = new String[5];
        Object[] ao = as;

        List<T2> aList = new ArrayList<>();
        List<? extends T1> covariantList = aList;
        List<? super T3> contravariantList = aList;

        //covariance
        // A< B then Type1[A] < Type2[B]
        // T1 < T2 then <? extends T1> < <? extends T2>

        // contravariance
        // A< B then Type1[A] < Type2[B]
        // T2 < T3 then <? super T3> < <? super T2>

        T1 a = covariantList.get(1);
        //following will not work
//        covariantList.add(new T2
// ());
        //following will not work
//        T2 a1 = contravariantList.get(2);
        Object a1 = contravariantList.get(2);
        contravariantList.add(new T3());
        System.out.println(as[0]);
    }
}
