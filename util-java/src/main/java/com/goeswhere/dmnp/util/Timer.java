package com.goeswhere.dmnp.util;

class Timer {
    private Timer() {
        throw new AssertionError();
    }

    public static void runnable(Runnable r) {
        long start = System.nanoTime();
        r.run();
        System.out.println(((System.nanoTime() - start) / 1e9) + " seconds");
    }


}
