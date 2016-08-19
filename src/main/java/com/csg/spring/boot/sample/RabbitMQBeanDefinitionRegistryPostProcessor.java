package com.csg.spring.boot.sample;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
// @EnableConfigurationProperties(ConnectionConfigurationProperties.class)
public class RabbitMQBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	public static final String CONNECTION_FACTORY = "ConnectionFactory";
	public static final String RABBIT_ADMIN = "RabbitAdmin";
	public static final String RABBIT_TEMPLATE = "RabbitTemplate";
	public static final String MESSAGING_PROPERTY_PREFIX = "csg.rabbitmq";

	@Autowired
	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}
	// @Autowired
	// ConnectionConfigurationProperties properties;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		// String[] keys = {"primary", "other"};

		Map<String, LinkedHashMap<String, Object>> result = new HashMap();
		bindProperties(result, MESSAGING_PROPERTY_PREFIX);

		Iterator<String> i = result.keySet().iterator();

		while (i.hasNext()) {
			String key = i.next();

			boolean primary = key.equals("primary");

			registerBeans(registry, key, primary);
		}
	}

	private void registerBeans(BeanDefinitionRegistry registry, String key, boolean primary) {

		ConnectionConfigurationProperties properties = new ConnectionConfigurationProperties();

		bindProperties(properties, "csg.rabbitmq" + "." + key);

		BeanDefinition beanDefinition = createConnectionFactoryBean(properties, primary);
		String beanName = getConnectionFactoryBeanName(key);
		registry.registerBeanDefinition(beanName, beanDefinition);

		beanDefinition = createRabbitAdminBean(getConnectionFactoryBeanName(key), primary);
		beanName = getRabbitAdminBeanName(key);
		registry.registerBeanDefinition(beanName, beanDefinition);

		beanDefinition = createRabbitTemplateBean(getConnectionFactoryBeanName(key), primary);
		beanName = getRabbitTemplateBeanName(key);
		registry.registerBeanDefinition(beanName, beanDefinition);

	}

	private BeanDefinitionBuilder createBeanDefinitionBase(final Class type, final boolean isPrimary) {
		// now register ConnectionFactory bean
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(type);

		if (isPrimary) {
			// http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_customizing_the_amqp_connectionfactory
			// adding org.springframework.context.annotation.Primary qualifier doesn't seem to make this bean as the primary
			beanDefinitionBuilder.getBeanDefinition().addQualifier(new AutowireCandidateQualifier(Primary.class));
			// had to set the primary attribute on the bean definition to make this the primary
			beanDefinitionBuilder.getBeanDefinition().setPrimary(true);
		}

		return beanDefinitionBuilder;
	}

	private BeanDefinition createConnectionFactoryBean(final ConnectionConfigurationProperties properties,
		final boolean isPrimary) {
		BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBase(CachingConnectionFactory.class, isPrimary);

		beanDefinitionBuilder.addConstructorArgValue(properties.getHost());
		beanDefinitionBuilder.addConstructorArgValue(properties.getPort());
		beanDefinitionBuilder.addPropertyValue("username", properties.getUsername());
		beanDefinitionBuilder.addPropertyValue("password", properties.getPassword());

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private BeanDefinition createRabbitAdminBean(String connectionFactoryBeanName, boolean isPrimary) {
		BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBase(RabbitAdmin.class, isPrimary);

		beanDefinitionBuilder.addConstructorArgReference(connectionFactoryBeanName);

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private BeanDefinition createRabbitTemplateBean(String connectionFactoryBeanName, boolean isPrimary) {
		BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBase(RabbitTemplate.class, isPrimary);

		beanDefinitionBuilder.addConstructorArgReference(connectionFactoryBeanName);
		beanDefinitionBuilder.addPropertyValue("replyTimeout", 10000L); // use a property for the reply timeout ??

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private String getConnectionFactoryBeanName(String key) {
		return key + CONNECTION_FACTORY;
	}

	private String getRabbitTemplateBeanName(String key) {
		return key + RABBIT_TEMPLATE;
	}

	private String getRabbitAdminBeanName(String key) {
		return key + RABBIT_ADMIN;
	}

	/**
	 * Method to retrieve properties from Spring's Environment.
	 *
	 * Note: ConfigurableEnvironment is a sub interface of Environment. All Environment implementation classes inherit from
	 * ConfigurableEnvironment.
	 *
	 * @return
	 */
	private final Object bindProperties(Object target, String prefix) {
		ConfigurableEnvironment env = (ConfigurableEnvironment)context.getEnvironment();

		RelaxedDataBinder binder = new RelaxedDataBinder(target, prefix);
		binder.bind(new PropertySourcesPropertyValues(env.getPropertySources()));

		return target;
	}
}
