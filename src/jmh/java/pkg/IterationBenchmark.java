package pkg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class IterationBenchmark {
  private static final Callback SENDER = new Callback();

  private List<Consumer<Blackhole>> subscribers = new ArrayList<>();

  @Param(value = {"1", "10", "100", "1000"})
  private int subscriberCount;

  @Setup
  public void setup() {
    subscribers.add(SENDER);
    for(int i = 0; i < subscriberCount; i++) {
      subscribers.add(new Callback());
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public int stream(final Blackhole blackhole) {
    final int deliveries = subscribers.contains(SENDER) ? subscribers.size() - 1 : subscribers.size();
    subscribers.stream()
        .filter(subscriber -> subscriber != SENDER)
        .forEach(subscriber -> subscriber.accept(blackhole));

    return deliveries;
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public int iterator(final Blackhole blackhole) {
    int deliveries = 0;
    for (Consumer<Blackhole> subscriber : subscribers) {
      if (subscriber != SENDER) {
        subscriber.accept(blackhole);
        deliveries++;
      }
    }

    return deliveries;
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public int indexedForLoop(final Blackhole blackhole) {
    int deliveries = 0;
    for (int i = 0; i < subscribers.size(); i++) {
      Consumer<Blackhole> subscriber = subscribers.get(i);
      if (subscriber != SENDER) {
        subscriber.accept(blackhole);
        deliveries++;
      }
    }

    return deliveries;
  }

  private static final class Callback implements Consumer<Blackhole> {
    private long invocationCount = 0;

    @Override
    public void accept(final Blackhole blackhole) {
      blackhole.consume(invocationCount++);
    }
  }
}
