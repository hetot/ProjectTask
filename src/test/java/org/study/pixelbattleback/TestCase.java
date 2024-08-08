package org.study.pixelbattleback;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.study.pixelbattleback.dto.PixelRequest;
import org.study.pixelbattleback.dto.ResultResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.*;


public class TestCase {
    private final static Logger logger = LoggerFactory.getLogger(TestCase.class);

    private static final int THREAD_COUNT = 200;

    private static final int REQUEST_COUNT = 1000;

    public static final String URL = "http://127.0.0.1:8080/drawPixel";

    @Test
    public void testParallel() {
        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<?>> futureList = new ArrayList<>();
        for (int threadNumber = 0; threadNumber < THREAD_COUNT; ++threadNumber) {
            int threadColor = ThreadLocalRandom.current().nextInt(1, 255<<17);
            final RestTemplate rest = new RestTemplate();
            futureList.add(pool.submit(() -> {
                PixelRequest pixel = new PixelRequest();
                pixel.setColor(threadColor);
                StopWatch stopWatch = new StopWatch();
                LongSummaryStatistics stats = new LongSummaryStatistics();
                for (int i = 0; i < REQUEST_COUNT; i++) {
                    pixel.setX(ThreadLocalRandom.current().nextInt(0, 100));
                    pixel.setY(ThreadLocalRandom.current().nextInt(0, 100));
                    stopWatch.start();
                    try {
                        ResultResponse res = rest.postForObject(URL, pixel, ResultResponse.class);
                    }catch (Exception e){
                        logger.error(e.getMessage(),e);
                    }
                    stopWatch.stop();
                    stats.accept(stopWatch.getLastTaskTimeMillis());
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 100));
                }
                return stats;
            }));
        }
        LongSummaryStatistics stats = new LongSummaryStatistics();
        for (Future<?> voidFuture : futureList) {
            try {
                stats.combine((LongSummaryStatistics) voidFuture.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(stats);
    }
}
