package co.streamx.fluent.JPA;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import co.streamx.fluent.JPA.repository.EntityManagerSupplier;
import co.streamx.fluent.JPA.repository.EntityManagerSupplierImpl;

@SpringBootApplication
public class TestApplication {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

//    @Bean
    // Optional EntityManagerSupplier creation override
    public EntityManagerSupplier entityManagerSupplierImpl() {
        EntityManagerSupplierImpl bean = beanFactory.createBean(EntityManagerSupplierImpl.class);
        return bean;
    }
}
