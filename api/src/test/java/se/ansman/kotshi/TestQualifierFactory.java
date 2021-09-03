package se.ansman.kotshi;

import java.lang.annotation.Annotation;

public class TestQualifierFactory {
    static TestQualifier create(String foo) {
        return new TestQualifier() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TestQualifier.class;
            }

            @Override
            public String foo() {
                return foo;
            }
        };
    }
}
