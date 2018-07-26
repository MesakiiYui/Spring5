package com.getTest;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

/**
 * Created by tiejin on 7/26/2018.
 */
public class test_reactor {
    @Test
    public void reactorTest(){
//        Integer[] array = new Integer[]{1,2,3,4,5,6};
//        Flux.fromArray(array);//基于数组生成Flux
//        List<Integer> list = Arrays.asList(array);
//        Flux.fromIterable(list);//基于集合生成Flux
//        Stream<Integer> stream = list.stream();
//        Flux.fromStream(stream);//基于stream生成Flux

//        数据流有了，假设我们想把每个数据元素原封不动地打印出来：
        Flux.just(1, 2, 3, 4, 5, 6).subscribe(System.out::print);
        System.out.println();
        Mono.just(1).subscribe(System.out::println);

        Flux.just(1, 2, 3, 4, 5, 6).subscribe(
                System.out::print,
                System.err::println,
                () -> System.out.print("Completed!"));
        //输出一个有错误信号的例子
        Mono.error(new Exception("We got some unexpected error")).subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Completed!")
        );


    }

    //一个基本的单元测试工具——StepVerifier
    private Flux<Integer> generateFluxFrom1To6() {
        return Flux.just(1, 2, 3, 4, 5, 6);
    }
    private Mono<Integer> generateMonoWithError() {
        return Mono.error(new Exception("some error"));
    }
    @Test
    public void viaStepVerifierTest() {
        StepVerifier.create(generateFluxFrom1To6())
                .expectNext(1, 2, 3, 4, 5, 6)//检验未来的Flux的输出是否符合我们预设的标准
                .expectComplete()
                .verify();
        StepVerifier.create(generateMonoWithError())
                .expectErrorMessage("some error")
                .verify();
    }

    //Flux的map，类似于rxjs的map
    @Test
    public void mapTest(){
        StepVerifier.create(Flux.range(1,6)
                .map(i -> i*i))
                .expectNext(1,4,9,16,25,36)
                .expectComplete();

    }
    //flatMap - 元素映射为流
    @Test
    public void flatMapTest(){
        StepVerifier.create(
                Flux.just("flux", "mono")
                        .flatMap(s -> Flux.fromArray(s.split("\\s*"))   // 1
                                .delayElements(Duration.ofMillis(100))) // 2
                        .doOnNext(System.out::print)) // 3
                        .expectNextCount(8) // 4
                        .verifyComplete();
//        对于每一个字符串s，将其拆分为包含一个字符的字符串流；
//        对每个元素延迟100ms；
//        对每个元素进行打印（注doOnNext方法是“偷窥式”的方法，不会消费数据流）；
//        验证是否发出了8个元素。
    }

    //filter - 过滤
    //filter接受一个Predicate的函数式接口为参数，这个函数式的作用是进行判断并返回boolean。
    @Test
    public void filterTest(){
        StepVerifier.create(Flux.range(1, 6)
                .filter(i -> i % 2 == 1)    // 1
                .doOnNext(System.out::println)
                .map(i -> i * i)
                .doOnNext(System.out::println)
        )
                .expectNext(1, 9, 25)   // 2
                .verifyComplete();
//    filter的lambda参数表示过滤操作将保留奇数；
//    验证仅得到奇数的平方。
    }


    //zip - 一对一合并
    //下面的例子讲的是最简单的二合一的例子
    private Flux<String> getZipDescFlux() {
        String desc = "Zip two sources together, that is to say wait for all the sources to emit one element and combine these elements once into a Tuple2.";
        return Flux.fromArray(desc.split("\\s+"));  // 1
    }

    @Test
    public void testSimpleOperators() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);  // 2
        Flux.zip(
                getZipDescFlux(),
                Flux.interval(Duration.ofMillis(200)))  // 3
        .subscribe(t -> System.out.println(t.getT1()), null, countDownLatch::countDown);    // 4
        countDownLatch.await(10, TimeUnit.SECONDS);     // 5
