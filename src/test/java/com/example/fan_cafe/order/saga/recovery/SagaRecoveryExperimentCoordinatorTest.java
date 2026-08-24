package com.example.fan_cafe.order.saga.recovery;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SagaRecoveryExperimentCoordinatorTest {

    @Test
    void claimTransactionUsesReadCommittedForConcurrentSkipLockedWorkers() throws Exception {
        Transactional transactional = SagaRecoveryTransactionService.class
                .getMethod("claimNext")
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
    }

    @Test
    void configuredConcurrencyCreatesThatManySimultaneousRecoveryLanes() throws Exception {
        int concurrency = 4;
        SagaRecoveryWorker worker = mock(SagaRecoveryWorker.class);
        CountDownLatch allLanesEntered = new CountDownLatch(concurrency);
        CountDownLatch releaseLanes = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        doAnswer(invocation -> {
            int current = active.incrementAndGet();
            maximumActive.accumulateAndGet(current, Math::max);
            allLanesEntered.countDown();
            assertThat(releaseLanes.await(5, TimeUnit.SECONDS)).isTrue();
            active.decrementAndGet();
            return null;
        }).when(worker).recoverDueSagas();

        ExecutorService laneExecutor = Executors.newFixedThreadPool(concurrency);
        ExecutorService coordinatorThread = Executors.newSingleThreadExecutor();
        try {
            SagaRecoveryExperimentCoordinator coordinator =
                    new SagaRecoveryExperimentCoordinator(worker, laneExecutor, concurrency);
            var completion = coordinatorThread.submit(coordinator::recoverDueSagasConcurrently);

            assertThat(allLanesEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(maximumActive.get()).isEqualTo(concurrency);
            releaseLanes.countDown();
            completion.get(5, TimeUnit.SECONDS);
            verify(worker, times(concurrency)).recoverDueSagas();
        } finally {
            releaseLanes.countDown();
            coordinatorThread.shutdownNow();
            laneExecutor.shutdownNow();
        }
    }

    @Test
    void experimentCoordinatorAndDefaultSchedulerAreProfileIsolated() {
        Profile experimentProfile = SagaRecoveryExperimentCoordinator.class.getAnnotation(Profile.class);
        Profile defaultProfile = SagaRecoveryScheduler.class.getAnnotation(Profile.class);

        assertThat(Arrays.asList(experimentProfile.value())).containsExactly("experiment");
        assertThat(Arrays.asList(defaultProfile.value())).containsExactly("!experiment");
    }
}
