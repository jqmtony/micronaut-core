package io.micronaut.inject.visitor.beans

import io.micronaut.core.beans.BeanWrapper
import spock.lang.Specification

class BeanWrapperSpec extends Specification {

    void "test bean wrapper"() {
        when:"A wrapper is obtained"
        def bean = new TestBean()
        BeanWrapper<TestBean> wrapper = BeanWrapper.getWrapper(bean)

        then:"it is correct"
        wrapper.propertyNames.size() == 3
        wrapper.bean == bean
        wrapper.beanProperties.size() == 3

        when:
        wrapper.setProperty("name", "Fred")
               .setProperty("age", "10")

        then:
        bean.name == 'Fred'
        bean.age == 10
        wrapper.getRequiredProperty("name", String) == 'Fred'
        wrapper.getRequiredProperty("age", Integer.class) == 10
    }
}