//        将英文说明用空格拆分为字符串流；
//        定义一个CountDownLatch，初始为1，则会等待执行1次countDown方法后结束，不使用它的话，测试方法所在的线程会直接返回而不会等待数据流发出完毕；
//        使用Flux.interval声明一个每200ms发出一个元素的long数据流；因为zip操作是一对一的，故而将其与字符串流zip之后，字符串流也将具有同样的速度；
//        zip之后的流中元素类型为Tuple2，使用getT1方法拿到字符串流的元素；定义完成信号的处理为countDown;
//        countDownLatch.await(10, TimeUnit.SECONDS)会等待countDown倒数至0，最多等待10秒钟。

        //异步条件下，数据流的流速不同，使用zip能够一对一地将两个或多个数据流的元素对齐发出。
    }


    //调度器与线程模型
    //1.将同步的阻塞调用变为异步的
    //Schedulers.elastic()能够方便地给一个阻塞的任务分配专门的线程，从而不会妨碍其他任务和资源。
    // 我们就可以利用这一点将一个同步阻塞的调用调度到一个自己的线程中，并利用订阅机制，待调用结束后异步返回。
    //原有的同步阻塞策略:
    private String getStringSync() {
        try {
            TimeUnit.SECONDS.sleep(2);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Hello WebFlux!!origin");
        return "Hello WebFlux";
    }

    //正常情况下，调用这个方法会被阻塞2秒钟，然后同步地返回结果。
    //我们借助elastic调度器将其变为异步，由于是异步的，为了保证测试方法所在的线程能够等待结果的返回，我们使用CountDownLatch：
    @Test
    public void scheddulersTest()throws InterruptedException{
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Mono.fromCallable(() -> getStringSync())    // 1
                .subscribeOn(Schedulers.elastic())  // 2
                .subscribe(System.out::println, null, countDownLatch::countDown);
        countDownLatch.await(10, TimeUnit.SECONDS);

        //使用fromCallable声明一个基于Callable的Mono；
        //使用subscribeOn将任务调度到Schedulers内置的弹性线程池执行，弹性线程池会为Callable的执行任务分配一个单独的线程。
    }





    //错误处理
    @Test
    public void errorHandlingTest() {
        Flux.range(1, 6)
                .map(i -> 10/(i-3)) // 1
                .map(i -> i*i)
                .subscribe(System.out::println, System.err::println);
                //subscribe方法的第二个参数定义了对错误信号的处理，从而测试方法exit为0（即正常退出），可见错误没有蔓延出去。不过这还不够~
    }

//    此外，Reactor还提供了其他的用于在链中处理错误的操作符（error-handling operators），使得对于错误信号的处理更加及时，处理方式更加多样化。
//
//    在讨论错误处理操作符的时候，我们借助命令式编程风格的 try 代码块来作比较。我们都很熟悉在 try-catch 代码块中处理异常的几种方法。常见的包括如下几种：
//
//    捕获并返回一个静态的缺省值。
//    捕获并执行一个异常处理方法或动态计算一个候补值来顶替。
//    捕获，并再包装为某一个 业务相关的异常，然后再抛出业务异常。
//    捕获，记录错误日志，然后继续抛出。
//    使用 finally 来清理资源，或使用 Java 7 引入的 “try-with-resource”。
//    以上所有这些在 Reactor 都有相应的基于 error-handling 操作符处理方式。


    //1.捕获并返回一个静态的缺省值
    //onErrorReturn方法能够在收到错误信号的时候提供一个缺省值：
    @Test
    public void onErrorReturnTest(){
        Flux.range(1, 6)
                .map(i -> 10/(i-3))
                .onErrorReturn(0)   // 1
                .map(i -> i*i)
                .subscribe(System.out::println, System.err::println);
        //程序将在运行到3时停止，只输出25,100,0
    }

    //2.捕获并执行一个异常处理方法或计算一个候补值来顶替
    //onErrorResume方法能够在收到错误信号的时候提供一个新的数据流：
    @Test
    public void onErrorResumeTest(){
        Flux.range(1, 6)
                .map(i -> 10/(i-3))
                .onErrorResume(e -> Mono.just(new Random().nextInt(6))) // 提供新的数据流
                .map(i -> i*i)
                .subscribe(System.out::println, System.err::println);

    }

//    举一个更有业务含义的例子：
//
//            Flux.just(endpoint1, endpoint2)
//            .flatMap(k -> callExternalService(k))   // 1   调用外部服务
//            .onErrorResume(e -> getFromCache(k));   // 2    如果外部服务异常，则从缓存中取值代替。

    //3.捕获，并再包装为某一个业务相关的异常，然后再抛出业务异常
    //有时候，我们收到异常后并不想立即处理，而是会包装成一个业务相关的异常交给后续的逻辑处理，可以使用onErrorMap方法：
//            Flux.just("timeout1")
//                    .flatMap(k -> callExternalService(k))   // 1调用外部服务；
//            .onErrorMap(original -> new BusinessException("SLA exceeded", original)); // 2如果外部服务异常，将其包装为业务相关的异常后再次抛出。
//    这一功能其实也可以用onErrorResume实现，略麻烦一点：
//
//            Flux.just("timeout1")
//            .flatMap(k -> callExternalService(k))
//            .onErrorResume(original -> Flux.error(
//            new BusinessException("SLA exceeded", original)
//    );


    //4.捕获，记录错误日志，然后继续抛出
    //如果对于错误你只是想在不改变它的情况下做出响应（如记录日志），并让错误继续传递下去，
    // 那么可以用doOnError 方法。前面提到，形如doOnXxx是只读的，对数据流不会造成影响：

//    Flux.just(endpoint1, endpoint2)
//            .flatMap(k -> callExternalService(k))
//            .doOnError(e -> {   // 1只读地拿到错误信息，错误信号会继续向下游传递；
//        log("uh oh, falling back, service failed for key " + k);    // 2记录日志。
//    })
//            .onErrorResume(e -> getFromCache(k));

    //5.使用 finally 来清理资源，或使用 Java 7 引入的 “try-with-resource”
//      Flux.using(
//          () -> getResource(),    // 1第一个参数获取资源；
//          resource -> Flux.just(resource.getAll()),   // 2第二个参数利用资源生成数据流；
//          MyResource::clean   // 3第三个参数最终清理资源。
//);
//    另一方面， doFinally在序列终止（无论是 onComplete、onError还是取消）的时候被执行， 并且能够判断是什么类型的终止事件（完成、错误还是取消），
// 以便进行针对性的清理。如：
//
    @Test
    public void doFinallyTest(){
        LongAdder statsCancel = new LongAdder();    // 1用LongAdder进行统计；

        Flux<String> flux =
                Flux.just("foo", "bar")
                        .doFinally(type -> {
                            if (type == SignalType.CANCEL)  // 2doFinally用SignalType检查了终止信号的类型；
                                statsCancel.increment();  // 3如果是取消，那么统计数据自增；
                        })
                        .take(1);   // 4
    }

//    take(1)能够在发出1个元素后取消流。



    //6.重试
    //还有一个用于错误处理的操作符你可能会用到，就是retry，见文知意，用它可以对出现错误的序列进行重试。
    // 请注意：**retry对于上游Flux是采取的重订阅（re-subscribing）的方式，因此重试之后实际上已经一个不同的序列了，
    // 发出错误信号的序列仍然是终止了的。举例如下：

    @Test
    public void retryTest(){
        Flux.range(1, 6)
                .map(i -> 10 / (3 - i))
                .retry(1)
                .subscribe(System.out::println, System.err::println);
        try
        {
            Thread.sleep(2000);
        }
        catch(InterruptedException e)
        {
            // this part is executed when an exception (in this example InterruptedException) occurs
        }
    }


     //7.回压
// 前边的例子并没有进行流量控制，也就是，当执行.subscribe(System.out::println)这样的订阅的时候，
// 直接发起了一个无限的请求（unbounded request），就是对于数据流中的元素无论快慢都“照单全收”。
//    subscribe方法还有一个变体：

//    // 接收一个Subscriber为参数，该Subscriber可以进行更加灵活的定义
//    subscribe(Subscriber subscriber)


// 注：其实这才是subscribe方法本尊，前边介绍到的可以接收0~4个函数式接口为参数的subscribe最终都是拼装为这个方法，
// 所以按理说前边的subscribe方法才是“变体”。

// 我们可以通过自定义具有流量控制能力的Subscriber进行订阅。Reactor提供了一个BaseSubscriber，我们可以通过扩展它来定义自己的Subscriber。

// 假设，我们现在有一个非常快的Publisher——Flux.range(1, 6)，然后自定义一个每秒处理一个数据元素的慢的Subscriber，
// Subscriber就需要通过request(n)的方法来告知上游它的需求速度。代码如下：

        @Test
        public void testBackpressure(){
            Flux.range(1, 6)    // 1 Flux.range是一个快的Publisher；
                    .doOnRequest(n -> System.out.println("Request " + n + " values..."))    // 2 在每次request的时候打印request个数；
                    .subscribe(new BaseSubscriber<Integer>() {  // 3 通过重写BaseSubscriber的方法来自定义Subscriber；
                        @Override
                        protected void hookOnSubscribe(Subscription subscription) { // 4 hookOnSubscribe定义在订阅的时候执行的操作；
                            System.out.println("Subscribed and make a request...");
                            request(1); // 5 订阅时首先向上游请求1个元素；
                        }

                        @Override
                        protected void hookOnNext(Integer value) {  // 6 hookOnNext定义每次在收到一个元素的时候的操作；
                            try {
                                TimeUnit.SECONDS.sleep(1);  // 7 sleep 1秒钟来模拟慢的Subscriber；
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Get value [" + value + "]");    // 8 打印收到的元素；
                            request(1); // 9 每次处理完1个元素后再请求1个。
                        }
                    });
            //这6个元素是以每秒1个的速度被处理的。由此可见range方法生成的Flux采用的是缓存的回压策略，能够缓存下游暂时来不及处理的元素。
        }






}

//其余的subscribe方式
// 订阅并触发数据流
//subscribe();
//    // 订阅并指定对正常数据元素如何处理
//    subscribe(Consumer<? super T> consumer);
//    // 订阅并定义对正常数据元素和错误信号的处理
//    subscribe(Consumer<? super T> consumer,
//              Consumer<? super Throwable> errorConsumer);
//    // 订阅并定义对正常数据元素、错误信号和完成信号的处理
//    subscribe(Consumer<? super T> consumer,
//              Consumer<? super Throwable> errorConsumer,
//              Runnable completeConsumer);
//    // 订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑
//    subscribe(Consumer<? super T> consumer,
//              Consumer<? super Throwable> errorConsumer,
//              Runnable completeConsumer,
//              Consumer<? super Subscription> subscriptionConsumer);