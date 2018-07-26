package com.getTest;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
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
    public void testViaStepVerifier() {
        StepVerifier.create(generateFluxFrom1To6())
                .expectNext(1, 2, 3, 4, 5, 6)//检验未来的Flux的输出是否符合我们预设的标准
                .expectComplete()
                .verify();
        StepVerifier.create(generateMonoWithError())
                .expectErrorMessage("some error")
                .verify();
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