package mioneF.yumCup.performance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {
    private final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    public static class MethodStats {
        private long totalTime;
        private long invocations;
        private long minTime = Long.MAX_VALUE;
        private long maxTime;

        public synchronized void addExecution(long time) {
            totalTime += time;
            invocations++;
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
        }

        public double getAverageTime() {
            return invocations > 0 ? (double) totalTime / invocations : 0;
        }
    }

    @Around("@annotation(Monitored)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            methodStats.computeIfAbsent(methodName, k -> new MethodStats(0, 0, Long.MAX_VALUE, 0))
                    .addExecution(executionTime);

            log.info("Method: {} executed in {}ms", methodName, executionTime);
        }
    }

    @Scheduled(fixedRate = 60000) // 매 1분마다 통계 출력
    public void logStats() {
        methodStats.forEach((method, stats) -> {
            log.info("\nPerformance Stats for {}:\n" +
                            "Total Invocations: {}\n" +
                            "Average Time: {:.2f}ms\n" +
                            "Min Time: {}ms\n" +
                            "Max Time: {}ms\n",
                    method, stats.getInvocations(),
                    stats.getAverageTime(),
                    stats.getMinTime(),
                    stats.getMaxTime());
        });
    }
}
