package io.deephaven.engine.v2.utils;

import io.deephaven.engine.v2.sources.LogicalClock;

public class PreviousPlaypen {
    public static void main(String[] args) {
        // LogicalClock.DEFAULT.startUpdateCycle();

        // final TrackingMutableRowSet i = TrackingMutableRowSet.FACTORY.getFlatIndex(100);
        // System.out.println(i);
        // System.out.println(i.getPrevIndex());
        //
        // LogicalClock.DEFAULT.completeUpdateCycle();
        //
        // System.out.println(i);
        // System.out.println(i.getPrevIndex());


        LogicalClock.DEFAULT.startUpdateCycle();

        final TrackingMutableRowSet i2 = TrackingMutableRowSet.FACTORY.getFlatIndex(200);
        System.out.println(i2);
        System.out.println(i2.getPrevIndex());

        i2.insert(500);

        System.out.println(i2);
        System.out.println(i2.getPrevIndex());

        LogicalClock.DEFAULT.completeUpdateCycle();

        System.out.println(i2);
        System.out.println(i2.getPrevIndex());

    }
}
