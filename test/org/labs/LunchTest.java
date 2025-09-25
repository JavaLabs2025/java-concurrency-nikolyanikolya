package org.labs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static java.lang.Thread.State.TERMINATED;
import static org.assertj.core.api.Assertions.assertThat;

class LunchTest {

  @ParameterizedTest
  @MethodSource("progressArguments")
  void all_portions_in_restaurant_are_eaten_in_finite_time(int programmersCount, int portionsCount) {
    var lunch = new Lunch(programmersCount, portionsCount);

    lunch.run();

    int eatenInTotal = lunch.portionsEatenInTotal.get();
    assertThat(eatenInTotal).isEqualTo(portionsCount);
  }

  @ParameterizedTest
  @MethodSource("progressArguments")
  void all_threads_are_terminated_after_lunch(int programmersCount, int portionsCount) {
    var lunch = new Lunch(programmersCount, portionsCount);

    lunch.run();

    assertThat(lunch.programmers.stream().map(Thread::getState))
      .allMatch((state) -> state == TERMINATED);
  }

  @ParameterizedTest
  @MethodSource("fairDistributionArguments")
  void portions_are_distributed_fairly_between_programmers(int programmersCount, int portionsCount) {
    var lunch = new Lunch(programmersCount, portionsCount);

    lunch.run();

    var allowedStandartDeviation = ((double) portionsCount / programmersCount) / 8.0;
    var portions = lunch.portionsEatenByProgrammer.values();
    var mean = portions.stream().reduce(0, Integer::sum) / programmersCount;
    var variance = portions.stream()
      .map((p) -> (p - mean) * (p - mean))
      .reduce(0, Integer::sum) / programmersCount;
    var actualStandartDeviation = Math.pow(variance, 0.5);

    System.out.println("actual deviation: " + actualStandartDeviation);
    System.out.println("allowed deviation: " + allowedStandartDeviation);

    assertThat(actualStandartDeviation).isLessThanOrEqualTo(allowedStandartDeviation);
  }

  public static List<Arguments> progressArguments() {
    return List.of(
            Arguments.of(2, 10),
            Arguments.of(3, 100),
            Arguments.of(4, 1_000),
            Arguments.of(5, 10_000),
            Arguments.of(6, 100_000),
            Arguments.of(7, 1_000_000));
  }

  public static List<Arguments> fairDistributionArguments() {
    return List.of(Arguments.of(7, 1_000_000));
  }

}