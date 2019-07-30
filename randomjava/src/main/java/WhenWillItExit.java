/**
 * I was reading http://psy-lob-saw.blogspot.com/2015/12/safepoints.html which included this source.
 * Neat article.
 * <p>
 * To play along at home, you may want to install the HotSpot disassembler library. Read:
 * https://www.chrisnewland.com/building-hsdis-amd64dylib-on-mac-osx-376
 * Don't attempt to install binutils through Homebrew or any other package manager, as the
 * Makefile expects to
 * hand-build binutils itself. You'll get a funny message about the configure script going
 * missing otherwise.
 * <p>
 * Interesting JVM flags to use:
 * -XX:+PrintCompilation -XX:+PrintSafepointStatistics -XX:+PrintGCApplicationStoppedTime
 * -XX:PrintSafepointStatisticsCount=1 -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly
 */
public class WhenWillItExit {
    public static void main(String[] argc) throws InterruptedException {
        /*
          The premise is that there are certain operations in the JVM that require the mutual
          cooperation of all threads in order to proceed. It seems like exiting the JVM is one of
           them.
         */
        Thread t = new Thread(() -> {
            long l = 0;
            /*
              No safepoint poll shows up here.
             */
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                /*
                  However, a safepoint poll shows up in this for loop. You can see it in the
                  disassembler:
                    0x0000000103301822: test   %eax,-0x212a828(%rip)        # 0x00000001011d7000
                 ;*iload_3
                 ; - WhenWillItExit::lambda$main$0@12
                 ;   {poll}
                  Maybe the JVM is smart enough to recognize nested loops as potentially playing
                  poorly with safepoints.
                 */
                for (int j = 0; j < Integer.MAX_VALUE; j++) {
                    if ((j & 1) == 1) {
                        l++;
                    }
                }
            }
            System.out.println("How Odd:" + l);
        });
        t.setDaemon(true);
        t.start();
        /*
          The sleep only has to happen long enough for method compilation to kick in. Interpreted
          methods poll for safepoints in loops.
         */
        Thread.sleep(5000);
    }
}