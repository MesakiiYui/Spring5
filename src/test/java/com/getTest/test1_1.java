package com.getTest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/**
 * Created by tiejin on 7/26/2018.
 */
public class test1_1 {
        /**
         * 1. 响应式之道 - 1 什么是响应式编程
         *
         * 使用Java Stream演示第一章购物的例子。
         * Java Stream在处理响应式编程时有其局限性，这里仅做示意。
         */
        @Test
        public void testJavaStream() {
            @Data
            class Product {
                private String name;
                private double price;

                public Product(String name, double price) {
                    this.name = name;
                    this.price = price;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public double getPrice() {
                    return price;
                }

                public void setPrice(double price) {
                    this.price = price;
                }
            }

            @Data
//            @AllArgsConstructor
            class CartItem {
                private Product product;
                private int quantity;

                public CartItem(Product product, int quantity) {
                    this.product = product;
                    this.quantity = quantity;
                }

                public Product getProduct() {
                    return product;
                }

                public void setProduct(Product product) {
                    this.product = product;
                }

                public int getQuantity() {
                    return quantity;
                }

                public void setQuantity(int quantity) {
                    this.quantity = quantity;
                }
            }

            List<CartItem> cartItems = new ArrayList<>();
            cartItems.add(new CartItem(new Product("饼干", 8.5), 4));
            cartItems.add(new CartItem(new Product("干果", 39.9), 3));
            cartItems.add(new CartItem(new Product("长粒香米", 39), 2));
            cartItems.add(new CartItem(new Product("玉米油", 42.9), 1));
            cartItems.add(new CartItem(new Product("饼干", 8.5), 1));
            cartItems.add(new CartItem(new Product("牛肉干", 59), 2));
            cartItems.add(new CartItem(new Product("剃须刀", 259), 1));
            cartItems.add(new CartItem(new Product("干果", 39.9), -1));
            cartItems.add(new CartItem(new Product("牛肉干", 59), -1));

            // 对List进行迭代的处理方法
//        double sum = 0;
//        for (CartItem item : cartItems) {
//            double result = item.getProduct().getPrice() * item.getQuantity();
//            result = (result > 199) ? (result - 40) : result;
//            sum += result;
//        }

            // 直接使用Stream的of方法构造数据流
//        Stream<CartItem> cartItemStream = Stream.of(
//                new CartItem(new Product("饼干", 8.5), 4),
//                new CartItem(new Product("干果", 39.9), 3),
//                new CartItem(new Product("长粒香米", 39), 2),
//                new CartItem(new Product("玉米油", 42.9), 1),
//                new CartItem(new Product("饼干", 8.5), 1),
//                new CartItem(new Product("牛肉干", 59), 2),
//                new CartItem(new Product("剃须刀", 259), 1),
//                new CartItem(new Product("干果", 39.9), -1),
//                new CartItem(new Product("牛肉干", 59), -1)
//        );


            double sum = cartItems.stream()
                    // 分别计算商品金额
                    .mapToDouble(value -> value.getProduct().getPrice() * value.getQuantity())
                    // 计算满减后的商品金额
                    .map(operand -> (operand > 199) ? (operand - 40) : operand)
                    // 金额累加
                    .sum();
            sum = (sum > 500) ? sum : (sum + 50);
            System.out.println("应付款金额：" + sum);
        }



}
