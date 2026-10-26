package it.solphivia.mobile;

/** In-memory reading position only. Engines without range callbacks repeat the current chunk. */
final class ReadingContinuation {
    private static String text="";
    private static int offset;
    private static long generation;
    private static boolean paused;
    static synchronized long begin(String value){text=value==null?"":value;offset=0;paused=false;return ++generation;}
    static synchronized void progress(long token,int start){if(token==generation&&!paused)offset=Math.max(0,Math.min(start,text.length()));}
    static synchronized void pause(){if(!text.isEmpty())paused=true;}
    static synchronized boolean available(){return paused&&!text.isEmpty();}
    static synchronized String take(){if(!available())return "";paused=false;return text.substring(offset);}
    static synchronized void complete(long token){if(token==generation&&!paused){text="";offset=0;}}
    static synchronized void clear(){text="";offset=0;paused=false;generation++;}
}
