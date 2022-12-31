package com.itheima.reggie.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {
    /*
     springboot与Redis的底层交互是以二进制的形式 (byte[]) 进行的, 因此，需要设定该如何序列化java对象
     springboot提供了两种序列化方式，分别是JdkSerializationRedisSerializer和StringRedisSerializer

     JdkSerializationRedisSerializer是springboot默认的序列化方式，
     它使用JDK的序列化机制将java对象序列化为二进制数据，存储到redis中
     缺点1：该方法序列化后保存到Redis的数据包含很多不可读的16进制数据,不利于调试
     缺点2：该方法序列化后的数据，无法跨语言使用，因为它是基于JDK的序列化机制，只能在java环境下使用
     缺点3：由于存储了额外的类型信息，所以占用的空间比较大
     但是该方法的优势是可以序列化任何java对象，包括自定义的对象/

     StringRedisSerializer是springboot提供的另一种序列化方式，它将java对象统一序列化为字符串，存储到redis中
     使用StringRedisSerializer进行序列化后，保存到Redis的数据可读性好，方便调试
     但同时使用该序列化方式也隐式地限定了数据类型必须为String

     当然，可以自定义序列化方式，比如使用JSON序列化方式，将java对象序列化为JSON字符串，存储到redis中
     （只是举个例子，在本项目中不需要额外这么做，因为已经在MVC配置类中配置好了消息转换器,
     所有RestController在返回数据时会通过jackson将数据转为json传递给前端）

     不同类型的key 和 value可以组合出不同的RedisTemplate
     例如： RedisTemplate<String, String>、
     RedisTemplate<String, User>、
     RedisTemplate<Integer, List<User>>等
     前面已经提到，springboot默认使用JdkSerializationRedisSerializer对所有的类型进行序列化
     如果你对某一种类型的序列化方式有自定义的需求(包括hashKey、 hashValue、set等）可以通过重新注册自定义RedisTemplateBean来实现
     在构造器内部，通过setKeySerializer、setValueSerializer、setHashKeySerializer、setHashValueSerializer等方法来设置序列化方式

     也许你会觉得这样做有点麻烦，因为你需要为每一种类型都注册一个RedisTemplateBean
     但其实不用，大部分情况下，我们只需要使用<Object, Object>泛型的RedisTemplate即可,然后对方法返回值进行强转即可
     同时springboot针对<String,String>泛型的RedisTemplate提供了一个专门的RedisTemplateBean，即StringRedisTemplate
     当你确认操作的都是String类型时，可以直接使用StringRedisTemplate
    */

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        // 设置key的序列化器为：StringRedisSerializer，以获得更好的可读性。即所有的key统一序列化为String类型
        // 注意：该操作也限制了key的类型必须为String，当你将key设置为其他类型时，会抛出异常
        // 如果有这方面的需求，你应该设置一个新的指定泛型的RedisTemplateBean<newKey,value>
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

}
