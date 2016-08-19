# Sample Spring Boot Autoconfiguration project
-----
This sample project highlights a bug with RabbitAutoConfiguration calss in spring-boot-autoconfigure 1.4.0.RELEASE that results in NoSuchBeanDefinitionException when multiple ConnectionFactory beans are defined. (https://github.com/spring-projects/spring-boot/issues/6559)


This project dynamically autoconfigures RabbitMQ beans (ConnectionFactory, RabbitTemplate, RabbitAdmin) based on the presence of properties.

The property `csg.rabbitmq.<binding-name>` triggers a BeanDefinitionRegistryPostProcessor `RabbitMQBeanDefinitionRegistryPostProcessor` to register RabbitMQ beans with names prefixed with the binding name. The beans for a binding named "primary" will be registered as @Primary Spring beans.

```
csg:
  rabbitmq:
    primary:
      host: localhost
      port: 5640
      username: guest
      password: guest
    another:
      host: localhost
      port: 5640
      username: guest
      password: guest
``` 


